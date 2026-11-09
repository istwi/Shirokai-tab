package com.shirokai.core.clans;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.*;

/** Read-only history, authorized against the exact clan inside the SQL query. */
public final class ClanHistoryMenu extends GenericContainerScreenHandler {
    private final ServerPlayerEntity owner;private final ClanService service;private final UUID clan;private final int page;private final int size;
    private boolean closed;
    private ClanHistoryMenu(int sync,PlayerInventory inventory,ServerPlayerEntity owner,ClanService service,UUID clan,int page,List<ClanMaintenance.Event> events){
        this(sync,inventory,owner,service,clan,page,events,new SimpleInventory(54));
    }
    private ClanHistoryMenu(int sync,PlayerInventory inventory,ServerPlayerEntity owner,ClanService service,UUID clan,int page,List<ClanMaintenance.Event> events,SimpleInventory display){
        super(ScreenHandlerType.GENERIC_9X6,sync,inventory,display,6);this.owner=owner;this.service=service;this.clan=clan;this.page=page;this.size=events.size();
        for(int i=0;i<events.size();i++){var e=events.get(i);var item=new ItemStack(Items.PAPER);item.set(DataComponentTypes.CUSTOM_NAME,Text.literal("#"+e.id()+" • "+e.action()).formatted(Formatting.YELLOW));
            item.set(DataComponentTypes.LORE,new LoreComponent(List.of(Text.literal(e.details()).formatted(Formatting.GRAY),Text.literal("Autor: "+e.actor()).formatted(Formatting.GRAY),Text.literal(java.time.Instant.ofEpochMilli(e.at()).toString()).formatted(Formatting.DARK_GRAY))));display.setStack(9+i,item);}
        button(display,45,"Página anterior");button(display,49,"Atualizar • página "+(page+1));button(display,50,"Voltar ao clã");button(display,53,"Próxima página");
    }
    private static void button(SimpleInventory display,int slot,String name){var item=new ItemStack(Items.ARROW);item.set(DataComponentTypes.CUSTOM_NAME,Text.literal(name).formatted(Formatting.AQUA));display.setStack(slot,item);}
    public static void open(ServerPlayerEntity player,ClanService service,UUID clan,int page){
        if(!service.menuEnabled()){player.sendMessage(Text.literal("Menu de clã desativado."),false);return;}
        var previous=player.currentScreenHandler;
        service.maintenance().memberHistory(player.getUuid(),clan,page).whenCompleteAsync((events,error)->{
            if(player.isDisconnected()||player.currentScreenHandler!=previous||!service.menuEnabled())return;
            if(error!=null){while(error.getCause()!=null)error=error.getCause();player.sendMessage(Text.literal("Histórico: "+error.getMessage()).formatted(Formatting.RED),false);return;}
            if(service.cached(player.getUuid()).filter(c->c.id().equals(clan)).isEmpty())return;
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((sync,inventory,ignored)->new ClanHistoryMenu(sync,inventory,player,service,clan,page,events),Text.literal("✦ HISTÓRICO DO CLÃ")));
        },player.getServer());
    }
    @Override public boolean canUse(PlayerEntity player){return !closed&&player==owner&&!owner.isDisconnected()&&service.menuEnabled()&&service.cached(owner.getUuid()).filter(c->c.id().equals(clan)).isPresent();}
    @Override public ItemStack quickMove(PlayerEntity player,int slot){return ItemStack.EMPTY;}
    @Override public void onSlotClick(int slot,int button,SlotActionType action,PlayerEntity player){
        if(!canUse(player)||owner.currentScreenHandler!=this||action!=SlotActionType.PICKUP||button!=0)return;
        if(slot==50){ClanMenu.open(owner,service);return;}
        if(slot==49)open(owner,service,clan,page);else if(slot==45&&page>0)open(owner,service,clan,page-1);else if(slot==53&&size==10)open(owner,service,clan,page+1);
    }
    @Override public void onClosed(PlayerEntity player){closed=true;super.onClosed(player);}
}
