package com.shirokai.smoke;

import com.google.gson.*;
import com.shirokai.core.chat.*;
import com.shirokai.core.clans.*;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.storage.SqliteStorage;
import com.shirokai.core.titles.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import static net.minecraft.server.command.CommandManager.literal;

/** Tests real transport methods with captured packets, no simulated authentication or client. */
public final class ChatSmoke implements ModInitializer {
    private boolean running;
    @Override public void onInitialize(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server,resources,success)->{
            if(success)org.slf4j.LoggerFactory.getLogger("shirokai-smoke").info("DATAPACK_RELOAD_SMOKE_READY");
        });
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->{
        dispatcher.register(literal("shirokai-chat-smoke").requires(s->s.hasPermissionLevel(4)).executes(context->{
            if(running)return 0;running=true;var source=context.getSource();test(source.getServer()).whenCompleteAsync((v,error)->{
                running=false;if(error==null)source.sendFeedback(()->Text.literal("CHAT_SMOKE_PASS"),false);
                else{org.slf4j.LoggerFactory.getLogger("shirokai-smoke").error("CHAT_SMOKE_FAIL",error);source.sendError(Text.literal("CHAT_SMOKE_FAIL"));}
            },source.getServer());return 1;
        }));
        dispatcher.register(literal("shirokai-chat-alias-smoke").requires(s->s.hasPermissionLevel(4)).executes(context->{
            var root=context.getSource().getServer().getCommandManager().getDispatcher().getRoot();var msg=root.getChild("msg");
            check(root.getChild("g")!=null&&root.getChild("l")!=null,"Short chat aliases missing after reload");
            check(msg.getChild("targets").getChild("message").getCommand().getClass().getName().startsWith(ChatCommands.class.getName()),"Vanilla private moderation wrapper missing");
            check(root.getChild("tell").getRedirect()==msg&&root.getChild("w").getRedirect()==msg,"Private redirects do not share moderated handler");
            context.getSource().sendFeedback(()->Text.literal("CHAT_ALIAS_SMOKE_PASS"),false);return 1;
        }));
    });}
    private static void check(boolean value,String error){if(!value)throw new AssertionError(error);}
    private static void rejected(Runnable action){try{action.run();}catch(RuntimeException expected){return;}throw new AssertionError("Moderation did not reject operation");}
    private CompletableFuture<Void> test(MinecraftServer server){
        var storage=new SqliteStorage(FabricLoader.getInstance().getGameDir().resolve("chat-smoke-"+UUID.randomUUID()+".db"));
        var pa=new ArrayList<Packet<?>>();var pb=new ArrayList<Packet<?>>();var pc=new ArrayList<Packet<?>>();
        var a=ProfileSmoke.player(server,"ChatAlice",pa::add);var b=ProfileSmoke.player(server,"ChatBob",pb::add);var c=ProfileSmoke.player(server,"ChatCarol",pc::add);
        a.setPosition(0,100,0);b.setPosition(10,100,0);c.setPosition(200,100,0);
        var players=List.of(a,b,c);var nodes=new HashMap<UUID,Set<String>>();nodes.put(a.getUuid(),Set.of("shirokaicore.clan.create","shirokaicore.chat.staff"));nodes.put(b.getUuid(),Set.of("shirokaicore.chat.staff"));
        var permissions=new PermissionService(){public boolean allowed(UUID id,String node){check(server.isOnThread(),"Permission off main thread");return nodes.getOrDefault(id,Set.of()).contains(node);}public String provider(){return "test-only";}};
        var clans=new ClanService(new ClanRepository(storage,Clock.systemUTC()),permissions,server,ClanConfig.defaults());clans.cacheOnline(id->true);
        var titles=new TitleService(storage,TitleCatalog.defaults(),permissions::allowed,server,id->true,Clock.systemUTC(),(message,error)->{});
        titles.bindClans(id->clans.cached(id).flatMap(ClanTitle::from),id->clans.membership(id).thenApply(v->null));
        var json=new Gson().toJsonTree(ChatConfig.defaults()).getAsJsonObject();json.addProperty("cooldownMillis",0);json.addProperty("duplicateWindowSeconds",0);final ChatConfig config;
        try{config=ChatConfig.parse(json.toString());}catch(Exception failure){throw new CompletionException(failure);}
        var endpoint=new MinecraftChat(server,storage,permissions,titles,clans,config,org.slf4j.LoggerFactory.getLogger("shirokai-chat-smoke"),()->players);
        var chat=endpoint.service();Runnable clear=()->{pa.clear();pb.clear();pc.clear();};
        var result=storage.ready().thenComposeAsync(v->clans.create(a.getUuid(),"ChatClan",0),server)
            .thenComposeAsync(clan->clans.identity(a.getUuid(),"CHT",true),server).thenComposeAsync(clan->clans.invite(a.getUuid(),b.getUuid()),server)
            .thenComposeAsync(clan->clans.accept(b.getUuid(),"ChatClan"),server).thenComposeAsync(clan->titles.equip(a.getUuid(),"traveler",true),server)
            .thenComposeAsync(state->titles.equip(a.getUuid(),clans.cached(a.getUuid()).map(snapshot->ClanTitle.id(snapshot.id())).orElseThrow(),true),server)
            .thenComposeAsync(v->chat.join(a.getUuid()),server).thenComposeAsync(v->chat.join(b.getUuid()),server).thenComposeAsync(v->chat.join(c.getUuid()),server)
            .thenRunAsync(()->{
                clear.run();String body="<click:run_command:'/op ChatAlice'>literal</click>";
                check(!endpoint.signed(a,SignedMessage.ofUnsigned(a.getUuid(),body)),"Vanilla broadcast not suppressed");
                var packet=pb.stream().filter(ChatMessageS2CPacket.class::isInstance).map(ChatMessageS2CPacket.class::cast).findFirst().orElseThrow();
                check(packet.sender().equals(a.getUuid())&&packet.body().content().equals(body)&&packet.unsignedContent()==null,"Message body/author altered");
                check(packet.serializedParameters().name().getString().contains("[VIAJANTE] [CHT]"),"Chosen identifications missing");
                clear.run();chat.send(a.getUuid(),Channel.LOCAL,"Near",endpoint::system);check(!pb.isEmpty()&&pc.isEmpty(),"Local message leaked by distance");
                clear.run();chat.send(a.getUuid(),Channel.CLAN,"Clan",endpoint::system);check(!pb.isEmpty()&&pc.isEmpty(),"Clan message leaked");
                clear.run();chat.send(a.getUuid(),Channel.STAFF,"Staff",endpoint::system);check(!pb.isEmpty()&&pc.isEmpty(),"Staff message leaked");
                nodes.remove(b.getUuid());clear.run();chat.send(a.getUuid(),Channel.STAFF,"Staff revoked",endpoint::system);check(pb.isEmpty()&&pc.isEmpty(),"Revoked staff received message");
            },server).thenComposeAsync(v->chat.ignore(b.getUuid(),a.getUuid()),server).thenRunAsync(()->{
                clear.run();chat.send(a.getUuid(),Channel.GLOBAL,"Ignored",endpoint::system);check(pb.isEmpty(),"Ignore bypassed");
                rejected(()->chat.privateMessage(a.getUuid(),b.getUuid(),"Denied",endpoint::system));
            },server).thenComposeAsync(v->chat.ignore(b.getUuid(),a.getUuid()),server).thenRunAsync(()->{
                clear.run();String body="<click:run_command:'/op me'>not markup</click>";chat.privateMessage(a.getUuid(),b.getUuid(),body,endpoint::system);
                check(!pa.isEmpty()&&!pb.isEmpty()&&pc.isEmpty(),"Private recipient mismatch");var message=pb.stream().filter(GameMessageS2CPacket.class::isInstance).map(GameMessageS2CPacket.class::cast).findFirst().orElseThrow().content();
                check(message.getString().contains(body),"Private body parsed");check(message.getSiblings().getLast().getStyle().getClickEvent()==null,"User body inherited click event");
                check(chat.replyTarget(b.getUuid()).equals(a.getUuid()),"Reply peer missing");
            },server).thenComposeAsync(v->chat.mute(null,a.getUuid(),0L,"Moderation"),server).thenRunAsync(()->{
                clear.run();endpoint.signed(a,SignedMessage.ofUnsigned(a.getUuid(),"Muted"));check(pb.isEmpty()&&pc.isEmpty(),"Muted signed adapter leaked");
                rejected(()->chat.privateMessage(a.getUuid(),b.getUuid(),"Muted private",endpoint::system));
            },server).thenComposeAsync(v->chat.mute(null,a.getUuid(),null,""),server).thenComposeAsync(v->chat.select(a.getUuid(),Channel.STAFF),server)
            .thenRunAsync(()->{nodes.remove(a.getUuid());clear.run();endpoint.signed(a,SignedMessage.ofUnsigned(a.getUuid(),"Lost staff"));check(pb.isEmpty()&&pc.isEmpty(),"Lost staff fell back to public");},server);
        return result.whenCompleteAsync((v,error)->{for(var player:players){chat.leave(player.getUuid());titles.forget(player.getUuid());clans.forget(player.getUuid());player.closeHandledScreen();}try{storage.close();}catch(Exception close){throw new CompletionException(close);}},server);
    }
}
