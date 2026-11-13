package com.shirokai.smoke;

import com.google.gson.*;
import com.shirokai.core.chat.*;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.storage.SqliteStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.message.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.text.*;
import java.util.*;
import java.util.concurrent.*;
import static net.minecraft.server.command.CommandManager.literal;

/** Real chat registry/packets and transformed join/quit callbacks, without authenticating a client. */
public final class ChatAppearanceSmoke implements ModInitializer {
    public void onInitialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(
        literal("shirokai-chat-appearance-smoke").requires(s->s.hasPermissionLevel(4)).executes(c->{
            test(c.getSource().getServer()).whenCompleteAsync((v,error)->{
                if(error==null)c.getSource().sendFeedback(()->Text.literal("CHAT_APPEARANCE_SMOKE_PASS"),false);
                else{org.slf4j.LoggerFactory.getLogger("chat-appearance-smoke").error("CHAT_APPEARANCE_SMOKE_FAIL",error);c.getSource().sendError(Text.literal("CHAT_APPEARANCE_SMOKE_FAIL"));}
            },c.getSource().getServer());return 1;
        })));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static ChatMessageS2CPacket packet(List<Packet<?>> packets){return packets.stream().filter(ChatMessageS2CPacket.class::isInstance).map(ChatMessageS2CPacket.class::cast).findFirst().orElseThrow();}
    private static boolean color(Text text,String contains,int rgb){
        boolean[] found={false};text.visit((style,part)->{if(part.contains(contains)&&style.getColor()!=null&&style.getColor().getRgb()==rgb)found[0]=true;return Optional.empty();},Style.EMPTY);return found[0];
    }
    private static Object transformed(Class<?> type,Object target,String suffix,Object...args)throws Exception{
        var method=Arrays.stream(type.getDeclaredMethods()).filter(m->m.getName().contains(suffix)&&m.getParameterCount()==args.length).findFirst().orElseThrow();
        method.setAccessible(true);return method.invoke(target,args);
    }
    private static final class Arguments extends org.spongepowered.asm.mixin.injection.invoke.arg.Args {
        Arguments(Text text){super(new Object[]{text,false});}
        public <T> void set(int index,T value){values[index]=value;}
        public void setAll(Object...args){System.arraycopy(args,0,values,0,values.length);}
    }
    private CompletableFuture<Void> test(net.minecraft.server.MinecraftServer server){
        var storage=new SqliteStorage(FabricLoader.getInstance().getGameDir().resolve("chat-appearance-"+UUID.randomUUID()+".db"));
        var pa=new ArrayList<Packet<?>>();var pb=new ArrayList<Packet<?>>();var pc=new ArrayList<Packet<?>>();
        var a=ProfileSmoke.player(server,"LayoutAlice",pa::add);var b=ProfileSmoke.player(server,"LayoutBob",pb::add);var c=ProfileSmoke.player(server,"LayoutFar",pc::add);
        a.setPosition(0,100,0);b.setPosition(10,100,0);c.setPosition(200,100,0);
        var permitted=new boolean[]{false};
        var permissions=new PermissionService(){public boolean allowed(UUID id,String node){check(server.isOnThread(),"Off-thread permission");return permitted[0];}public String provider(){return "test";}};
        final ChatConfig config;
        try{var json=new Gson().toJsonTree(ChatConfig.defaults()).getAsJsonObject();json.addProperty("cooldownMillis",0);json.addProperty("duplicateWindowSeconds",0);config=ChatConfig.parse(json.toString());}
        catch(Exception error){throw new CompletionException(error);}
        var endpoint=new MinecraftChat(server,storage,permissions,null,null,config,org.slf4j.LoggerFactory.getLogger("chat-appearance-smoke"),()->List.of(a,b,c));
        var chat=endpoint.service();Runnable clear=()->{pa.clear();pb.clear();pc.clear();};
        var previous=ConnectionAnnouncements.install(endpoint);
        return chat.join(a.getUuid()).thenComposeAsync(v->chat.join(b.getUuid()),server).thenComposeAsync(v->chat.join(c.getUuid()),server)
            .thenRunAsync(()->{
                clear.run();var incoming=SignedMessage.ofUnsigned(a.getUuid(),"Global");
                endpoint.signed(a,incoming);var global=packet(pb);
                check(global.serializedParameters().name().getStyle().getHoverEvent()==null,"Core chat still has a hover tooltip");
                check(global.serializedParameters().name().getStyle().getClickEvent()!=null,"Core chat lost click action");
                check(a.getDisplayName().getStyle().getHoverEvent()==null,"Vanilla player name still exposes entity UUID");
                check(a.getDisplayName().getStyle().getClickEvent()!=null,"Vanilla player name lost tell action");
                var normalAppearance=endpoint.appearance();
                endpoint.appearance(new ChatAppearanceConfig(1,true,true,"",true,normalAppearance.bodyColors(),true));
                check(a.getDisplayName().getStyle().getHoverEvent()!=null&&a.getDisplayName().getStyle().getHoverEvent().getAction()==HoverEvent.Action.SHOW_ENTITY,"Hover option failed to restore vanilla identity");
                endpoint.appearance(normalAppearance);
                Text displayed=global.serializedParameters().applyChatDecoration(Text.literal(global.body().content()));
                check(displayed.getString().equals("[G] LayoutAlice: Global"),"Global layout still has vanilla brackets or wrong prefix");
                check(color(displayed,"[G]",0xaaaaaa)&&color(displayed,"Global",0xaaaaaa),"Global prefix/body not gray");
                check(color(displayed,"LayoutAlice",0xffffff),"Global nickname not white");
                check(global.unsignedContent()==null&&Objects.equals(global.signature(),incoming.signature()),"Default color changed signed fields");
            },server).thenComposeAsync(v->chat.select(a.getUuid(),Channel.LOCAL),server).thenRunAsync(()->{
                clear.run();endpoint.signed(a,SignedMessage.ofUnsigned(a.getUuid(),"Local"));var local=packet(pb);
                var displayed=local.serializedParameters().applyChatDecoration(Text.literal(local.body().content()));
                check(displayed.getString().equals("[L] LayoutAlice: Local")&&color(displayed,"[L]",0xffff55)&&color(displayed,"Local",0xffff55),"Local layout/body not yellow");
                check(color(displayed,"LayoutAlice",0xffffff),"Local nickname not white");
                check(pc.isEmpty(),"Styled local escaped distance gate");
                endpoint.shares().providers((id,slot)->{check(id.equals(a.getUuid()),"Share queried recipient instead of author");return Optional.of(Text.literal("[Pikachu "+slot+"]").styled(style->style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Text.literal("Own party")))));},id->{check(id.equals(a.getUuid()),"Wallet queried other player");return Optional.of(Text.literal("[Dollars: 10]"));});
                clear.run();String shareBody="Veja @1 e @carteira";
                var sharedIncoming=SignedMessage.ofUnsigned(a.getUuid(),shareBody);
                endpoint.signed(a,sharedIncoming);var shared=packet(pb);
                check(shared.body().content().equals(shareBody)&&Objects.equals(shared.signature(),sharedIncoming.signature()),"Share changed signed body/signature");
                check(shared.unsignedContent()!=null&&shared.unsignedContent().getString().equals("Veja [Pikachu 1] e [Dollars: 10]"),"Share did not decorate normal chat");
                check(pc.isEmpty(),"Share escaped local audience");
                clear.run();var shareMask=new FilterMask(shareBody.length());shareMask.markFiltered(1);
                b.setClientOptions(new SyncedClientOptions("en_us",2,ChatVisibility.FULL,true,127,net.minecraft.util.Arm.RIGHT,true,true));
                endpoint.signed(a,sharedIncoming.withFilterMask(shareMask));
                check(packet(pb).unsignedContent()==null&&packet(pb).filterMask().equals(shareMask),"Share bypassed filtering");
                b.setClientOptions(SyncedClientOptions.createDefault());
                clear.run();chat.send(a.getUuid(),Channel.LOCAL,shareBody,endpoint::system);
                check(pb.stream().filter(GameMessageS2CPacket.class::isInstance).map(GameMessageS2CPacket.class::cast).anyMatch(p->p.content().getString().contains("[Pikachu 1] e [Dollars: 10]")),"Command route missing shares");
                clear.run();String body="&aGreen &7Gray &rDefault <click:/op>literal</click>";
                endpoint.signed(a,SignedMessage.ofUnsigned(a.getUuid(),body));var colored=packet(pb);
                check(colored.body().content().equals(body)&&colored.unsignedContent()!=null,"Original body lost or optional color missing");
                check(color(colored.unsignedContent(),"Green",0x55ff55)&&color(colored.unsignedContent(),"Gray",0xaaaaaa)&&color(colored.unsignedContent(),"Default",0xffff55),"Legacy colors/reset incorrect");
                colored.unsignedContent().visit((style,text)->{check(style.getClickEvent()==null&&style.getHoverEvent()==null&&style.getInsertion()==null,"Body gained actions");return Optional.empty();},Style.EMPTY);
                clear.run();var mask=new FilterMask(body.length());mask.markFiltered(3);
                b.setClientOptions(new SyncedClientOptions("en_us",2,ChatVisibility.FULL,true,127,net.minecraft.util.Arm.RIGHT,true,true));
                endpoint.signed(a,SignedMessage.ofUnsigned(a.getUuid(),body).withFilterMask(mask));
                var filtered=packet(pb);check(filtered.filterMask().equals(mask)&&filtered.unsignedContent()==null,"Color decoration bypassed filtering");
                b.setClientOptions(SyncedClientOptions.createDefault());
                var base=ChatAppearanceConfig.defaults();endpoint.appearance(new ChatAppearanceConfig(1,true,true,"chat.color",true,base.bodyColors()));
                clear.run();endpoint.signed(a,SignedMessage.ofUnsigned(a.getUuid(),"&aRestricted"));check(packet(pb).unsignedContent()==null,"Color permission bypassed");
                permitted[0]=true;clear.run();endpoint.signed(a,SignedMessage.ofUnsigned(a.getUuid(),"&aAllowed"));check(packet(pb).unsignedContent()!=null,"Granted color permission ignored");
                permitted[0]=false;clear.run();endpoint.signed(a,SignedMessage.ofUnsigned(a.getUuid(),"&aRevoked"));check(packet(pb).unsignedContent()==null,"Revoked color permission cached");
                endpoint.appearance(base);clear.run();chat.send(a.getUuid(),Channel.LOCAL,"&7Command body",endpoint::system);
                var command=pb.stream().filter(GameMessageS2CPacket.class::isInstance).map(GameMessageS2CPacket.class::cast).findFirst().orElseThrow().content();
                check(command.getString().equals("[L] LayoutAlice: Command body")&&color(command,"[L]",0xffff55)&&color(command,"Command body",0xaaaaaa),"Command path layout/colors differ");
                check(color(command,"LayoutAlice",0xffffff),"Command nickname not white");
                var visual=new com.shirokai.core.presentation.PresentationRuntime(server,new com.shirokai.core.presentation.PresentationPlan(com.shirokai.core.presentation.PresentationConfig.defaults(),new com.shirokai.core.styles.StyleEngine(com.shirokai.core.styles.StyleConfig.defaults()),config),permissions,null,()->List.of(a,b,c));
                for(var channel:Channel.values()){
                    var envelope=new ChatService.Envelope(a.getUuid(),"", "Body",channel,b.getGameProfile().getName());
                    var header=visual.header(envelope);var decorated=endpoint.parameters(channel,header).applyChatDecoration(Text.literal("Body"));
                    check(color(decorated,"LayoutAlice",0xffffff),"Styled nickname not white in "+channel);
                    if(channel==Channel.PRIVATE)check(color(decorated,"LayoutBob",0xffffff),"Private target nickname not white");
                }
                try{
                    var arguments=new Arguments(Text.translatable("multiplayer.player.joined",a.getDisplayName()));
                    transformed(net.minecraft.server.PlayerManager.class,server.getPlayerManager(),"shirokai$join",arguments,null,a,null);
                    Text joined=arguments.get(0);check(joined.getString().equals("[+] LayoutAlice")&&color(joined,"[+]",0x55ff55),"Transformed join callback missing green prefix");
                    check(color(joined,"LayoutAlice",0xffffff),"Join nickname not white");
                    Text quit=(Text)transformed(net.minecraft.server.network.ServerPlayNetworkHandler.class,a.networkHandler,"shirokai$quit",Text.translatable("multiplayer.player.left",a.getDisplayName()));
                    check(quit.getString().equals("[-] LayoutAlice")&&color(quit,"[-]",0xff5555),"Transformed quit callback missing red prefix");
                    check(color(quit,"LayoutAlice",0xffffff),"Quit nickname not white");
                    Text custom=Text.literal("Other mod owns announcement");check(ConnectionAnnouncements.replace(custom,a,true)==custom,"Custom announcement overwritten");
                    endpoint.appearance(new ChatAppearanceConfig(1,true,true,"",false,base.bodyColors()));
                    Text vanilla=Text.translatable("multiplayer.player.left",a.getDisplayName());check(ConnectionAnnouncements.replace(vanilla,a,false)==vanilla,"Disabled announcement override still active");
                }catch(Exception error){throw new CompletionException(error);}
                var dispatcher=new com.mojang.brigadier.CommandDispatcher<net.minecraft.server.command.ServerCommandSource>();
                var external=literal("g").executes(context->42).build();dispatcher.getRoot().addChild(external);
                new ChatCommands(()->null).register(dispatcher);
                check(dispatcher.getRoot().getChild("g")==external&&dispatcher.getRoot().getChild("l")!=null,"Short aliases replaced foreign command or not registered");
            },server).whenCompleteAsync((v,error)->{
                ConnectionAnnouncements.install(previous);for(var player:List.of(a,b,c))chat.leave(player.getUuid());
                try{storage.close();}catch(Exception close){throw new CompletionException(close);}
            },server);
    }
}
