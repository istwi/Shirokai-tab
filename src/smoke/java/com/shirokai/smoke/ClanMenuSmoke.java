package com.shirokai.smoke;

import com.shirokai.core.clans.*;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.storage.SqliteStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import static net.minecraft.server.command.CommandManager.literal;

/** Handler integration in a real server, using synthetic players with no client transport. */
public final class ClanMenuSmoke implements ModInitializer {
    private final Function<BooleanSupplier,CompletableFuture<Void>> until;
    private boolean running;
    public ClanMenuSmoke(Function<BooleanSupplier,CompletableFuture<Void>> until){this.until=until;}
    @Override public void onInitialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(
        literal("shirokai-clan-menu-smoke").requires(s->s.hasPermissionLevel(4)).executes(context->{
            if(running)return 0;running=true;var source=context.getSource();test(source.getServer()).whenCompleteAsync((v,error)->{
                running=false;if(error==null)source.sendFeedback(()->Text.literal("CLAN_MENU_SMOKE_PASS"),false);
                else{org.slf4j.LoggerFactory.getLogger("shirokai-smoke").error("CLAN_MENU_SMOKE_FAIL",error);source.sendError(Text.literal("CLAN_MENU_SMOKE_FAIL"));}
            },source.getServer());return 1;
        })));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static ClanMenu menu(ServerPlayerEntity player){return (ClanMenu)player.currentScreenHandler;}
    private static void click(ServerPlayerEntity player,int slot){menu(player).onSlotClick(slot,0,SlotActionType.PICKUP,player);}
    private static int memberSlot(ServerPlayerEntity owner,UUID target){
        for(int slot=9;slot<45;slot++){var name=menu(owner).getSlot(slot).getStack().get(DataComponentTypes.CUSTOM_NAME);if(name!=null&&name.getString().equals("Offline • "+target.toString().substring(0,8)))return slot;}
        throw new AssertionError("Member not listed");
    }
    private CompletableFuture<Void> test(MinecraftServer server){
        var storage=new SqliteStorage(FabricLoader.getInstance().getGameDir().resolve("clan-menu-"+UUID.randomUUID()+".db"));
        var leader=ProfileSmoke.player(server,"ClanLeader");var member=ProfileSmoke.player(server,"ClanMember");
        UUID a=leader.getUuid(),b=member.getUuid();leader.getInventory().setStack(0,new ItemStack(Items.DIAMOND,5));
        var permissions=new PermissionService(){public boolean allowed(UUID player,String node){return true;}public String provider(){return "test-only";}};
        var clans=new ClanService(new ClanRepository(storage,Clock.systemUTC()),permissions,server,ClanConfig.defaults());clans.cacheOnline(id->true);
        var previous=leader.currentScreenHandler;
        ClanMenu.open(leader,clans);check(leader.currentScreenHandler==previous,"Paused clan menu opened");
        var flow=storage.ready().thenComposeAsync(v->clans.create(a,"MenuClan",0),server)
            .thenApplyAsync(clan->{
                com.shirokai.core.clans.ClanIdentityEditor.open(leader,clans,clan,false);
                check(leader.currentScreenHandler==previous,"Paused clan editor opened");
                clans.menuEnabled(true);return clan;
            },server)
            .thenComposeAsync(clan->clans.invite(a,b),server).thenComposeAsync(clan->{ClanMenu.open(member,clans);return until.apply(()->member.currentScreenHandler instanceof ClanMenu);},server)
            .thenComposeAsync(v->{
                var shownPage=menu(member).page();clans.menuEnabled(false);check(!menu(member).canUse(member),"Disabled open clan screen still accepts actions");
                click(member,22);check(menu(member).page()==shownPage,"Disabled screen changed pages");
                clans.menuEnabled(true);click(member,22);check(menu(member).page()==ClanMenu.Page.INVITES,"Invite page missing");click(member,9);return until.apply(()->clans.cached(b).isPresent()&&menu(member).page()==ClanMenu.Page.HOME);},server)
            .thenComposeAsync(v->{ClanMenu.open(leader,clans);return until.apply(()->leader.currentScreenHandler instanceof ClanMenu);},server)
            .thenComposeAsync(v->{click(leader,33);return until.apply(()->leader.currentScreenHandler instanceof com.shirokai.core.clans.ClanHistoryMenu);},server)
            .thenComposeAsync(v->{
                var history=(com.shirokai.core.clans.ClanHistoryMenu)leader.currentScreenHandler;
                check(!history.canUse(member)&&history.quickMove(leader,9).isEmpty(),"History menu ownership/inventory leak");
                check(!history.getSlot(9).getStack().isEmpty(),"Clan history missing");history.onSlotClick(50,0,SlotActionType.PICKUP,leader);return until.apply(()->leader.currentScreenHandler instanceof ClanMenu);
            },server)
            .thenComposeAsync(v->clans.invite(a,UUID.fromString("33333333-3333-3333-3333-333333333333")),server)
            .thenComposeAsync(v->{click(leader,49);return until.apply(()->menu(leader).getSlot(31).getStack().get(net.minecraft.component.DataComponentTypes.LORE).lines().getFirst().getString().equals("1"));},server)
            .thenComposeAsync(v->{click(leader,31);check(menu(leader).page()==ClanMenu.Page.SENT_INVITES,"Sent invites page missing");click(leader,9);return until.apply(()->menu(leader).getSlot(9).getStack().isEmpty());},server)
            .thenComposeAsync(v->{click(leader,50);click(leader,30);click(leader,20);return until.apply(()->leader.currentScreenHandler instanceof com.shirokai.core.clans.ClanIdentityEditor);},server)
            .thenComposeAsync(v->{
                var editor=(com.shirokai.core.clans.ClanIdentityEditor)leader.currentScreenHandler;
                check(editor.setNewItemName("MenuEdited"),"Editor refused text");
                for(var action:SlotActionType.values())if(action!=SlotActionType.PICKUP)editor.onSlotClick(0,0,action,leader);
                editor.onSlotClick(0,0,SlotActionType.PICKUP,member);
                check(clans.cached(a).orElseThrow().name().equals("MenuClan"),"Invalid editor action changed identity");
                check(editor.quickMove(leader,0).isEmpty(),"Editor transferred paper");
                editor.onSlotClick(0,0,SlotActionType.PICKUP,leader);
                return until.apply(()->leader.currentScreenHandler instanceof ClanMenu&&clans.cached(a).orElseThrow().name().equals("MenuEdited"));
            },server).thenComposeAsync(v->{
                check(leader.getInventory().getStack(0).getCount()==5&&leader.experienceLevel==0,"Editor altered inventory or XP");
                check(!leader.getInventory().contains(new ItemStack(Items.PAPER)),"Editor leaked display paper");
                click(leader,30);click(leader,24);return until.apply(()->leader.currentScreenHandler instanceof com.shirokai.core.clans.ClanIdentityEditor);
            },server).thenComposeAsync(v->{
                var editor=(com.shirokai.core.clans.ClanIdentityEditor)leader.currentScreenHandler;editor.setNewItemName("CANCEL");leader.closeHandledScreen();
                editor.onSlotClick(0,0,SlotActionType.PICKUP,leader);check(clans.cached(a).orElseThrow().tag()==null,"Closed editor saved draft");
                ClanMenu.open(leader,clans);return until.apply(()->leader.currentScreenHandler instanceof ClanMenu);
            },server)
            .thenComposeAsync(v->{var menu=menu(leader);check(!menu.canUse(member),"Other player owns menu");
                for(var action:SlotActionType.values())if(action!=SlotActionType.PICKUP)menu.onSlotClick(40,0,action,leader);
                menu.onSlotClick(40,0,SlotActionType.PICKUP,member);menu.onSlotClick(-999,0,SlotActionType.PICKUP,leader);
                check(menu.quickMove(leader,4).isEmpty(),"Menu icon transferred");check(leader.getInventory().getStack(0).getCount()==5,"Inventory altered");
                click(leader,20);click(leader,memberSlot(leader,b));click(leader,20);
                return until.apply(()->clans.cached(b).orElseThrow().members().get(b)==ClanSnapshot.Role.OFFICER&&menu(leader).page()==ClanMenu.Page.HOME);
            },server).thenComposeAsync(v->{
                click(leader,20);click(leader,memberSlot(leader,b));click(leader,40);check(menu(leader).page()==ClanMenu.Page.TRANSFER,"Transfer confirmation missing");
                return clans.transferOffer(b);
            },server).thenComposeAsync(offer->{check(offer.isEmpty(),"Transfer requested before confirming");click(leader,22);return until.apply(()->menu(leader).page()==ClanMenu.Page.HOME);},server)
            .thenComposeAsync(v->{click(member,49);return until.apply(()->!menu(member).getSlot(46).getStack().isEmpty());},server)
            .thenComposeAsync(v->{click(member,46);return until.apply(()->clans.cached(b).orElseThrow().leader().equals(b)&&!menu(member).getSlot(40).getStack().isEmpty());},server)
            .thenComposeAsync(v->{click(member,40);return until.apply(()->menu(member).page()==ClanMenu.Page.DISBAND);},server)
            .thenComposeAsync(v->{check(clans.cached(b).isPresent(),"First click deleted clan");click(member,24);return until.apply(()->menu(member).page()==ClanMenu.Page.HOME);},server)
            .thenComposeAsync(v->clans.disband(b,true).handleAsync((value,error)->{check(error!=null,"Cancel retained disband confirmation");return null;},server),server)
            .thenComposeAsync(v->{click(member,40);return until.apply(()->menu(member).page()==ClanMenu.Page.DISBAND);},server)
            .thenComposeAsync(v->{
                var shownPage=menu(member).page();clans.menuEnabled(false);check(!menu(member).canUse(member),"Disabled open clan screen still accepts actions");
                click(member,22);check(menu(member).page()==shownPage,"Disabled screen changed pages");
                clans.menuEnabled(true);click(member,22);return until.apply(()->clans.cached(b).isEmpty()&&menu(member).page()==ClanMenu.Page.HOME);},server)
            .thenRunAsync(()->{check(clans.cached(a).isEmpty(),"Disband left membership");check(leader.getInventory().getStack(0).getCount()==5,"Items changed");
                var closed=menu(member);click(member,50);check(member.currentScreenHandler!=closed&&!closed.canUse(member),"Menu remained usable after close");
            },server);
        return flow.whenCompleteAsync((v,error)->{leader.closeHandledScreen();member.closeHandledScreen();try{storage.close();}catch(Exception close){throw new CompletionException(close);}},server);
    }
}
