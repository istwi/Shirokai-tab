package com.shirokai.smoke;

import com.google.gson.Gson;
import com.shirokai.core.presentation.*;
import com.shirokai.core.chat.*;
import com.shirokai.core.clans.*;
import com.shirokai.core.titles.*;
import com.shirokai.core.styles.*;
import com.shirokai.core.tab.*;
import com.shirokai.core.scoreboard.*;
import com.shirokai.core.profiles.ProfileMenu;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.storage.SqliteStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.message.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.text.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import static net.minecraft.server.command.CommandManager.literal;

/** Actual Fabric packets/components/menus; synthetic players, no cryptographic/client rendering claim. */
public final class PresentationSmoke implements ModInitializer {
    private final Function<BooleanSupplier,CompletableFuture<Void>> until;private boolean running;
    public PresentationSmoke(Function<BooleanSupplier,CompletableFuture<Void>> until){this.until=until;}
    public void onInitialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(literal("shirokai-presentation-smoke").requires(s->s.hasPermissionLevel(4)).executes(c->{
        if(running)return 0;running=true;var source=c.getSource();test(source.getServer()).whenCompleteAsync((v,error)->{
            running=false;if(error==null)source.sendFeedback(()->Text.literal("PRESENTATION_SMOKE_PASS"),false);else{org.slf4j.LoggerFactory.getLogger("presentation-smoke").error("PRESENTATION_SMOKE_FAIL",error);source.sendError(Text.literal("PRESENTATION_SMOKE_FAIL"));}
        },source.getServer());return 1;
    })));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static boolean color(Text text,int rgb){boolean[] found={false};text.visit((style,part)->{if(!part.isEmpty()&&style.getColor()!=null&&style.getColor().getRgb()==rgb)found[0]=true;return Optional.empty();},Style.EMPTY);return found[0];}
    private static boolean overlay(List<Packet<?>> packets){return packets.stream().filter(GameMessageS2CPacket.class::isInstance).map(GameMessageS2CPacket.class::cast).anyMatch(GameMessageS2CPacket::overlay);}
    private static ChatMessageS2CPacket signed(List<Packet<?>> packets){return packets.stream().filter(ChatMessageS2CPacket.class::isInstance).map(ChatMessageS2CPacket.class::cast).findFirst().orElseThrow();}
    private CompletableFuture<Void> test(net.minecraft.server.MinecraftServer server){
        var storage=new SqliteStorage(FabricLoader.getInstance().getGameDir().resolve("presentation-"+UUID.randomUUID()+".db"));
        var pa=new ArrayList<Packet<?>>();var pb=new ArrayList<Packet<?>>();var pc=new ArrayList<Packet<?>>();
        var a=ProfileSmoke.player(server,"StyledAlice",pa::add);var b=ProfileSmoke.player(server,"StyledBob",pb::add);var c=ProfileSmoke.player(server,"StyledFar",pc::add);var online=List.of(a,b,c);
        a.setPosition(0,100,0);b.setPosition(10,100,0);c.setPosition(300,100,0);var vip=new boolean[]{true};
        var permissions=new PermissionService(){public boolean allowed(UUID id,String node){return id.equals(a.getUuid())&&(node.equals("shirokaicore.clan.create")||node.equals("visual.vip")&&vip[0]);}public String provider(){return "test-only";}public Metadata metadata(UUID id){return new Metadata(id.equals(a.getUuid())?"vip":"member","","",0);}};
        var titles=new TitleService(storage,TitleCatalog.defaults(),permissions::allowed,server,id->true,Clock.systemUTC(),(message,error)->{});
        var clans=new ClanService(new ClanRepository(storage,Clock.systemUTC()),permissions,server,ClanConfig.defaults());clans.menuEnabled(true);clans.cacheOnline(id->true);
        titles.bindClans(id->clans.cached(id).flatMap(ClanTitle::from),id->clans.membership(id).thenApply(v->null));
        var chatJson=new Gson().toJsonTree(ChatConfig.defaults()).getAsJsonObject();chatJson.addProperty("cooldownMillis",0);chatJson.addProperty("duplicateWindowSeconds",0);final ChatConfig chatConfig;
        try{chatConfig=ChatConfig.parse(chatJson.toString());}catch(Exception e){throw new CompletionException(e);}
        var base=PresentationConfig.defaults();var rule=new PresentationConfig.Rule("vip",100,new TabConfig.Condition("visual.vip","vip",Set.of("minecraft:overworld")),
            Map.of(Surface.TITLE,"<gradient:#ff0000:#0000ff>%visual:value%</gradient>",Surface.CLAN_TAG,"<dark_purple>%visual:value%</dark_purple>",Surface.CLAN_NAME,"<gradient:#ff0000:#0000ff>%visual:value%</gradient>"),Map.of(Channel.GLOBAL,"<green>[G] {tags}</green><white>{name}</white>"));
        var visualConfig=new PresentationConfig(1,true,true,true,"",base.templates(),List.of(rule));var engine=new StyleEngine(StyleConfig.defaults());var plan=new PresentationPlan(visualConfig,engine,chatConfig);
        var renderer=new PresentationRuntime(server,plan,permissions,titles,()->online);titles.presentation(renderer);clans.presentation(renderer);
        var endpoint=new MinecraftChat(server,storage,permissions,titles,clans,chatConfig,org.slf4j.LoggerFactory.getLogger("presentation-smoke"),()->online);endpoint.presentation(renderer);var chat=endpoint.service();
        var tab=new TabRuntime(server,new TabPlan(TabConfig.defaults(),engine),permissions,null,titles,clans,storage,()->"N/D",org.slf4j.LoggerFactory.getLogger("presentation-smoke"),()->online);
        var board=new ScoreboardRuntime(server,new ScoreboardPlan(ScoreboardConfig.defaults(),engine),permissions,null,titles,clans,storage,()->"N/D",null,org.slf4j.LoggerFactory.getLogger("presentation-smoke"),()->online);
        Runnable clear=()->{pa.clear();pb.clear();pc.clear();};
        var untrusted=Text.literal("<op>😀</op>").styled(s->s.withColor(0x123456).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/op me")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Text.literal("data"))).withInsertion("/op me"));
        var composed=engine.renderRich(engine.compile("<bold>%test:value%</bold>"),key->untrusted,0,false);
        check(composed.getString().equals(untrusted.getString())&&untrusted.getStyle().getHoverEvent()!=null,"Rich slot reparsed or source mutated");
        composed.visit((style,part)->{check(style.getClickEvent()==null&&style.getHoverEvent()==null&&style.getInsertion()==null,"Rich slot leaked executable actions");return Optional.empty();},Style.EMPTY);
        var result=storage.ready().thenComposeAsync(v->clans.create(a.getUuid(),"StyledClan",0),server).thenComposeAsync(v->clans.identity(a.getUuid(),"STY",true),server)
            .thenComposeAsync(v->titles.equip(a.getUuid(),"traveler",true),server)
            .thenComposeAsync(v->titles.equip(a.getUuid(),ClanTitle.id(clans.cached(a.getUuid()).orElseThrow().id()),true),server)
            .thenComposeAsync(v->chat.join(a.getUuid()),server).thenComposeAsync(v->chat.join(b.getUuid()),server).thenComposeAsync(v->chat.join(c.getUuid()),server)
            .thenComposeAsync(v->board.join(a.getUuid()),server).thenRunAsync(()->{
                clear.run();String body="Hi @StyledBob <click:run_command:/op>literal</click>";var incoming=SignedMessage.ofUnsigned(a.getUuid(),body);
                endpoint.signed(a,incoming);var packet=signed(pb);
                check(packet.body().content().equals(body)&&Objects.equals(packet.signature(),incoming.signature())&&packet.unsignedContent()==null,"Signed body/signature decorated");
                check(packet.serializedParameters().name().getString().equals("[G] [VIAJANTE] [STY] StyledAlice"),"Selected tags duplicated or lost");
                check(color(packet.serializedParameters().name(),0xff0000)&&color(packet.serializedParameters().name(),0xaa00aa),"Title/clan styles lost in header");
                check(overlay(pb)&&!overlay(pc),"Mention notification escaped addressed recipient");
                clear.run();b.setClientOptions(new SyncedClientOptions("en_us",2,ChatVisibility.FULL,true,127,net.minecraft.util.Arm.RIGHT,true,true));
                var mask=new FilterMask(body.length());mask.markFiltered(4);var filtered=incoming.withFilterMask(mask);endpoint.signed(a,filtered);
                check(signed(pb).filterMask().equals(mask)&&!overlay(pb),"Filter mask/hidden mention leaked");b.setClientOptions(SyncedClientOptions.createDefault());
                clear.run();chat.privateMessage(a.getUuid(),b.getUuid(),body,endpoint::system);
                var system=pb.stream().filter(GameMessageS2CPacket.class::isInstance).map(GameMessageS2CPacket.class::cast).filter(p->!p.overlay()).findFirst().orElseThrow().content();
                check(system.getString().endsWith(body)&&color(system,0xffff55),"System mention not styled literally");
                system.getSiblings().getLast().visit((style,part)->{check(style.getClickEvent()==null,"Body inherited sender click");return Optional.empty();},Style.EMPTY);
                clear.run();chat.send(a.getUuid(),Channel.LOCAL,"@StyledFar outside local",endpoint::system);check(pc.isEmpty(),"Undelivered mention leaked notification");
                vip[0]=false;check(!color(titles.chatPresentation(a.getUuid()).getFirst(),0xff0000),"Revoked visual permission kept premium style");check(titles.cached(a.getUuid()).orElseThrow().chatIds().contains("traveler"),"Visual permission erased title selection");
                vip[0]=true;check(color(titles.chatPresentation(a.getUuid()).getFirst(),0xff0000)&&renderer.plan().config().equals(visualConfig),"Configured style lost after permission restored");
                clear.run();tab.update();board.refresh(a.getUuid());
                var names=pb.stream().filter(PlayerListS2CPacket.class::isInstance).map(PlayerListS2CPacket.class::cast).flatMap(p->p.getEntries().stream()).filter(e->e.profileId().equals(a.getUuid())).findFirst().orElseThrow().displayName();check(!names.getString().contains("VIAJANTE")&&!names.getString().contains("STY"),"Optional tags leaked into TAB");
                // Initial board frame may already be cached; inspect title directly and assert a config frame update below.
                var modified=new ScoreboardConfig(1,20,List.of(new ScoreboardConfig.Layout("default",0,TabConfig.Condition.any(),"Title",List.of(new ScoreboardConfig.Line("title",TabConfig.Condition.any(),"%shirokai:selected_title%")))));
                board.replacePlan(new ScoreboardPlan(modified,engine));var score=pa.stream().filter(ScoreboardScoreUpdateS2CPacket.class::isInstance).map(ScoreboardScoreUpdateS2CPacket.class::cast).findFirst().orElseThrow();check(score.display().orElseThrow().getString().isEmpty(),"Optional tag leaked into sidebar rank");
                var menu=new ProfileMenu(91,a.getInventory(),a,titles,titles.cached(a.getUuid()).orElseThrow());a.currentScreenHandler=menu;
                check(color(menu.getInventory().getStack(9).get(DataComponentTypes.CUSTOM_NAME),0xff0000),"Profile title preview lost style");
                ClanMenu.open(a,clans);
            },server).thenComposeAsync(v->until.apply(()->a.currentScreenHandler instanceof ClanMenu),server).thenRunAsync(()->{
                var menu=(ClanMenu)a.currentScreenHandler;check(color(menu.getInventory().getStack(4).get(DataComponentTypes.CUSTOM_NAME),0xff0000),"Clan menu name lost style");
                check(color(menu.getInventory().getStack(4).get(DataComponentTypes.LORE).lines().getFirst(),0xaa00aa),"Clan tag and name styles not independent");
            },server).thenComposeAsync(v->chat.mute(null,a.getUuid(),0L,"fixture"),server).thenRunAsync(()->{clear.run();endpoint.signed(a,SignedMessage.ofUnsigned(a.getUuid(),"@StyledBob muted"));check(pb.isEmpty()&&pc.isEmpty(),"Mention bypassed mute");},server);
        return result.whenCompleteAsync((v,error)->{tab.close();board.close();for(var p:online){chat.leave(p.getUuid());titles.forget(p.getUuid());clans.forget(p.getUuid());p.closeHandledScreen();}try{storage.close();}catch(Exception e){throw new CompletionException(e);}},server);
    }
}
