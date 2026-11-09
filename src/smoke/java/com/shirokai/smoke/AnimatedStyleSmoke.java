package com.shirokai.smoke;

import com.shirokai.core.clans.*;
import com.shirokai.core.customization.*;
import com.shirokai.core.storage.SqliteStorage;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.presentation.*;
import com.shirokai.core.profiles.ProfileMenu;
import com.shirokai.core.titles.*;
import com.shirokai.core.styles.*;
import com.shirokai.core.chat.ChatConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.*;
import net.minecraft.component.DataComponentTypes;
import java.util.*;
import java.util.concurrent.*;
import java.time.Clock;
import static net.minecraft.server.command.CommandManager.literal;

public final class AnimatedStyleSmoke implements ModInitializer {
    public void onInitialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(literal("shirokai-animated-style-smoke").requires(s->s.hasPermissionLevel(4)).executes(c->{
        test(c.getSource().getServer()).whenCompleteAsync((v,e)->{if(e==null)c.getSource().sendFeedback(()->Text.literal("ANIMATED_STYLE_SMOKE_PASS"),false);else{org.slf4j.LoggerFactory.getLogger("animated-style-smoke").error("ANIMATED_STYLE_SMOKE_FAIL",e);c.getSource().sendError(Text.literal("ANIMATED_STYLE_SMOKE_FAIL"));}},c.getSource().getServer());return 1;
    })));}
    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
    private static boolean color(Text text,int rgb){boolean[] found={false};text.visit((style,part)->{if(!part.isEmpty()&&style.getColor()!=null&&style.getColor().getRgb()==rgb)found[0]=true;return Optional.empty();},Style.EMPTY);return found[0];}
    private CompletableFuture<Void> test(net.minecraft.server.MinecraftServer server){
        var storage=new SqliteStorage(FabricLoader.getInstance().getGameDir().resolve("animated-style-"+UUID.randomUUID()+".db"));var owner=ProfileSmoke.player(server,"AnimatedOwner");
        var permissions=new PermissionService(){public boolean allowed(UUID id,String node){return true;}public String provider(){return "fixture";}};
        var clans=new ClanService(new ClanRepository(storage,Clock.systemUTC()),permissions,server,ClanConfig.defaults());clans.cacheOnline(id->true);
        var styles=new StyleEngine(StyleConfig.defaults());var catalog=new PresetCatalog("basic",Map.of("basic",new PresetCatalog.Definition("",PresetCatalog.SURFACES,"<white>%visual:value%</white>"),"pulse",new PresetCatalog.Definition("style.pulse",PresetCatalog.SURFACES,"<white>%visual:value%</white>",List.of("<red>%visual:value%</red>","<blue>%visual:value%</blue>"),20)),styles);
        var choices=new StyleChoiceService(new StyleChoiceRepository(storage),catalog,permissions,clans,server,id->true);choices.animationPreference(id->true);
        var titles=new TitleService(storage,TitleCatalog.defaults(),permissions::allowed,server,id->true,Clock.systemUTC(),(m,e)->{throw new AssertionError(m,e);});
        var renderer=new PresentationRuntime(server,new PresentationPlan(PresentationConfig.defaults(),styles,ChatConfig.defaults()),permissions,titles,()->List.of(owner));renderer.customization(choices);titles.presentation(renderer);clans.presentation(renderer);
        var tick=new java.util.concurrent.atomic.AtomicLong();titles.frameSource(tick::get,id->true);clans.frameSource(tick::get,id->true);
        return clans.create(owner.getUuid(),"Animated",0).thenComposeAsync(c->choices.join(owner.getUuid()),server)
            .thenComposeAsync(v->choices.view(owner.getUuid(),Surface.TITLE,null),server).thenComposeAsync(view->choices.choose(owner.getUuid(),view,"pulse",()->true),server)
            .thenComposeAsync(v->choices.view(owner.getUuid(),Surface.CLAN_NAME,null),server).thenComposeAsync(view->choices.choose(owner.getUuid(),view,"pulse",()->true),server)
            .thenComposeAsync(v->titles.equip(owner.getUuid(),"traveler",true),server).thenRunAsync(()->{
                var oldChat=titles.chatPresentation(owner.getUuid()).getFirst();check(color(oldChat,0xff5555),"First chat frame missing");
                check(color(clans.present(owner.getUuid(),Surface.CLAN_NAME,"Animated"),0xff5555),"Clan frame missing");tick.set(20);
                check(color(titles.chatPresentation(owner.getUuid()).getFirst(),0x5555ff)&&color(oldChat,0xff5555),"Chat snapshot mutated retroactively");
                check(color(clans.present(owner.getUuid(),Surface.CLAN_NAME,"Animated"),0x5555ff),"Clan frame did not advance");
                var menu=new ProfileMenu(125,owner.getInventory(),owner,titles,titles.cached(owner.getUuid()).orElseThrow());owner.currentScreenHandler=menu;
                check(color(menu.getSlot(9).getStack().get(DataComponentTypes.CUSTOM_NAME),0x5555ff),"Profile preview did not use frame");tick.set(40);menu.refreshVisuals();check(color(menu.getSlot(9).getStack().get(DataComponentTypes.CUSTOM_NAME),0xff5555),"Profile animation did not refresh");
                var rules=new EnumMap<VisualTarget,PersonalizationConfig.Rule>(VisualTarget.class);rules.putAll(PersonalizationConfig.defaults().surfaces());rules.put(VisualTarget.MENU,new PersonalizationConfig.Rule(true,false,""));choices.policy(new PersonalizationConfig(1,rules));menu.refreshVisuals();
                check(color(menu.getSlot(9).getStack().get(DataComponentTypes.CUSTOM_NAME),0xffffff),"Area animation control ignored");
                check(choices.resolution(owner.getUuid(),Surface.TITLE).saved().equals("pulse"),"Area control erased choice");
                check(color(renderer.renderAt(owner.getUuid(),Surface.CLAN_NAME,"Animated",VisualTarget.TAB,20,true),0x5555ff),"TAB area control mixed with menu");
                check(color(renderer.renderAt(owner.getUuid(),Surface.CLAN_NAME,"Animated",VisualTarget.SCOREBOARD,20,false),0xffffff),"Viewer disabled animation ignored");
            },server).whenCompleteAsync((v,e)->{owner.closeHandledScreen();try{storage.close();}catch(Exception error){throw new CompletionException(error);}},server);
    }
}
