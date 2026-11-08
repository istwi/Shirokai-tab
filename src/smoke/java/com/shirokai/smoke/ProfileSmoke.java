package com.shirokai.smoke;

import com.mojang.authlib.GameProfile;
import com.shirokai.core.profiles.ProfileMenu;
import com.shirokai.core.storage.SqliteStorage;
import com.shirokai.core.titles.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.*;
import net.minecraft.network.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.*;
import net.minecraft.text.Text;
import java.net.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static net.minecraft.server.command.CommandManager.literal;

/** Development-only server integration tests, never included in the production JAR. */
public final class ProfileSmoke implements ModInitializer {
    private record Wait(BooleanSupplier condition, long deadline, CompletableFuture<Void> result) { }
    private final List<Wait> waits = new ArrayList<>();
    private long tick;
    private boolean running;
    @Override public void onInitialize() {
        new ClanSmoke().onInitialize();
        new ChatSmoke().onInitialize();
        new TabSmoke().onInitialize();
        new ScoreboardSmoke().onInitialize();
        new PresentationSmoke(this::until).onInitialize();
        new StyleChoiceSmoke(this::until).onInitialize();
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cobblemon") && net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("impactor") && net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("shirokaidex") && net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("luckperms")) new IntegrationSmoke(this::until).onInitialize();
        new TeamOwnershipSmoke().onInitialize();
        new ClanMenuSmoke(this::until).onInitialize();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tick++;
            for (Wait wait : List.copyOf(waits)) {
                try {
                    if (wait.condition().getAsBoolean()) { waits.remove(wait); wait.result().complete(null); }
                    else if (tick > wait.deadline()) { waits.remove(wait); wait.result().completeExceptionally(new AssertionError("GUI test timed out")); }
                } catch (Throwable failure) { waits.remove(wait); wait.result().completeExceptionally(failure); }
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
                literal("shirokai-profile-smoke").requires(source -> source.hasPermissionLevel(4)).executes(context -> {
                    if (running) return 0; running = true;
                    var source = context.getSource(); var server = source.getServer();
                    test(server).whenCompleteAsync((ignored, error) -> {
                        running = false;
                        if (error == null) source.sendFeedback(() -> Text.literal("PROFILE_GUI_SMOKE_PASS"), false);
                        else {
                            org.slf4j.LoggerFactory.getLogger("shirokai-profile-smoke").error("PROFILE_GUI_SMOKE_FAIL", error);
                            source.sendError(Text.literal("PROFILE_GUI_SMOKE_FAIL"));
                        }
                    }, server);
                    return 1;
                })));
    }
    private CompletableFuture<Void> until(BooleanSupplier condition) {
        var result = new CompletableFuture<Void>(); waits.add(new Wait(condition, tick + 100, result)); return result;
    }
    private static void check(boolean value, String error) { if (!value) throw new AssertionError(error); }
    static ServerPlayerEntity player(MinecraftServer server,String name){return player(server,name,packet->{});}
    static ServerPlayerEntity player(MinecraftServer server, String name, java.util.function.Consumer<net.minecraft.network.packet.Packet<?>> capture) {
        var profile = new GameProfile(UUID.randomUUID(), name);
        var player = new ServerPlayerEntity(server, server.getOverworld(), profile, SyncedClientOptions.createDefault());
        var connection = new ClientConnection(NetworkSide.SERVERBOUND) {
            @Override public void send(Packet<?> packet) { capture.accept(packet); }
            @Override public void send(Packet<?> packet, PacketCallbacks callbacks) { capture.accept(packet); }
            @Override public void send(Packet<?> packet, PacketCallbacks callbacks, boolean flush) { capture.accept(packet); }
            @Override public void flush() { }
            @Override public boolean isOpen() { return true; }
            @Override public SocketAddress getAddress() { return new InetSocketAddress("127.0.0.1", 0); }
        };
        player.networkHandler = new ServerPlayNetworkHandler(server, connection, player, ConnectedClientData.createDefault(profile, false));
        return player;
    }
    private CompletableFuture<Void> test(MinecraftServer server) {
        var storage = new SqliteStorage(FabricLoader.getInstance().getGameDir().resolve("profile-smoke-" + UUID.randomUUID() + ".db"));
        var definitions = new HashMap<String, TitleDefinition>();
        for (String id : List.of("a", "b", "c")) definitions.put(id, new TitleDefinition(id,id,"["+id+"]","",TitleDefinition.Category.SPECIAL,0,null,true,true,true,true));
        var service = new TitleService(storage,new TitleCatalog(1,definitions),(id,node)->false,server,id->true,Clock.systemUTC(),(message,error)->{
            org.slf4j.LoggerFactory.getLogger("shirokai-profile-smoke").error(message,error);
        });
        var owner = player(server,"ProfileTest"); var stranger = player(server,"OtherTest");
        owner.getInventory().setStack(0, new ItemStack(Items.DIAMOND, 5));
        UUID id = owner.getUuid();
        var result = storage.ready().thenComposeAsync(v -> {
            ProfileMenu.open(owner,service);
            return until(() -> owner.currentScreenHandler instanceof ProfileMenu);
        },server).thenComposeAsync(v -> {
            var menu = (ProfileMenu)owner.currentScreenHandler;
            check(!menu.canUse(stranger),"Other player can use menu");
            check(menu.quickMove(owner,9).isEmpty(),"Quick move extracted icon");
            for (var action : SlotActionType.values()) if (action != SlotActionType.PICKUP)
                menu.onSlotClick(9,0,action,owner);
            menu.onSlotClick(-999,0,SlotActionType.PICKUP,owner);
            menu.onSlotClick(9999,0,SlotActionType.PICKUP,owner);
            menu.onSlotClick(9,0,SlotActionType.PICKUP,stranger);
            check(service.cached(id).orElseThrow().selectedVisual()==null,"Malformed click mutated profile");
            check(owner.getInventory().getStack(0).getCount()==5,"Menu changed player inventory");
            menu.onSlotClick(9,0,SlotActionType.PICKUP,owner);
            return until(() -> "a".equals(service.cached(id).orElseThrow().selectedVisual()));
        },server).thenComposeAsync(v -> {
            ((ProfileMenu)owner.currentScreenHandler).onSlotClick(9,1,SlotActionType.PICKUP,owner);
            return until(() -> service.cached(id).orElseThrow().chatIds().equals(List.of("a")));
        },server).thenComposeAsync(v -> {
            ((ProfileMenu)owner.currentScreenHandler).onSlotClick(10,1,SlotActionType.PICKUP,owner);
            return until(() -> service.cached(id).orElseThrow().chatIds().equals(List.of("a","b")));
        },server).thenComposeAsync(v -> {
            ((ProfileMenu)owner.currentScreenHandler).onSlotClick(11,1,SlotActionType.PICKUP,owner);
            long after = tick + 5;
            return until(() -> tick >= after);
        },server).thenComposeAsync(v -> storage.titleState(id),server).thenComposeAsync(state -> {
            check(state.chatIds().equals(List.of("a","b")),"Third identification was accepted");
            check("a".equals(state.selectedVisual()),"Chat selection changed shared visual title");
            var menu = (ProfileMenu)owner.currentScreenHandler;
            menu.onSlotClick(48,0,SlotActionType.PICKUP,owner);
            return until(() -> service.cached(id).orElseThrow().selectedVisual()==null && service.cached(id).orElseThrow().chatIds().isEmpty());
        },server).thenRunAsync(() -> {
            var menu = (ProfileMenu)owner.currentScreenHandler;
            menu.onSlotClick(50,0,SlotActionType.PICKUP,owner);
            check(owner.currentScreenHandler!=menu,"Return button did not close profile");
            check(!menu.canUse(owner),"Closed menu accepted use");
            menu.onSlotClick(9,0,SlotActionType.PICKUP,owner);
            check(service.cached(id).orElseThrow().selectedVisual()==null,"Closed menu mutated selection");
            check(owner.getInventory().getStack(0).getCount()==5,"Player items changed");
        },server);
        return result.whenCompleteAsync((v,error) -> {
            owner.closeHandledScreen(); stranger.closeHandledScreen(); service.forget(id);
            try { storage.close(); } catch (Exception close) { throw new CompletionException(close); }
        },server);
    }
}
