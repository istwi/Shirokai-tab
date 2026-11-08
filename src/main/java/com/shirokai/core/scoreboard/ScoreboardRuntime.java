package com.shirokai.core.scoreboard;

import com.shirokai.core.clans.ClanService;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.placeholders.PlaceholderService;
import com.shirokai.core.storage.SqliteStorage;
import com.shirokai.core.tab.TabPlan;
import com.shirokai.core.titles.TitleService;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Server-thread confined. Client-only objectives never enter the saved world scoreboard. */
public final class ScoreboardRuntime implements AutoCloseable {
    private record Row(int score, Text text) { }
    private static final class View {
        final ServerPlayerEntity player;
        ScoreboardObjective objective;
        Text title;
        Map<String, Row> rows = Map.of();
        boolean ready, enabled, animations, relinquished;
        long displayRevision, resetRevision;
        CompletableFuture<Void> writes = CompletableFuture.completedFuture(null);
        long lastError = -200;
        View(ServerPlayerEntity player) { this.player = player; }
    }
    private final MinecraftServer server;
    private final PermissionService permissions;
    private final PlaceholderService placeholders;
    private final TitleService titles;
    private final ClanService clans;
    private final SqliteStorage storage;
    private final Supplier<String> fallback;
    private final Supplier<Collection<ServerPlayerEntity>> online;
    private final Function<UUID, Boolean> animations;
    private final Logger logger;
    private final Map<UUID, View> views = new HashMap<>();
    private ScoreboardPlan plan;
    private long tick, packets, conflicts, errors;
    private boolean closed, dirty;
    public void invalidate() { dirty = true; }

