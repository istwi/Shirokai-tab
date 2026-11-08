package com.shirokai.smoke;

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
import net.minecraft.text.Text;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import static net.minecraft.server.command.CommandManager.literal;

/** Real vanilla packet/mixin/team APIs, captured with synthetic players; no real client assertion. */
public final class TabSmoke implements ModInitializer{
    private boolean running;
    @Override public void onInitialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(literal("shirokai-tab-smoke").requires(s->s.hasPermissionLevel(4)).executes(context->{
        if(running)return 0;running=true;var source=context.getSource();test(source.getServer()).whenCompleteAsync((value,error)->{
            running=false;if(error==null)source.sendFeedback(()->Text.literal("TAB_SMOKE_PASS"),false);else{org.slf4j.LoggerFactory.getLogger("shirokai-smoke").error("TAB_SMOKE_FAIL",error);source.sendError(Text.literal("TAB_SMOKE_FAIL"));}
        },source.getServer());return 1;
    })));
    }
    private static void check(boolean result,String message){if(!result)throw new AssertionError(message);}
    private CompletableFuture<Void> test(net.minecraft.server.MinecraftServer server){
        var storage=new SqliteStorage(FabricLoader.getInstance().getGameDir().resolve("tab-smoke-"+UUID.randomUUID()+".db"));var pa=new ArrayList<Packet<?>>();var pb=new ArrayList<Packet<?>>();
        var a=ProfileSmoke.player(server,"TabAlice",pa::add);var b=ProfileSmoke.player(server,"TabBob",pb::add);var players=List.of(a,b);
        var metadata=new HashMap<UUID,PermissionService.Metadata>();metadata.put(a.getUuid(),new PermissionService.Metadata("vip","[VIP] ","",100));metadata.put(b.getUuid(),new PermissionService.Metadata("member","","",0));
        var permissions=new PermissionService(){public boolean allowed(UUID player,String node){return player.equals(a.getUuid())&&metadata.get(player).group().equals("vip");}public String provider(){return "test-only";}public Metadata metadata(UUID player){return metadata.get(player);}};
        var titles=new TitleService(storage,TitleCatalog.defaults(),permissions::allowed,server,id->true,Clock.systemUTC(),(message,error)->{});
        titles.bindRanks(permissions::metadata);
        var base=TabConfig.defaults();var condition=new TabConfig.Condition("layout.vip","vip",Set.of("minecraft:overworld"));var layouts=new ArrayList<>(base.layouts());layouts.add(new TabConfig.Layout("vip",100,condition,List.of("<gradient:#ff0000:#0000ff>VIP %shirokai:player%</gradient>"),List.of("Online: %shirokai:online%")));
        var config=new TabConfig(1,20,true,true,base.nameTemplate(),layouts,List.of(new TabConfig.NameRule("vip",100,condition,"<gold>%shirokai:title_prefix%%shirokai:player%</gold>")));
        var endpoint=new TabRuntime(server,new TabPlan(config,new StyleEngine(StyleConfig.defaults())),permissions,new PlaceholderService((key,error)->{}),titles,null,storage,()->"N/D",org.slf4j.LoggerFactory.getLogger("shirokai-tab-smoke"),()->players);
        var board=server.getScoreboard();String foreignName="smoke_external";
        var result=storage.ready().thenComposeAsync(v->storage.setAnimations(a.getUuid(),false),server).thenComposeAsync(v->storage.setAnimations(b.getUuid(),false),server)
            .thenComposeAsync(v->titles.view(a.getUuid()),server).thenRunAsync(()->{
                endpoint.join(a.getUuid());endpoint.join(b.getUuid());pa.clear();pb.clear();endpoint.update();
                var header=pa.stream().filter(PlayerListHeaderS2CPacket.class::isInstance).map(PlayerListHeaderS2CPacket.class::cast).findFirst().orElseThrow();check(header.header().getString().equals("VIP TabAlice"),"Viewer layout not selected");
                var packet=pb.stream().filter(PlayerListS2CPacket.class::isInstance).map(PlayerListS2CPacket.class::cast).findFirst().orElseThrow();
                check(packet.getActions().equals(EnumSet.of(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME)),"Unexpected list action");
                var entry=packet.getEntries().stream().filter(e->e.profileId().equals(a.getUuid())).findFirst().orElseThrow();check(entry.displayName().getString().equals("[VIP] TabAlice"),"Selected title missing from TAB");
                check(entry.latency()==a.networkHandler.getLatency(),"Latency was fabricated");check(board.getScoreHolderTeam(a.getGameProfile().getName()).getName().equals(TeamOwnership.teamName(100)),"Team priority wrong");
                pa.clear();pb.clear();endpoint.update();check(pa.isEmpty()&&pb.isEmpty(),"Unchanged view sent duplicate packets");
                metadata.put(a.getUuid(),new PermissionService.Metadata("member","","",5));endpoint.update();
                var change=pb.stream().filter(PlayerListS2CPacket.class::isInstance).map(PlayerListS2CPacket.class::cast).findFirst().orElseThrow();check(change.getEntries().size()==1&&change.getEntries().getFirst().profileId().equals(a.getUuid()),"Changed source rebuilt whole list");
                check(board.getScoreHolderTeam(a.getGameProfile().getName()).getName().equals(TeamOwnership.teamName(5)),"Group change not reflected");
                var foreign=board.addTeam(foreignName);foreign.setPrefix(Text.literal("[EXTERNAL] "));board.addScoreHolderToTeam(b.getGameProfile().getName(),foreign);pa.clear();pb.clear();endpoint.update();
                check(board.getScoreHolderTeam(b.getGameProfile().getName())==foreign,"Foreign team stolen");
                var restore=pa.stream().filter(PlayerListS2CPacket.class::isInstance).map(PlayerListS2CPacket.class::cast).flatMap(p->p.getEntries().stream()).filter(e->e.profileId().equals(b.getUuid())).findFirst().orElseThrow();check(restore.displayName()==null,"Foreign team formatting not restored");
                pa.clear();pb.clear();endpoint.update();check(pa.isEmpty()&&pb.isEmpty(),"Foreign names repeatedly overwritten");
            },server).thenComposeAsync(v->endpoint.toggleAnimations(a.getUuid()),server).thenComposeAsync(enabled->{check(enabled,"Animation preference toggle failed");return storage.preferences(a.getUuid());},server)
            .thenRunAsync(()->{pa.clear();pb.clear();for(int i=0;i<20;i++)endpoint.tick();check(pa.stream().anyMatch(PlayerListHeaderS2CPacket.class::isInstance),"Animation frame did not update");check(pb.stream().noneMatch(PlayerListHeaderS2CPacket.class::isInstance),"Disabled viewer received animation frame");
                endpoint.close();check(board.getScoreHolderTeam(a.getGameProfile().getName())==null,"Owned team leaked on stop");check(board.getScoreHolderTeam(b.getGameProfile().getName())==board.getTeam(foreignName),"Foreign team deleted");
            },server);
        return result.whenCompleteAsync((v,error)->{
            endpoint.close();var foreign=board.getTeam(foreignName);if(foreign!=null)board.removeTeam(foreign);titles.forget(a.getUuid());a.closeHandledScreen();b.closeHandledScreen();try{storage.close();}catch(Exception close){throw new CompletionException(close);}
        },server);
    }
}
