package com.shirokai.core.integrations;

import com.shirokai.core.clans.ClanService;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.placeholders.PlaceholderService;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import java.util.*;
import java.util.function.*;

/** Server-thread snapshots, coalesced refresh, no blocking future waits or per-frame IO. Optional adapters are isolated. */
public final class IntegrationRuntime implements AutoCloseable {
    private static final class Session {
        final Map<String, String> values = new HashMap<>();
        final Set<String> pending = new HashSet<>();
    }
    private final MinecraftServer server;
    private final PlaceholderService placeholders;
    private final PermissionService permissions;
    private final Supplier<ClanService> clans;
    private final Logger logger;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final LinkedHashSet<UUID> pending = new LinkedHashSet<>();
    private final Map<String, String> providers = new LinkedHashMap<>();
    private final Map<String, Long> warnings = new HashMap<>();
    private PlaceholderBridge papi;
    private CobblemonAccess cobblemon;
    private ImpactorAccess impactor;
    private IntegrationConfig config;
    private long tick, failures;
    private boolean closed;
    public IntegrationRuntime(MinecraftServer server, IntegrationConfig config, PlaceholderService placeholders,
            PermissionService permissions, Supplier<ClanService> clans, Supplier<String> fallback, Logger logger) {
        this.server = server; this.config = config; this.placeholders = placeholders; this.permissions = permissions; this.clans = clans; this.logger = logger;
        if (supported("cobblemon", config.cobblemon(), "1.8.3+1.21.1")) attempt("cobblemon", () -> { cobblemon = new CobblemonAccess(); cobblemon.listen(id -> server.execute(() -> invalidate(id))); });
        if (supported("impactor", config.impactor(), "5.3.5", "5.3.5+1.21.1")) attempt("impactor", () -> impactor = new ImpactorAccess(server));
        if (supported("placeholder-api", config.placeholderApi(), "2.4.2+1.21")) attempt("placeholder-api", () -> papi = new PapiAccess(server, fallback, logger));
        var dex = FabricLoader.getInstance().getModContainer("shirokaidex");
        providers.put("shirokaidex", !config.shirokaiDex() ? "DISABLED" : dex.isEmpty() ? "ABSENT" : papi == null ? "PAPI_UNAVAILABLE" : "INSTALLED " + dex.get().getMetadata().getVersion().getFriendlyString());
        for (String key : List.of("dollars", "tokens", "cash", "pokedex_percent", "pokedex_total", "pokedex_owned", "pokedex_seen", "dex_percent", "gyms", "world", "ping", "group", "clan", "clan_tag", "clan_members", "clan_limit")) {
            String id = "shirokai:" + key;
            placeholders.register(id, context -> nativeValue(id, context));
        }
        for (String key : List.of("prefix", "suffix")) placeholders.register("luckperms:" + key, context -> {
            if (!server.isOnThread() || context.playerId() == null) return Optional.empty();
            var meta = permissions.metadata(context.playerId());
            return Optional.of(key.equals("prefix") ? meta.prefix() : meta.suffix());
        });
        placeholders.external((key, context) -> !closed && papi != null ? papi.resolve(key, context) : Optional.empty());
        if (papi != null) {
            try { papi.export(placeholders); } catch (RuntimeException | LinkageError error) { papi.close(); papi = null; failure("placeholder-api", error); }
        }
        logger.info("[integrations] {}", providers);
    }
    private boolean supported(String id, boolean enabled, String... versions) {
        var mod = FabricLoader.getInstance().getModContainer(id);
        String version = mod.map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
        boolean supported = enabled && mod.isPresent() && Arrays.asList(versions).contains(version);
        providers.put(id, !enabled ? "DISABLED" : mod.isEmpty() ? "ABSENT" : !supported ? "UNSUPPORTED " + version : "INSTALLED " + version);
        return supported;
    }
    private void attempt(String id, Runnable install) { try { install.run(); } catch (RuntimeException | LinkageError failure) { providers.put(id, "FAILED"); failure(id, failure); } }
    private void failure(String id, Throwable error) {
        failures++; Long previous = warnings.get(id);
        if (previous == null || tick - previous >= 400) { warnings.put(id, tick); logger.warn("[integrations] " + id + " indisponível; fallback preservado", error); }
    }
    public IntegrationConfig config() { return config; }
    public void replaceConfig(IntegrationConfig candidate) {
        if (candidate.requiresRestartComparedTo(config)) throw new IllegalArgumentException("Ativação de integrações exige reinício");
        config = candidate; sessions.clear(); pending.clear(); for (var p : server.getPlayerManager().getPlayerList()) join(p.getUuid());
    }
    public void join(UUID id) { sessions.put(id, new Session()); pending.add(id); }
    public void forget(UUID id) { sessions.remove(id); pending.remove(id); }
    public void invalidate(UUID id) { var session = sessions.get(id); if (!closed && session != null) { session.values.remove("shirokai:clan_limit"); pending.add(id); } }
    public void tick() {
        if (closed) return;
        if (++tick % config.refreshTicks() == 0) pending.addAll(sessions.keySet());
        if (pending.isEmpty()) return;
        var it = pending.iterator(); UUID id = it.next(); it.remove();
        var player = server.getPlayerManager().getPlayer(id); var session = sessions.get(id);
        if (player == null || session == null || player.isDisconnected()) { forget(id); return; }
        if (cobblemon != null && config.pokedexProvider().equals("cobblemon")) try { session.values.keySet().removeIf(k -> k.startsWith("shirokai:pokedex_")); session.values.putAll(cobblemon.snapshot(player)); }
        catch (RuntimeException | LinkageError error) { failure("cobblemon", error); }
        var clanService = clans.get();
        session.values.remove("shirokai:clan_limit");
        if (clanService != null) clanService.cached(id).ifPresent(clan -> clanService.currentLimit(clan).whenCompleteAsync((limit, error) -> {
            if (closed || sessions.get(id) != session) return;
            if (error != null) { failure("clan_limit", error); return; }
            if (clanService.cached(id).filter(current -> current.id().equals(clan.id()) && current.leader().equals(clan.leader())).isPresent()) session.values.put("shirokai:clan_limit", Integer.toString(limit));
        }, server));
        if (impactor != null) {
            balance(player, session, "shirokai:dollars", config.dollarsCurrency());
            balance(player, session, "shirokai:tokens", config.tokensCurrency());
            if(config.cashCurrency()!=null)balance(player,session,"shirokai:cash",config.cashCurrency());
        }
    }
    private void balance(ServerPlayerEntity player, Session session, String key, String currency) {
        if (!session.pending.add(key)) return;
        try {
            impactor.balance(player.getUuid(), currency, () -> !closed && sessions.get(player.getUuid()) == session && !player.isDisconnected()).whenCompleteAsync((value, error) -> {
                if (closed || sessions.get(player.getUuid()) != session) return;
                session.pending.remove(key); session.values.remove(key);
                if (error != null) failure("impactor:" + currency, error);
                else value.ifPresent(v -> session.values.put(key, v));
            }, server);
        } catch (RuntimeException | LinkageError error) { session.pending.remove(key); session.values.remove(key); failure("impactor:" + currency, error); }
    }
    private Optional<String> dex(PlaceholderService.Context context, String metric) {
        if (!config.shirokaiDex() || papi == null || !FabricLoader.getInstance().isModLoaded("shirokaidex")) return Optional.empty();
        return papi.resolve("shirokaidex:" + metric + " " + config.dexCollection(), context);
    }
    private Optional<String> nativeValue(String key, PlaceholderService.Context context) {
        if (closed || !server.isOnThread() || context.playerId() == null) return Optional.empty();
        var player = server.getPlayerManager().getPlayer(context.playerId()); if (player == null || player.isDisconnected()) return Optional.empty();
        if (key.equals("shirokai:dex_percent")) return dex(context, "percent");
        if (key.startsWith("shirokai:pokedex_") && config.pokedexProvider().equals("shirokaidex")) {
            String metric = switch (key) { case "shirokai:pokedex_percent" -> "percent"; case "shirokai:pokedex_total" -> "total"; case "shirokai:pokedex_owned" -> "registered"; case "shirokai:pokedex_seen" -> "seen"; default -> null; };
            return metric == null ? Optional.empty() : dex(context, metric);
        }
        if (key.equals("shirokai:world")) return Optional.of(player.getWorld().getRegistryKey().getValue().toString());
        if (key.equals("shirokai:ping")) return Optional.of(Integer.toString(player.networkHandler.getLatency()));
        if (key.equals("shirokai:group")) return Optional.ofNullable(permissions.metadata(player.getUuid()).group());
        if (key.equals("shirokai:gyms")) {
            var board = server.getScoreboard(); var objective = board.getNullableObjective(config.gymsObjective());
            var score = objective == null ? null : board.getScore(player, objective);
            return score == null ? Optional.empty() : Optional.of(Integer.toString(score.getScore()));
        }
        if (key.startsWith("shirokai:clan")) {
            var service = clans.get(); if (service == null) return Optional.empty();
            var clan = service.cached(player.getUuid());
            return clan.flatMap(c -> switch (key) {
                case "shirokai:clan" -> Optional.of(c.name()); case "shirokai:clan_tag" -> Optional.of(c.tag());
                case "shirokai:clan_members" -> Optional.of(Integer.toString(c.members().size()));
                case "shirokai:clan_limit" -> Optional.ofNullable(sessions.get(context.playerId())).map(s -> s.values.get(key));
                default -> Optional.empty();
            });
        }
        var session = sessions.get(context.playerId());
        return session == null ? Optional.empty() : Optional.ofNullable(session.values.get(key));
    }
    private Function<UUID,net.minecraft.text.Text> trainer;
    public void trainer(Function<UUID,net.minecraft.text.Text> provider){trainer=Objects.requireNonNull(provider);}
    public Optional<net.minecraft.text.Text> partyShare(UUID author,int slot) {
        if(closed||!server.isOnThread()||cobblemon==null)return Optional.empty();
        var player=server.getPlayerManager().getPlayer(author);
        if(player==null||player.isDisconnected())return Optional.empty();
        try{return trainer==null?cobblemon.partyShare(player,slot):cobblemon.partyShare(player,slot,trainer.apply(author));}
        catch(RuntimeException|LinkageError error){failure("party-share",error);return Optional.empty();}
    }
    public Optional<net.minecraft.text.Text> walletShare(UUID author) {
        if(closed||!server.isOnThread()||impactor==null)return Optional.empty();
        var session=sessions.get(author);var player=server.getPlayerManager().getPlayer(author);
        if(session==null||player==null||player.isDisconnected())return Optional.empty();
        var text=net.minecraft.text.Text.literal("[Carteira de "+player.getGameProfile().getName()+": ");
        text.append("Dollars: "+session.values.getOrDefault("shirokai:dollars","N/D"));
        text.append(" | Tokens: "+session.values.getOrDefault("shirokai:tokens","N/D"));
        if(config.cashCurrency()!=null)text.append(" | Cash: "+session.values.getOrDefault("shirokai:cash","N/D"));
        return Optional.of(text.append("]").formatted(net.minecraft.util.Formatting.GOLD));
    }
    public String status() { return providers + " sessions=" + sessions.size() + " queued=" + pending.size() + " failures=" + failures + " pokedex=" + config.pokedexProvider() + " currencies=" + config.dollarsCurrency() + "," + config.tokensCurrency() + "," + Objects.toString(config.cashCurrency(),"DISABLED"); }
    @Override public void close() { if (closed) return; closed = true; sessions.clear(); pending.clear(); placeholders.external((key, context) -> Optional.empty()); if (papi != null) papi.close(); if (cobblemon != null) cobblemon.close(); }
}