    public ScoreboardRuntime(MinecraftServer server, ScoreboardPlan plan, PermissionService permissions,
            PlaceholderService placeholders, TitleService titles, ClanService clans, SqliteStorage storage,
            Supplier<String> fallback, Function<UUID, Boolean> animations, Logger logger) {
        this(server, plan, permissions, placeholders, titles, clans, storage, fallback, animations, logger,
            () -> List.copyOf(server.getPlayerManager().getPlayerList()));
    }
    public ScoreboardRuntime(MinecraftServer server, ScoreboardPlan plan, PermissionService permissions,
            PlaceholderService placeholders, TitleService titles, ClanService clans, SqliteStorage storage,
            Supplier<String> fallback, Function<UUID, Boolean> animations, Logger logger,
            Supplier<Collection<ServerPlayerEntity>> online) {
        this.server = server; this.plan = plan; this.permissions = permissions; this.placeholders = placeholders;
        this.titles = titles; this.clans = clans; this.storage = storage; this.fallback = fallback;
        this.animations = animations; this.logger = logger; this.online = online;
    }
    public ScoreboardPlan plan() { return plan; }
    public void replacePlan(ScoreboardPlan candidate) { plan = candidate; update(); }
    public CompletableFuture<Void> join(UUID id) {
        var player = online.get().stream().filter(p -> p.getUuid().equals(id)).findFirst().orElse(null);
        if (closed || player == null) return CompletableFuture.completedFuture(null);
        var previous = views.get(id);
        if (previous != null && previous.player == player) return previous.writes;
        if (previous != null) forget(id);
        var view = new View(player); views.put(id, view); SidebarPackets.retain(player.networkHandler);
        if (storage == null) return CompletableFuture.completedFuture(null); // Fail closed until persisted visibility is known.
        view.writes = storage.preferences(id).thenAcceptAsync(prefs -> {
            if (closed || views.get(id) != view) return;
            view.enabled = prefs.scoreboardEnabled(); view.animations = prefs.animationsEnabled(); view.ready = true; update();
        }, server);
        return view.writes;
    }
    public CompletableFuture<Boolean> toggle(UUID id) {
        if (!closed && storage != null && !views.containsKey(id)) join(id);
        var view = views.get(id);
        if (closed || view == null || storage == null) return CompletableFuture.failedFuture(new IllegalStateException("Preferência de scoreboard indisponível"));
        var operation = view.writes.handle((v, error) -> null).thenComposeAsync(v -> {
            if (closed || views.get(id) != view || !view.ready) return CompletableFuture.failedFuture(new IllegalStateException("Preferência ainda não carregada"));
            boolean enabled = !view.enabled;
            return storage.setScoreboard(id, enabled).thenApplyAsync(ignored -> {
                if (!closed && views.get(id) == view) { view.enabled = enabled; update(); }
                return enabled;
            }, server);
        }, server);
        view.writes = operation.thenApply(v -> null);
        return operation;
    }
    public void refresh(UUID id) {
        var view = views.get(id);
        if (closed || view == null || view.player.isDisconnected()) return;
        try { update(view, (int)online.get().stream().filter(p -> !p.isDisconnected()).count()); }
        catch (RuntimeException error) { errors++; logger.warn("[scoreboard] Refresh preservado para " + id, error); }
    }
    public void tick() { if (!closed && (++tick % plan.config().updateTicks() == 0 || dirty)) { dirty = false; update(); } }
    private String resolve(String key, ServerPlayerEntity p, PermissionService.Metadata meta, int count) {
        return switch (key) {
            case "shirokai:player", "shirokai:viewer" -> p.getGameProfile().getName();
            case "shirokai:uuid" -> p.getUuidAsString();
            case "shirokai:online" -> Integer.toString(count);
            case "shirokai:world" -> p.getWorld().getRegistryKey().getValue().toString();
            case "shirokai:ping" -> Integer.toString(p.networkHandler.getLatency());
            // Same TitleService selection and validation as TAB. No independent visual selection.
            case "shirokai:selected_title" -> titles == null ? "" : titles.placeholder(p.getUuid(), key).orElse("");
            case "shirokai:clan" -> clans == null ? fallback.get() : clans.cached(p.getUuid()).map(c -> c.name()).orElse(fallback.get());
            case "shirokai:clan_tag" -> clans == null ? fallback.get() : clans.cached(p.getUuid()).map(c -> c.tag()).orElse(fallback.get());
            case "shirokai:group" -> Objects.requireNonNullElse(meta.group(), fallback.get());
            case "luckperms:prefix" -> meta.prefix();
            case "luckperms:suffix" -> meta.suffix();
            default -> placeholders == null ? fallback.get() : placeholders.render("%" + key + "%", new PlaceholderService.Context(p.getUuid(), p.getGameProfile().getName(), count), fallback.get());
        };
    }
    private void send(View view, Packet<?> packet) { SidebarPackets.send(view.player.networkHandler, packet); packets++; }
    private static boolean sidebar(ScoreboardDisplaySlot slot) { return slot != ScoreboardDisplaySlot.LIST && slot != ScoreboardDisplaySlot.BELOW_NAME; }
    private void release(View view, SidebarPackets.State state) {
        if (view.objective == null) return;
        // Any foreign use of the exact objective name revokes ownership. Never delete their replacement.
        if (!state.overflow() && !state.names().contains(view.objective.getName()) && server.getScoreboard().getNullableObjective(view.objective.getName()) == null) send(view, new ScoreboardObjectiveUpdateS2CPacket(view.objective, ScoreboardObjectiveUpdateS2CPacket.REMOVE_MODE));
        view.objective = null; view.title = null; view.rows = Map.of();
    }
    public void update() {
        if (closed) return;
        var players = online.get().stream().filter(p -> !p.isDisconnected()).toList();
        var active = new HashSet<UUID>(); players.forEach(p -> active.add(p.getUuid()));
        for (var id : List.copyOf(views.keySet())) if (!active.contains(id)) forget(id);
        for (var p : players) {
            var view = views.get(p.getUuid());
            if (view == null || view.player != p) {
                // JOIN may run before the player appears in PlayerManager. Reconcile on the next frame.
                join(p.getUuid()).whenComplete((ignored, error) -> {
                    if (error != null) logger.warn("[scoreboard] Preferência indisponível " + p.getUuid(), error);
                });
                continue;
            }
            try { update(view, players.size()); }
            catch (RuntimeException error) { errors++; if (tick - view.lastError >= 200) { view.lastError = tick; logger.warn("[scoreboard] Último frame preservado para " + p.getUuid(), error); } }
        }
    }
    private void update(View view, int count) {
        var p = view.player; var foreign = SidebarPackets.state(p.networkHandler); var board = server.getScoreboard();
        boolean occupied = foreign.overflow() || foreign.slots().keySet().stream().anyMatch(ScoreboardRuntime::sidebar)
            || Arrays.stream(ScoreboardDisplaySlot.values()).filter(ScoreboardRuntime::sidebar).anyMatch(slot -> board.getObjectiveForSlot(slot) != null);
        boolean collision = view.objective != null && (foreign.names().contains(view.objective.getName()) || board.getNullableObjective(view.objective.getName()) != null);
        if (collision) { conflicts++; view.relinquished = true; view.objective = null; view.title = null; view.rows = Map.of(); }
        if (foreign.overflow()) view.relinquished = true;
        if (!view.ready || !view.enabled || occupied || view.relinquished) { if (view.objective != null && occupied) conflicts++; release(view, foreign); return; }
        if (view.displayRevision != foreign.displayRevision()) { release(view, foreign); view.displayRevision = foreign.displayRevision(); }
        boolean resendRows = view.resetRevision != foreign.resetRevision();
        var meta = permissions.metadata(p.getUuid()); String world = p.getWorld().getRegistryKey().getValue().toString();
        Predicate<String> allowed = node -> permissions.allowed(p.getUuid(), node);
        var layout = plan.layout(meta, world, allowed); var values = new HashMap<String, String>();
        Function<String, String> resolver = key -> values.computeIfAbsent(key, k -> resolve(k, p, meta, count));
        Function<String,Text> richResolver = key -> {
            if(key.equals("shirokai:selected_title")&&titles!=null)return titles.visualPresentation(p.getUuid());
            if(clans!=null&&(key.equals("shirokai:clan")||key.equals("shirokai:clan_tag"))&&clans.cached(p.getUuid()).isPresent())
                return clans.present(p.getUuid(),key.equals("shirokai:clan")?com.shirokai.core.presentation.Surface.CLAN_NAME:com.shirokai.core.presentation.Surface.CLAN_TAG,resolver.apply(key));
            return Text.literal(resolver.apply(key));
        };
        boolean animate = animations == null ? view.animations : animations.apply(p.getUuid());
        Text title = plan.styles().renderRich(layout.title(), richResolver, tick, animate);
        if (title.getString().codePointCount(0, title.getString().length()) > 128) throw new IllegalArgumentException("Título de scoreboard excede 128 caracteres");
        // Validate the entire frame before sending its first packet; dynamic failures preserve the old view.
        var desired = new LinkedHashMap<String, Row>(); int visible = 0;
        for (int i = 0; i < layout.lines().size(); i++) {
            var line = layout.lines().get(i);
            if (!TabPlan.matches(line.definition().condition(), meta, world, allowed)) continue;
            Text text = plan.styles().renderRich(line.text(), richResolver, tick, animate);
            if (text.getString().codePointCount(0, text.getString().length()) > 256) throw new IllegalArgumentException("Linha de scoreboard excede 256 caracteres");
            desired.put(Integer.toString(i), new Row(15 - visible++, text));
        }
        if (view.objective == null) {
            String name;
            do { name = "sk" + UUID.randomUUID().toString().replace("-", "").substring(0, 14); }
            while (foreign.names().contains(name) || board.getNullableObjective(name) != null);
            view.objective = new ScoreboardObjective(new Scoreboard(), name, ScoreboardCriterion.DUMMY, title, ScoreboardCriterion.RenderType.INTEGER, false, BlankNumberFormat.INSTANCE);
            send(view, new ScoreboardObjectiveUpdateS2CPacket(view.objective, ScoreboardObjectiveUpdateS2CPacket.ADD_MODE));
            send(view, new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, view.objective)); view.title = title;
        } else if (!Objects.equals(view.title, title)) {
            view.objective.setDisplayName(title);
            send(view, new ScoreboardObjectiveUpdateS2CPacket(view.objective, ScoreboardObjectiveUpdateS2CPacket.UPDATE_MODE)); view.title = title;
        }
        for (String holder : view.rows.keySet()) if (!desired.containsKey(holder)) send(view, new ScoreboardScoreResetS2CPacket(view.objective.getName() + "_" + holder, view.objective.getName()));
        for (var entry : desired.entrySet()) if (resendRows || !entry.getValue().equals(view.rows.get(entry.getKey()))) {
            var row = entry.getValue(); send(view, new ScoreboardScoreUpdateS2CPacket(view.objective.getName() + "_" + entry.getKey(), view.objective.getName(), row.score(), Optional.of(row.text()), Optional.of(BlankNumberFormat.INSTANCE)));
        }
        view.rows = Map.copyOf(desired); view.resetRevision = foreign.resetRevision();
    }
    public void forget(UUID id) {
        var view = views.remove(id);
        if (view != null) {
            try { if (!view.player.isDisconnected()) release(view, SidebarPackets.state(view.player.networkHandler)); }
            finally { SidebarPackets.release(view.player.networkHandler); }
        }
    }
    public String status() { return "viewers=" + views.size() + " objectives=" + views.values().stream().filter(v -> v.objective != null).count() + " packets=" + packets + " conflicts=" + conflicts + " errors=" + errors; }
    @Override public void close() { if (closed) return; closed = true; for (var id : List.copyOf(views.keySet())) { try { forget(id); } catch (RuntimeException error) { logger.warn("[scoreboard] Falha ao liberar " + id, error); } } }
}
