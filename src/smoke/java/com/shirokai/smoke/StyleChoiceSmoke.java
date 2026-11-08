package com.shirokai.smoke;

import com.shirokai.core.customization.*;
import com.shirokai.core.presentation.*;
import com.shirokai.core.styles.*;
import com.shirokai.core.titles.*;
import com.shirokai.core.clans.*;
import com.shirokai.core.chat.ChatConfig;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.storage.SqliteStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import static net.minecraft.server.command.CommandManager.literal;

/** Native menus and the shared title rendering path in a bootstrapped Fabric server. */
public final class StyleChoiceSmoke implements ModInitializer {
    private final Function<BooleanSupplier,CompletableFuture<Void>> until;
    public StyleChoiceSmoke(Function<BooleanSupplier,CompletableFuture<Void>> until){this.until=until;}
    public void onInitialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(
        literal("shirokai-style-smoke").requires(s->s.hasPermissionLevel(4)).executes(c->{
            var source=c.getSource();test(source.getServer()).whenCompleteAsync((v,error)->{
                if(error==null)source.sendFeedback(()->Text.literal("STYLE_CHOICE_SMOKE_PASS"),false);
                else{org.slf4j.LoggerFactory.getLogger("style-smoke").error("STYLE_CHOICE_SMOKE_FAIL",error);source.sendError(Text.literal("STYLE_CHOICE_SMOKE_FAIL"));}
            },source.getServer());return 1;
        })));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static boolean color(Text text,int rgb){boolean[] found={false};text.visit((style,part)->{
        if(!part.isEmpty()&&style.getColor()!=null&&style.getColor().getRgb()==rgb)found[0]=true;return Optional.empty();
    },Style.EMPTY);return found[0];}
    private CompletableFuture<Void> test(net.minecraft.server.MinecraftServer server){
        var root=FabricLoader.getInstance().getGameDir().resolve("style-smoke-"+UUID.randomUUID());
        var storage=new SqliteStorage(root.resolve("data.db"));
        var packetsA=new ArrayList<net.minecraft.network.packet.Packet<?>>();var packetsB=new ArrayList<net.minecraft.network.packet.Packet<?>>();
        var a=ProfileSmoke.player(server,"StyleLeader",packetsA::add);var b=ProfileSmoke.player(server,"StyleMember",packetsB::add);
        var premium=new AtomicBoolean(true);
        PermissionService permissions=new PermissionService(){
            public boolean allowed(UUID id,String node){check(server.isOnThread(),"Permissions off server thread");return node.equals("shirokaicore.clan.create")||premium.get();}
            public String provider(){return "smoke";}
        };
        var styles=new StyleEngine(StyleConfig.defaults());
        final PresetCatalog catalog;
        try{catalog=PresetCatalog.load(root.resolve("presets.json"),styles);}catch(Exception error){return CompletableFuture.failedFuture(error);}
        var clans=new ClanService(new ClanRepository(storage,Clock.systemUTC()),permissions,server,ClanConfig.defaults());clans.cacheOnline(id->true);
        var choices=new StyleChoiceService(new StyleChoiceRepository(storage),catalog,permissions,clans,server,id->id.equals(a.getUuid())||id.equals(b.getUuid()));
        var titles=new TitleService(storage,TitleCatalog.defaults(),permissions::allowed,server,id->true,Clock.systemUTC(),(m,e)->{throw new AssertionError(m,e);});
        var presentation=new PresentationRuntime(server,new PresentationPlan(PresentationConfig.defaults(),styles,ChatConfig.defaults()),permissions,titles,()->List.of(a,b));
        presentation.customization(choices);titles.presentation(presentation);clans.presentation(presentation);
        var logger=org.slf4j.LoggerFactory.getLogger("style-smoke");
        var tab=new com.shirokai.core.tab.TabRuntime(server,new com.shirokai.core.tab.TabPlan(com.shirokai.core.tab.TabConfig.defaults(),styles),permissions,null,titles,clans,storage,()->"N/D",logger,()->List.of(a,b));
        var board=new com.shirokai.core.scoreboard.ScoreboardRuntime(server,new com.shirokai.core.scoreboard.ScoreboardPlan(com.shirokai.core.scoreboard.ScoreboardConfig.defaults(),styles),permissions,null,titles,clans,storage,()->"N/D",null,logger,()->List.of(a,b));
        titles.bindClans(id->clans.cached(id).flatMap(ClanTitle::from),id->clans.membership(id).thenApply(v->null));
        UUID[] clanId={null};StyleChoiceService.View[] stale={null};StyleChoiceMenu[] menu={null};
        return clans.create(a.getUuid(),"Styled",0).thenComposeAsync(clan->{clanId[0]=clan.id();return clans.identity(a.getUuid(),"STY",true);},server)
            .thenComposeAsync(c->clans.invite(a.getUuid(),b.getUuid()),server)
            .thenComposeAsync(c->clans.accept(b.getUuid(),"Styled"),server)
            .thenComposeAsync(c->choices.join(a.getUuid()),server).thenComposeAsync(v->choices.join(b.getUuid()),server)
            .thenComposeAsync(v->board.join(a.getUuid()),server)
            .thenComposeAsync(v->titles.equip(a.getUuid(),"traveler",true),server)
            .thenComposeAsync(v->{
                StyleChoiceMenu.open(a,choices,Surface.TITLE,null);return until.apply(()->a.currentScreenHandler instanceof StyleChoiceMenu);
            },server).thenComposeAsync(v->{
                menu[0]=(StyleChoiceMenu)a.currentScreenHandler;
                menu[0].onSlotClick(10,0,SlotActionType.QUICK_MOVE,a);
                check(choices.resolution(a.getUuid(),Surface.TITLE).saved()==null,"Quick move changed preference");
                menu[0].onSlotClick(10,0,SlotActionType.PICKUP,b);
                check(choices.resolution(a.getUuid(),Surface.TITLE).saved()==null,"Other owner changed preference");
                premium.set(false);menu[0].onSlotClick(10,0,SlotActionType.PICKUP,a);
                check(choices.resolution(a.getUuid(),Surface.TITLE).saved()==null,"Revoked permission accepted old button");
                premium.set(true);menu[0].onSlotClick(10,0,SlotActionType.PICKUP,a);
                return until.apply(()->"gold".equals(choices.resolution(a.getUuid(),Surface.TITLE).saved()));
            },server).thenComposeAsync(v->{
                var visual=titles.chatPresentation(a.getUuid()).getFirst();
                check(color(visual,0xffaa00),"Equipped title did not use saved preset");
                var base=PresentationConfig.defaults();
                var fixedConfig=new PresentationConfig(1,true,true,true,"",base.templates(),List.of(),Map.of(
                    "owner","<gradient:#ff3333:#ffcc33>%visual:value%</gradient>",
                    "admin","<gradient:#3355ff:#33ffff>%visual:value%</gradient>"));
                presentation.replacePlan(new PresentationPlan(fixedConfig,styles,ChatConfig.defaults()));
                var owner=new TitleDefinition("owner","Owner","[OWNER]","",TitleDefinition.Category.ROLE,100,"title.owner",false,true,true,true);
                var admin=new TitleDefinition("admin","Admin","[ADMIN]","",TitleDefinition.Category.ROLE,99,"title.admin",false,true,true,true);
                check(color(titles.display(a.getUuid(),owner),0xff3333),"Owner fixed gradient overridden by personal preset");
                check(color(titles.display(a.getUuid(),admin),0x3355ff),"Admin inherited owner gradient");
                check(color(titles.chatPresentation(a.getUuid()).getFirst(),0xffaa00),"Unconfigured special title lost personal preset");
                presentation.replacePlan(new PresentationPlan(base,styles,ChatConfig.defaults()));

                packetsA.clear();packetsB.clear();tab.update();
                var name=packetsB.stream().filter(net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.class::isInstance)
                    .map(net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.class::cast).flatMap(packet->packet.getEntries().stream())
                    .filter(entry->entry.profileId().equals(a.getUuid())).findFirst().orElseThrow().displayName();
                check(!name.getString().contains("VIAJANTE"),"Optional tag leaked into TAB");
                var boardConfig=new com.shirokai.core.scoreboard.ScoreboardConfig(1,20,List.of(new com.shirokai.core.scoreboard.ScoreboardConfig.Layout("default",0,com.shirokai.core.tab.TabConfig.Condition.any(),"Styles",List.of(new com.shirokai.core.scoreboard.ScoreboardConfig.Line("title",com.shirokai.core.tab.TabConfig.Condition.any(),"%shirokai:selected_title%")))));
                board.replacePlan(new com.shirokai.core.scoreboard.ScoreboardPlan(boardConfig,styles));
                check(packetsA.stream().filter(net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket.class::isInstance)
                    .map(net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket.class::cast).anyMatch(packet->packet.display().map(text->text.getString().isEmpty()).orElse(false)),"Optional tag leaked into sidebar rank");
                premium.set(false);
                check(color(titles.display(a.getUuid(),owner),0xffaa00),"Personal fallback changed the fixed default role style");
                check(choices.resolution(a.getUuid(),Surface.TITLE).status()==PresetCatalog.Status.UNAVAILABLE,"Permission loss not detected");
                check(color(titles.chatPresentation(a.getUuid()).getFirst(),0x55ffff),"Fallback not rendered");
                check("gold".equals(choices.resolution(a.getUuid(),Surface.TITLE).saved()),"Permission loss erased ID");
                premium.set(true);check(color(titles.chatPresentation(a.getUuid()).getFirst(),0xffaa00),"Recovery did not restore preset");
                a.closeHandledScreen();menu[0].onSlotClick(9,0,SlotActionType.PICKUP,a);
                check("gold".equals(choices.resolution(a.getUuid(),Surface.TITLE).saved()),"Closed menu changed selection");
                return choices.view(a.getUuid(),Surface.CLAN_NAME,clanId[0]);
            },server).thenComposeAsync(view->{stale[0]=view;return choices.choose(a.getUuid(),view,"gold",()->true);},server)
            .thenComposeAsync(v->choices.view(a.getUuid(),Surface.CLAN_TAG,clanId[0]),server)
            .thenComposeAsync(view->choices.choose(a.getUuid(),view,"basic",()->true),server)
            .thenComposeAsync(v->{
                check(color(presentation.render(b.getUuid(),Surface.CLAN_NAME,"Styled"),0xffaa00),"Member did not see clan name choice");
                check(color(presentation.render(b.getUuid(),Surface.CLAN_TAG,"STY"),0x55ffff),"Clan tag not independent");
                return titles.equip(b.getUuid(),"clan_"+clanId[0],true);
            },server).thenComposeAsync(v->{
                check(color(titles.chatPresentation(b.getUuid()).getFirst(),0x55ffff),"Clan title path lost clan style");
                return choices.view(a.getUuid(),Surface.TITLE,null);
            },server).thenComposeAsync(view->{
                choices.replaceCatalog(catalog);
                var replacement=new PresetCatalog("basic",Map.of("basic",catalog.presets().get("basic").definition()),styles);
                choices.replaceCatalog(replacement);
                check(choices.resolution(a.getUuid(),Surface.TITLE).status()==PresetCatalog.Status.REMOVED,"Removed preset not explicit");
                check(color(titles.chatPresentation(a.getUuid()).getFirst(),0x55ffff),"Removed preset did not fallback");
                try{choices.choose(a.getUuid(),view,"basic",()->true);throw new AssertionError("Stale catalog accepted");}catch(IllegalStateException expected){}
                choices.replaceCatalog(catalog);
                check(color(titles.chatPresentation(a.getUuid()).getFirst(),0xffaa00),"Restored catalog did not restore ID");
                return clans.transfer(a.getUuid(),b.getUuid());
            },server).thenComposeAsync(c->clans.answerTransfer(b.getUuid(),true),server)
            .thenComposeAsync(c->choices.choose(a.getUuid(),stale[0],"basic",()->true).handleAsync((v,error)->{
                check(error!=null,"Old leader changed clan style");choices.forget(a.getUuid());choices.forget(b.getUuid());return (Void)null;
            },server),server)
            .whenCompleteAsync((v,error)->{tab.close();board.close();a.closeHandledScreen();b.closeHandledScreen();try{storage.close();}catch(Exception close){throw new CompletionException(close);}},server);
    }
}
