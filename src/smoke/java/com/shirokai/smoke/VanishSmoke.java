package com.shirokai.smoke;

import com.shirokai.core.chat.*;
import com.shirokai.smoke.mixin.SmokePlayers;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.storage.SqliteStorage;
import com.shirokai.core.visibility.VisibilityService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import static net.minecraft.server.command.CommandManager.literal;

/** Audited external API/real mixins, synthetic network connections; not authenticated clients. */
public final class VanishSmoke implements ModInitializer {
    private final Function<BooleanSupplier,CompletableFuture<Void>> until;
    public VanishSmoke(Function<BooleanSupplier,CompletableFuture<Void>> until){this.until=until;}
    public void onInitialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(literal("shirokai-vanish-smoke").requires(s->s.hasPermissionLevel(4)).executes(c->{
        test(c.getSource().getServer()).whenCompleteAsync((v,error)->{if(error==null)c.getSource().sendFeedback(()->Text.literal("VANISH_SMOKE_PASS"),false);else{org.slf4j.LoggerFactory.getLogger("vanish-smoke").error("VANISH_SMOKE_FAIL",error);c.getSource().sendError(Text.literal("VANISH_SMOKE_FAIL"));}},c.getSource().getServer());return 1;
    })));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void set(ServerPlayerEntity actor,boolean hidden){try{Class.forName("me.drex.vanish.api.VanishAPI").getMethod("setVanish",ServerPlayerEntity.class,boolean.class).invoke(null,actor,hidden);}catch(ReflectiveOperationException error){throw new CompletionException(error);}}
    private static boolean header(List<Packet<?>> packets,int count){return packets.stream().filter(PlayerListHeaderS2CPacket.class::isInstance).map(PlayerListHeaderS2CPacket.class::cast).anyMatch(p->p.header().getString().contains("Online: "+count));}
    private CompletableFuture<Void> test(MinecraftServer server){
        var normalPackets=new ArrayList<Packet<?>>();var staffPackets=new ArrayList<Packet<?>>();
        var hidden=ProfileSmoke.player(server,"HiddenCore");var normal=ProfileSmoke.player(server,"NormalCore",normalPackets::add);var staff=ProfileSmoke.player(server,"StaffCore",staffPackets::add);var players=List.of(hidden,normal,staff);
        var fixture=(SmokePlayers)server.getPlayerManager();for(var player:players){fixture.shirokai$players().add(player);fixture.shirokai$playerMap().put(player.getUuid(),player);}
        server.getPlayerManager().addToOperators(staff.getGameProfile());
        var storage=new SqliteStorage(FabricLoader.getInstance().getGameDir().resolve("vanish-smoke.db"));
        var permissions=new PermissionService(){public boolean allowed(UUID id,String node){return true;}public String provider(){return "fixture";}};
        var chat=new MinecraftChat(server,storage,permissions,null,null,ChatConfig.defaults(),org.slf4j.LoggerFactory.getLogger("vanish-smoke"),()->players);
        return storage.ready().thenComposeAsync(v->chat.service().join(hidden.getUuid()),server).thenComposeAsync(v->chat.service().join(normal.getUuid()),server).thenComposeAsync(v->chat.service().join(staff.getUuid()),server)
            .thenComposeAsync(v->{
                check(VisibilityService.canSee(server,normal,hidden),"Initially visible player hidden");normalPackets.clear();staffPackets.clear();set(hidden,true);
                check(VisibilityService.isHidden(server,hidden),"Actual Vanish API did not hide player");check(!VisibilityService.canSee(server,normal,hidden)&&VisibilityService.canSee(server,staff,hidden),"Provider visibility/seer permission not respected");
                check(!hidden.canBeSpectated(normal)&&hidden.canBeSpectated(staff),"Actual entity tracking visibility gate not applied");
                check(hidden.isInvisibleTo(normal),"Actual entity render visibility gate not applied");
                check(com.shirokai.core.messages.Messages.player(server,normal,hidden.getUuid()).getString().equals("Jogador offline"),"Clan member label leaked hidden online status");
                var dispatcher=server.getCommandManager().getDispatcher();
                var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("clan invite H",normal.getCommandSource())).getNow(null);
                check(suggestions!=null&&suggestions.getList().stream().noneMatch(s->s.getText().equals("HiddenCore")),"Clan autocomplete leaked hidden actor");
                check(normalPackets.stream().anyMatch(PlayerRemoveS2CPacket.class::isInstance),"Actual provider did not remove roster entry");
                var recipients=new HashSet<UUID>();chat.service().send(hidden.getUuid(),Channel.GLOBAL,"hidden test",(id,message)->recipients.add(id));
                check(!recipients.contains(normal.getUuid())&&recipients.contains(staff.getUuid()),"Core chat leaked hidden actor");
                try{chat.service().privateMessage(normal.getUuid(),hidden.getUuid(),"private test",(id,message)->{});throw new AssertionError("Private message revealed hidden actor");}catch(IllegalStateException expected){}
                normalPackets.clear();check(VisibilityService.count(server,staff,players)==3,"Seer online count wrong");return until.apply(()->header(normalPackets,2));
            },server).thenComposeAsync(v->{
                check(server.getScoreboard().getScoreHolderTeam(hidden.getGameProfile().getName())==null,"Global Core team revealed hidden identity");
                check(normalPackets.stream().filter(PlayerListS2CPacket.class::isInstance).map(PlayerListS2CPacket.class::cast).flatMap(p->p.getEntries().stream()).noneMatch(e->e.profileId().equals(hidden.getUuid())),"Core TAB names leaked hidden player");
                check(normalPackets.stream().filter(PlayerListS2CPacket.class::isInstance).map(PlayerListS2CPacket.class::cast).flatMap(p->p.getEntries().stream()).filter(e->e.displayName()!=null).allMatch(e->e.displayName().getString().endsWith(" 2")),"Per-viewer name cache leaked seer online count");
                set(hidden,false);normalPackets.clear();return until.apply(()->header(normalPackets,3));
            },server).whenCompleteAsync((v,error)->{
                try{set(hidden,false);}finally{
                    server.getPlayerManager().removeFromOperators(staff.getGameProfile());for(var player:players){fixture.shirokai$players().remove(player);fixture.shirokai$playerMap().remove(player.getUuid());chat.service().leave(player.getUuid());}
                    try{storage.close();}catch(Exception close){throw new CompletionException(close);}
                }
            },server);
    }
}
