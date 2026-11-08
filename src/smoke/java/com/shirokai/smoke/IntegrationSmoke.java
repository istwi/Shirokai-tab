package com.shirokai.smoke;

import com.shirokai.core.integrations.*;
import com.shirokai.core.permissions.*;
import com.shirokai.core.placeholders.PlaceholderService;
import com.shirokai.core.styles.*;
import com.shirokai.smoke.mixin.SmokePlayers;
import eu.pb4.placeholders.api.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.PrefixNode;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import static net.minecraft.server.command.CommandManager.literal;

/** Actual optional providers in an isolated Fabric world; synthetic online player, no authenticated-client assertion. */
public final class IntegrationSmoke implements ModInitializer {
    private final Function<BooleanSupplier, CompletableFuture<Void>> until;
    private boolean running;
    public IntegrationSmoke(Function<BooleanSupplier, CompletableFuture<Void>> until) { this.until = until; }
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(literal("shirokai-integrations-smoke").requires(s -> s.hasPermissionLevel(4)).executes(c -> {
            if (running) return 0; running = true; var source = c.getSource();
            test(source.getServer()).whenCompleteAsync((v, error) -> {
                running = false;
                if (error == null) source.sendFeedback(() -> Text.literal("INTEGRATIONS_SMOKE_PASS"), false);
                else { org.slf4j.LoggerFactory.getLogger("integration-smoke").error("INTEGRATIONS_SMOKE_FAIL", error); source.sendError(Text.literal("INTEGRATIONS_SMOKE_FAIL")); }
            }, source.getServer()); return 1;
        })));
    }
    private static boolean positive(String value) {
        try { return new BigDecimal(value.replace("%", "").replace(",", ".").strip()).compareTo(BigDecimal.ZERO) > 0; } catch (NumberFormatException invalid) { return false; }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private CompletableFuture<Currency> currency(String key, BigDecimal starting, net.minecraft.server.MinecraftServer server) {
        var service = EconomyService.instance(); var existing = service.currencies().currency(Key.key(key));
        if (existing.isPresent()) return CompletableFuture.completedFuture(existing.get());
        var currency = Currency.builder().key(Key.key(key)).name(Component.text(key)).plural(Component.text(key)).symbol(Component.text("$"))
            .starting(starting).decimals(2).formatting(new Currency.CurrencyFormatting("{symbol}{amount}", "{amount} {currency}" )).build();
        return service.currencies().register(currency).thenApplyAsync(registered -> service.currencies().currency(Key.key(key)).orElseThrow(), server);
    }
    private CompletableFuture<Void> testStyles(net.minecraft.server.MinecraftServer server,net.minecraft.server.network.ServerPlayerEntity player,
            PermissionService permissions,net.luckperms.api.model.user.User user) {
        var directory=net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("lp-style-"+UUID.randomUUID());
        var storage=new com.shirokai.core.storage.SqliteStorage(directory.resolve("data.db"));
        var engine=new StyleEngine(StyleConfig.defaults());
        final com.shirokai.core.customization.PresetCatalog catalog;
        try { catalog=com.shirokai.core.customization.PresetCatalog.load(directory.resolve("presets.json"),engine); }
        catch(Exception error){try{storage.close();}catch(Exception close){error.addSuppressed(close);}return CompletableFuture.failedFuture(error);}
        var choices=new com.shirokai.core.customization.StyleChoiceService(new com.shirokai.core.customization.StyleChoiceRepository(storage),catalog,permissions,null,server,id->id.equals(player.getUuid()));
        var node=PermissionNode.builder("shirokaicore.style.gold").build();user.data().add(node);
        var surface=com.shirokai.core.presentation.Surface.TITLE;
        return choices.join(player.getUuid()).thenComposeAsync(v->choices.view(player.getUuid(),surface,null),server)
            .thenComposeAsync(view->choices.choose(player.getUuid(),view,"gold",()->true),server)
            .thenRunAsync(()->{
                check(choices.resolution(player.getUuid(),surface).status()==com.shirokai.core.customization.PresetCatalog.Status.ACTIVE,"Real LP did not authorize saved preset");
                user.data().remove(node);
                check(choices.resolution(player.getUuid(),surface).status()==com.shirokai.core.customization.PresetCatalog.Status.UNAVAILABLE,"Real LP revoke did not apply style fallback");
                check("gold".equals(choices.resolution(player.getUuid(),surface).saved()),"Real LP loss erased saved style");
                user.data().add(node);
                check(choices.resolution(player.getUuid(),surface).status()==com.shirokai.core.customization.PresetCatalog.Status.ACTIVE,"Real LP recovery did not restore saved preset");
            },server).whenCompleteAsync((v,error)->{
                choices.forget(player.getUuid());user.data().remove(node);
                try{storage.close();}catch(Exception close){throw new CompletionException(close);}
            },server);
    }
    private CompletableFuture<Void> test(net.minecraft.server.MinecraftServer server) {
        var fixture = (SmokePlayers)server.getPlayerManager(); var player = ProfileSmoke.player(server, "CoreIntegrate");
        fixture.shirokai$players().add(player); fixture.shirokai$playerMap().put(player.getUuid(), player);
        var logger = org.slf4j.LoggerFactory.getLogger("integration-smoke");
        var service = new PlaceholderService((key, error) -> logger.warn("Provider failed: " + key, error));
        var permissions = new LuckPermsPermissions();
        var runtime = new IntegrationRuntime(server, IntegrationConfig.defaults(), service, permissions, () -> null, () -> "N/D", logger);
        var bridge = new PapiAccess(server, () -> "N/D", logger);
        var context = new PlaceholderService.Context(player.getUuid(), player.getGameProfile().getName(), 1);
        var board = server.getScoreboard();
        var objective = board.addObjective("smoke_gyms", ScoreboardCriterion.DUMMY, Text.literal("Gym fixture"), ScoreboardCriterion.RenderType.INTEGER, false, BlankNumberFormat.INSTANCE);
        var base = IntegrationConfig.defaults();
        var config = new IntegrationConfig(1, true, true, true, true, 20, base.dollarsCurrency(), base.tokensCurrency(), "smoke_gyms", "national", "cobblemon");
        runtime.replaceConfig(config); runtime.join(player.getUuid());
        // Actual API handlers: import arguments, export literal values, and preserve replacement ownership.
        var nativeProbe = new PlaceholderService((key, error) -> {});
        nativeProbe.register("shirokai:smoke_literal", c -> Optional.of("<click:run_command:/op>literal</click>"));
        bridge.export(nativeProbe);
        var exported = Placeholders.parsePlaceholder(Identifier.of("shirokai:smoke_literal"), null, PlaceholderContext.of(player));
        check(exported.isValid() && exported.text().getString().equals("<click:run_command:/op>literal</click>") && exported.text().getStyle().getClickEvent() == null, "Native export interpreted markup");
        board.getOrCreateScore(player, objective).setScore(7);
        check(service.render("%shirokai:gyms%", context, "N/D").equals("7"), "Real gym objective missing");
        check(bridge.resolve("player:objective smoke_gyms", context).orElseThrow().equals("7"), "Real PAPI objective argument missing");
        var styles = new StyleEngine(StyleConfig.defaults());
        check(styles.render(styles.compile("Gyms %player:objective smoke_gyms%"), key -> service.render("%" + key + "%", context, "N/D"), 0, false).getString().equals("Gyms 7"), "StyleEngine did not import PAPI arguments");
        var api = LuckPermsProvider.get();
        BigDecimal[] before = new BigDecimal[2];
        var result = api.getGroupManager().createAndLoadGroup("smoke_owner")
            .thenCompose(group->{group.data().add(net.luckperms.api.node.types.WeightNode.builder(100).build());return api.getGroupManager().createAndLoadGroup("smoke_admin");})
            .thenCompose(group->{group.data().add(net.luckperms.api.node.types.WeightNode.builder(200).build());return api.getUserManager().loadUser(player.getUuid());}).thenComposeAsync(user -> {
            var rankTitles=new com.shirokai.core.titles.TitleService(null,com.shirokai.core.titles.TitleCatalog.defaults(),permissions::allowed,server,id->true,java.time.Clock.systemUTC(),(m,e)->{throw new AssertionError(m,e);});
            rankTitles.bindRanks(permissions::metadata);
            user.data().add(net.luckperms.api.node.types.InheritanceNode.builder("smoke_owner").build());
            user.setPrimaryGroup("smoke_owner");
            user.getCachedData().invalidate();
            check(rankTitles.visualPresentation(player.getUuid()).getString().equals("[SMOKE_OWNER]"),"Real LP primary rank was not automatic: "+permissions.metadata(player.getUuid()).group());
            user.data().add(net.luckperms.api.node.types.InheritanceNode.builder("smoke_admin").build());
            user.setPrimaryGroup("smoke_admin");
            user.getCachedData().invalidate();
            check(rankTitles.chatDefinitions(player.getUuid()).getFirst().display().equals("[SMOKE_ADMIN]"),"Real LP primary rank change not reflected in chat");
            var permission = PermissionNode.builder("shirokai.smoke.permission").build();
            var prefix = PrefixNode.builder("[INTEGRATION]", 900).build();
            user.data().add(permission); user.data().add(prefix);
            check(permissions.allowed(player.getUuid(), "shirokai.smoke.permission"), "Real LP grant not observed");
            check(permissions.metadata(player.getUuid()).prefix().equals("[INTEGRATION]"), "Real LP metadata not observed");
            user.data().remove(permission); user.data().remove(prefix);
            check(!permissions.allowed(player.getUuid(), "shirokai.smoke.permission"), "Real LP revoke not observed");
            // Dex access remains its own provider and permission source; this grant is only for the test user.
            user.data().add(PermissionNode.builder("shirokaidex.*").build());
            return testStyles(server,player,permissions,user).thenComposeAsync(v->currency("impactor:dollars", new BigDecimal("125.25"), server),server);
        }, server).thenComposeAsync(dollars -> EconomyService.instance().account(dollars, player.getUuid()).thenAcceptAsync(account -> before[0] = account.balance(), server), server)
            .thenComposeAsync(v -> currency("impactor:tokens", new BigDecimal("42.5"), server), server)
            .thenComposeAsync(tokens -> EconomyService.instance().account(tokens, player.getUuid()).thenAcceptAsync(account -> before[1] = account.balance(), server), server)
            .thenRunAsync(() -> { runtime.invalidate(player.getUuid()); runtime.tick(); }, server)
            .thenComposeAsync(v -> until.apply(() -> { runtime.tick(); return !service.render("%shirokai:dollars%", context, "N/D").equals("N/D") && !service.render("%shirokai:tokens%", context, "N/D").equals("N/D"); }), server)
            .thenRunAsync(() -> {
                check(new BigDecimal(service.render("%shirokai:dollars%", context, "N/D")).compareTo(before[0]) == 0, "Wrong dollars balance");
                check(new BigDecimal(service.render("%shirokai:tokens%", context, "N/D")).compareTo(before[1]) == 0, "Wrong tokens balance");
                check(before[0].compareTo(before[1]) != 0, "Currency fixture is not distinct");
                int total = Integer.parseInt(service.render("%shirokai:pokedex_total%", context, "N/D"));
                check(total > 800 && service.render("%shirokai:pokedex_owned%", context, "N/D").equals("0"), "Actual Cobblemon registry or fresh Pokédex unavailable");
                check(service.render("%shirokai:pokedex_percent%", context, "N/D").equals("0"), "Empty official Pokédex not zero");
                check(bridge.resolve("shirokaidex:percent national", context).isPresent(), "Actual Dex handler absent");
                server.getCommandManager().executeWithPrefix(server.getCommandSource(), "givepokemonother CoreIntegrate pikachu");
            }, server)
            .thenComposeAsync(v -> until.apply(() -> { runtime.tick(); return service.render("%shirokai:pokedex_owned%", context, "N/D").equals("1"); }), server)
            .thenRunAsync(() -> check(new BigDecimal(service.render("%shirokai:pokedex_percent%", context, "N/D")).compareTo(BigDecimal.ZERO) > 0, "Official acquisition did not update percent"), server)
            .thenComposeAsync(v -> until.apply(() -> positive(service.render("%shirokai:dex_percent%", context, "N/D"))), server)
            .thenRunAsync(() -> {
                String dex = service.render("%shirokai:dex_percent%", context, "N/D");
                check(dex.equals(bridge.resolve("shirokaidex:percent national", context).orElseThrow()), "Dex alias recalculated provider data");
                var dexConfig = new IntegrationConfig(1, true, true, true, true, 20, base.dollarsCurrency(), base.tokensCurrency(), "smoke_gyms", "national", "shirokaidex");
                runtime.replaceConfig(dexConfig);
                for (var entry : Map.of("percent", "percent", "total", "total", "owned", "registered", "seen", "seen").entrySet()) {
                    check(service.render("%shirokai:pokedex_" + entry.getKey() + "%", context, "N/D").equals(bridge.resolve("shirokaidex:" + entry.getValue() + " national", context).orElseThrow()), "Dex source mixed metrics from different collections");
                }
                // Unsupported/missing currency is not silently replaced by the primary currency, nor is an account created.
            }, server).thenComposeAsync(v -> new ImpactorAccess(server).balance(player.getUuid(), "impactor:missing_fixture"), server)
            .thenComposeAsync(missing -> {
                check(missing.isEmpty(), "Missing currency fell back to primary");
                UUID absent = UUID.randomUUID();
                return new ImpactorAccess(server).balance(absent, "impactor:dollars")
                    .thenComposeAsync(balance -> { check(balance.isEmpty(), "Missing account invented a balance"); return EconomyService.instance().hasAccount(EconomyService.instance().currencies().currency(Key.key("impactor:dollars")).orElseThrow(), absent); }, server)
                    .thenAccept(exists -> check(!exists, "Display created an absent economy account"));
            }, server).thenRunAsync(() -> {
                var replacement = (PlaceholderHandler)(c, arg) -> PlaceholderResult.value(Text.literal("replacement"));
                Placeholders.register(Identifier.of("shirokai:smoke_literal"), replacement); bridge.close();
                check(Placeholders.getPlaceholders().get(Identifier.of("shirokai:smoke_literal")) == replacement, "Replaced handler removed on close");
                Placeholders.remove(Identifier.of("shirokai:smoke_literal"));
                runtime.forget(player.getUuid()); check(service.render("%shirokai:dollars%", context, "N/D").equals("N/D"), "Disconnect left monetary cache");
            }, server);
        return result.whenCompleteAsync((v, error) -> {
            runtime.close(); bridge.close(); board.removeObjective(objective);
            fixture.shirokai$playerMap().remove(player.getUuid(), player); fixture.shirokai$players().remove(player);
        }, server);
    }
}
