package com.shirokai.smoke;

import com.shirokai.core.scoreboard.*;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.placeholders.PlaceholderService;
import com.shirokai.core.storage.SqliteStorage;
import com.shirokai.core.styles.*;
import com.shirokai.core.tab.*;
import com.shirokai.core.titles.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.text.Text;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import static net.minecraft.server.command.CommandManager.literal;

/** Real Fabric transformations and packet capture; synthetic connections, not visual/client validation. */
public final class ScoreboardSmoke implements ModInitializer {
    private boolean running;
    @Override public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(literal("shirokai-scoreboard-smoke").requires(s -> s.hasPermissionLevel(4)).executes(c -> {
            if (running) return 0; running = true; var source = c.getSource();
            test(source.getServer()).whenCompleteAsync((v, error) -> {
                running = false;
                if (error == null) source.sendFeedback(() -> Text.literal("SCOREBOARD_SMOKE_PASS"), false);
                else { org.slf4j.LoggerFactory.getLogger("shirokai-smoke").error("SCOREBOARD_SMOKE_FAIL", error); source.sendError(Text.literal("SCOREBOARD_SMOKE_FAIL")); }
            }, source.getServer()); return 1;
        })));
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static List<ScoreboardScoreUpdateS2CPacket> scores(List<Packet<?>> packets) { return packets.stream().filter(ScoreboardScoreUpdateS2CPacket.class::isInstance).map(ScoreboardScoreUpdateS2CPacket.class::cast).toList(); }
    private static String objective(List<Packet<?>> packets) { return packets.stream().filter(ScoreboardObjectiveUpdateS2CPacket.class::isInstance).map(ScoreboardObjectiveUpdateS2CPacket.class::cast).filter(p -> p.getMode() == 0).findFirst().orElseThrow().getName(); }
    private static ScoreboardObjective foreign(String name) { return new ScoreboardObjective(new Scoreboard(), name, ScoreboardCriterion.DUMMY, Text.literal("Other mod"), ScoreboardCriterion.RenderType.INTEGER, false, BlankNumberFormat.INSTANCE); }
    private CompletableFuture<Void> test(net.minecraft.server.MinecraftServer server) {
        var storage = new SqliteStorage(FabricLoader.getInstance().getGameDir().resolve("sidebar-smoke-" + UUID.randomUUID() + ".db"));
        var pa = new ArrayList<Packet<?>>(); var pb = new ArrayList<Packet<?>>();
        var a = ProfileSmoke.player(server, "SideAlice", pa::add); var b = ProfileSmoke.player(server, "SideBob", pb::add); var players = List.of(a, b);
        var meta = new HashMap<UUID, PermissionService.Metadata>();
        meta.put(a.getUuid(), new PermissionService.Metadata("vip", "", "", 0)); meta.put(b.getUuid(), new PermissionService.Metadata("member", "", "", 0));
        var permissions = new PermissionService() {
            public boolean allowed(UUID id, String node) { return meta.get(id).group().equals("vip"); }
            public String provider() { return "test-only"; }
            public Metadata metadata(UUID id) { return meta.get(id); }
        };
        var titles = new TitleService(storage, TitleCatalog.defaults(), permissions::allowed, server, id -> true, Clock.systemUTC(), (message, error) -> {});
        var placeholders = new PlaceholderService((key, error) -> {}); var dynamic = new String[]{"<click:run_command:/op>literal</click>"};
        placeholders.register("smoke:value", c -> Optional.of(dynamic[0]));
        var any = TabConfig.Condition.any(); var vip = new TabConfig.Condition("sidebar.vip", "vip", Set.of("minecraft:overworld"));
        var lines = List.of(new ScoreboardConfig.Line("title", any, "%shirokai:selected_title%"), new ScoreboardConfig.Line("blank_a", any, ""), new ScoreboardConfig.Line("blank_b", any, ""),
            new ScoreboardConfig.Line("vip", vip, "VIP"), new ScoreboardConfig.Line("dynamic", any, "%smoke:value%"), new ScoreboardConfig.Line("fallback", any, "%shirokai:dollars%"));
        var config = new ScoreboardConfig(1, 20, List.of(new ScoreboardConfig.Layout("default", 0, any, "%animation:gold_wave%", lines), new ScoreboardConfig.Layout("vip", 100, vip, "VIP %shirokai:player%", lines)));
        var styles = new StyleEngine(StyleConfig.defaults());
        var animate = new boolean[]{false};
        var endpoint = new ScoreboardRuntime(server, new ScoreboardPlan(config, styles), permissions, placeholders, titles, null, storage, () -> "N/D", id -> animate[0], org.slf4j.LoggerFactory.getLogger("sidebar-smoke"), () -> players);
        var tab = new TabRuntime(server, new TabPlan(TabConfig.defaults(), styles), permissions, placeholders, titles, null, storage, () -> "N/D", org.slf4j.LoggerFactory.getLogger("sidebar-smoke"), () -> players);
        var old = foreign("smoke_before_join"); var other = foreign("smoke_foreign"); var worldBoard = server.getScoreboard(); int originalObjectives = worldBoard.getObjectives().size();
        String[] owned = new String[1];
        var dispatcher = new com.mojang.brigadier.CommandDispatcher<net.minecraft.server.command.ServerCommandSource>();
        var externalCommand = dispatcher.register(literal("sidebar").executes(c -> 17));
        new ScoreboardCommands(() -> null).register(dispatcher);
        check(dispatcher.getRoot().getChild("sidebar") == externalCommand && externalCommand.getChildren().isEmpty(), "External command changed");
        check(dispatcher.getRoot().getChild("shirokai:sidebar") != null, "Namespaced fallback missing");
        // Real send() mixin must capture private packets already sent before runtime JOIN.
        b.networkHandler.sendPacket(new BundleS2CPacket(List.of(new ScoreboardObjectiveUpdateS2CPacket(old, 0), new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, old))));
        var result = storage.ready().thenCompose(v -> storage.setAnimations(a.getUuid(), false)).thenCompose(v -> storage.setAnimations(b.getUuid(), false))
            .thenComposeAsync(v -> titles.equip(a.getUuid(), "traveler", false), server)
            .thenComposeAsync(v -> endpoint.join(a.getUuid()), server).thenComposeAsync(v -> endpoint.join(b.getUuid()), server)
            .thenRunAsync(() -> {
                owned[0] = objective(pa); check(owned[0].length() == 16, "Unsafe objective identifier");
                var initial = scores(pa); check(initial.size() == 6, "Initial rows missing");
                check(initial.getFirst().display().orElseThrow().getString().equals("[VIAJANTE]"), "Selected title missing");
                check(initial.stream().filter(p -> p.display().orElseThrow().getString().isEmpty()).count() == 2, "Duplicate blanks collapsed");
                check(initial.stream().map(ScoreboardScoreUpdateS2CPacket::scoreHolderName).distinct().count() == 6, "Holders not unique");
                check(initial.stream().map(ScoreboardScoreUpdateS2CPacket::score).toList().equals(List.of(15, 14, 13, 12, 11, 10)), "Line order wrong");
                check(initial.stream().allMatch(p -> p.numberFormat().orElseThrow() == BlankNumberFormat.INSTANCE), "Numbers not hidden");
                check(initial.stream().anyMatch(p -> p.display().orElseThrow().getString().equals(dynamic[0])), "Placeholder parsed as markup");
                check(initial.stream().anyMatch(p -> p.display().orElseThrow().getString().equals("N/D")), "Unavailable money fabricated");
                check(!SidebarPackets.state(a.networkHandler).names().contains(owned[0]), "Own packets mistaken for foreign");
                check(pb.stream().noneMatch(ScoreboardScoreUpdateS2CPacket.class::isInstance), "Foreign sidebar overwritten at join");
                tab.update(); var names = pa.stream().filter(PlayerListS2CPacket.class::isInstance).map(PlayerListS2CPacket.class::cast).flatMap(p -> p.getEntries().stream()).filter(e -> e.profileId().equals(a.getUuid())).findFirst().orElseThrow();
                check(names.displayName().getString().equals("[VIAJANTE] SideAlice"), "TAB and scoreboard selections diverged");
                pa.clear(); pb.clear(); endpoint.update(); check(pa.isEmpty() && pb.isEmpty(), "Unchanged frame sent packets");
                dynamic[0] = "Changed"; endpoint.update(); check(scores(pa).size() == 1, "One line change rebuilt scoreboard");
                pa.clear(); dynamic[0] = "x".repeat(257); endpoint.update(); check(pa.isEmpty(), "Invalid dynamic frame partially published"); dynamic[0] = "Changed";
                meta.put(a.getUuid(), new PermissionService.Metadata("member", "", "", 0)); endpoint.update();
                check(pa.stream().anyMatch(ScoreboardScoreResetS2CPacket.class::isInstance), "Hidden row not removed");
                check(pa.stream().filter(ScoreboardObjectiveUpdateS2CPacket.class::isInstance).map(ScoreboardObjectiveUpdateS2CPacket.class::cast).allMatch(p -> p.getMode() == 2), "Layout rebuilt objective");
                pa.clear(); animate[0] = true; for (int i = 0; i < 20; i++) endpoint.tick();
                check(pa.stream().anyMatch(ScoreboardObjectiveUpdateS2CPacket.class::isInstance) && scores(pa).isEmpty(), "Animation rebuilt rows or did not advance");
                animate[0] = false; endpoint.update(); pa.clear(); for (int i = 0; i < 20; i++) endpoint.tick(); check(pa.isEmpty(), "Frozen animation sent redundant packets");
                a.networkHandler.sendPacket(new ScoreboardScoreResetS2CPacket(owned[0] + "_0", null)); pa.clear(); endpoint.update();
                check(scores(pa).size() == 5, "Global holder reset left cached row missing");
                pa.clear(); endpoint.replacePlan(new ScoreboardPlan(new ScoreboardConfig(1, 20, List.of(new ScoreboardConfig.Layout("default", 0, any, "Reload", List.of(new ScoreboardConfig.Line("one", any, "Reloaded"))))), styles));
                check(scores(pa).size() == 1 && pa.stream().filter(ScoreboardScoreResetS2CPacket.class::isInstance).count() == 4, "Reload did not remove stale rows");
                pa.clear(); a.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(other, 0)); a.networkHandler.sendPacket(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.TEAM_RED, other)); pa.clear(); endpoint.update();
                check(pa.size() == 1 && pa.getFirst() instanceof ScoreboardObjectiveUpdateS2CPacket p && p.getMode() == 1 && p.getName().equals(owned[0]), "Did not yield colored sidebar safely");
                pa.clear(); endpoint.update(); check(pa.isEmpty(), "Fought external owner");
                a.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(other, 1)); pa.clear(); endpoint.update(); owned[0] = objective(pa);
                // Clearing the slot externally must invalidate the cached display even with no foreign objective.
                a.networkHandler.sendPacket(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, null)); pa.clear(); endpoint.update(); check(pa.stream().anyMatch(ScoreboardDisplayS2CPacket.class::isInstance), "External clear left stale display cache");
                // Normal visibility toggles are serialized; two concurrent toggles must restore the original value.
            }, server).thenComposeAsync(v -> {
                var one = endpoint.toggle(a.getUuid()); var two = endpoint.toggle(a.getUuid()); return CompletableFuture.allOf(one, two).thenRun(() -> { check(!one.join() && two.join(), "Concurrent toggles lost update"); });
            }, server).thenComposeAsync(v -> endpoint.toggle(a.getUuid()), server).thenComposeAsync(enabled -> {
                check(!enabled, "Visibility did not turn off"); return storage.preferences(a.getUuid());
            }, server).thenComposeAsync(prefs -> {
                check(!prefs.scoreboardEnabled() && !prefs.animationsEnabled(), "Visibility changed animation preference");
                endpoint.forget(a.getUuid()); pa.clear(); return endpoint.join(a.getUuid());
            }, server).thenRunAsync(() -> {
                check(scores(pa).isEmpty(), "Reconnect ignored saved hidden preference");
                check(worldBoard.getObjectives().size() == originalObjectives, "Private objectives leaked into world scoreboard");
                b.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(old, 1)); pb.clear(); endpoint.update(); String bobOwned = objective(pb);
                // Another mod replaces our exact name. Never remove that replacement or reclaim the sidebar.
                b.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(foreign(bobOwned), 2)); pb.clear(); endpoint.update(); endpoint.close();
                check(pb.isEmpty(), "Foreign replacement removed or overwritten on stop");
                check(SidebarPackets.state(b.networkHandler).names().isEmpty(), "Connection ledger leaked after close");
            }, server);
        return result.whenCompleteAsync((v, error) -> {
            endpoint.close(); tab.close(); titles.forget(a.getUuid()); titles.forget(b.getUuid()); SidebarPackets.forget(a.networkHandler); SidebarPackets.forget(b.networkHandler);
            try { storage.close(); } catch (Exception close) { throw new CompletionException(close); }
        }, server);
    }
}
