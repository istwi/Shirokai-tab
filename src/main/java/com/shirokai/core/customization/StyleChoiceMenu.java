package com.shirokai.core.customization;

import com.shirokai.core.presentation.Surface;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.*;

/** Menu identity, exact clan UUID, catalog generation and row revision are bound to the displayed choices. */
public final class StyleChoiceMenu extends GenericContainerScreenHandler {
    private final ServerPlayerEntity owner;
    private final StyleChoiceService service;
    private final SimpleInventory display;
    private StyleChoiceService.View view;
    private List<String> entries;
    private int page;
    private boolean busy,closed;
    private StyleChoiceMenu(int syncId,PlayerInventory inventory,ServerPlayerEntity owner,StyleChoiceService service,StyleChoiceService.View view){
        this(syncId,inventory,owner,service,view,new SimpleInventory(54));
    }
    private StyleChoiceMenu(int syncId,PlayerInventory inventory,ServerPlayerEntity owner,StyleChoiceService service,StyleChoiceService.View view,SimpleInventory display){
        super(ScreenHandlerType.GENERIC_9X6,syncId,inventory,display,6);
        this.owner=owner;this.service=service;this.view=view;this.display=display;render();
    }
    public static void open(ServerPlayerEntity player,StyleChoiceService service,Surface surface,UUID expectedClan){
        var previous=player.currentScreenHandler;
        try {
            service.view(player.getUuid(),surface,expectedClan).whenCompleteAsync((view,error)->{
                if(player.isDisconnected()||player.currentScreenHandler!=previous)return;
                if(error!=null){failure(player,error);return;}
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory((sync,inventory,ignored)->
                    new StyleChoiceMenu(sync,inventory,player,service,view),Text.literal("✦ ESTILOS: "+surface)));
            },player.getServer());
        } catch(Exception error){failure(player,error);}
    }
    private void item(int slot,Item type,Text name,String...lore){
        var stack=new ItemStack(type);stack.set(DataComponentTypes.CUSTOM_NAME,name);
        stack.set(DataComponentTypes.LORE,new LoreComponent(Arrays.stream(lore).map(Text::literal).map(t->(Text)t).toList()));display.setStack(slot,stack);
    }
    private void render(){
        display.clear();
        var saved=view.choices().selections().get(view.surface());
        var status=view.catalog().resolve(saved,view.surface(),node->service.allowed(owner.getUuid(),view.surface(),saved));
        item(4,Items.BOOK,Text.literal("Escolha salva: "+Objects.toString(saved,"padrão administrativo")),
            "Estado: "+status.status(),"Escopo: "+view.choices().owner(),"Revisão: "+view.choices().revision(),
            "Permissão indisponível: usa fallback sem apagar escolha.");
        entries=view.catalog().presets().entrySet().stream().filter(e->e.getValue().definition().surfaces().contains(view.surface())).map(Map.Entry::getKey).sorted().toList();
        page=Math.min(page,Math.max(0,(entries.size()-1)/36));
        for(int i=0;i<36&&page*36+i<entries.size();i++){
            String id=entries.get(page*36+i);var preset=view.catalog().presets().get(id);
            var preview=view.catalog().styles().render(preset.template(),key->id,0,false);
            item(9+i,service.allowed(owner.getUuid(),view.surface(),id)?Items.NAME_TAG:Items.BARRIER,preview,
                "ID: "+id,"Permitido: "+service.allowed(owner.getUuid(),view.surface(),id),"Salvo: "+id.equals(saved),"Clique para escolher");
        }
        item(45,Items.ARROW,Text.literal("Página anterior"));item(53,Items.ARROW,Text.literal("Próxima página"));
        item(48,Items.MILK_BUCKET,Text.literal("Usar padrão administrativo"));
        item(50,Items.ARROW,Text.literal("Fechar"));
        sendContentUpdates();
    }
    @Override public boolean canUse(PlayerEntity player){return !closed&&player==owner&&!owner.isDisconnected();}
    @Override public ItemStack quickMove(PlayerEntity player,int slot){return ItemStack.EMPTY;}
    @Override public void onSlotClick(int slot,int button,SlotActionType type,PlayerEntity player){
        if(!canUse(player)||owner.currentScreenHandler!=this||busy||type!=SlotActionType.PICKUP||button!=0)return;
        if(slot==50){owner.closeHandledScreen();return;}
        if(slot==45){page=Math.max(0,page-1);render();return;}
        if(slot==53){if((page+1)*36<entries.size())page++;render();return;}
        String id;
        if(slot==48)id=null;
        else if(slot>=9&&slot<45&&page*36+slot-9<entries.size())id=entries.get(page*36+slot-9);
        else return;
        try{
            var operation=service.choose(owner.getUuid(),view,id,()->canUse(owner)&&owner.currentScreenHandler==this);
            busy=true;
            operation.whenCompleteAsync((choices,error)->{
                busy=false;if(!canUse(owner)||owner.currentScreenHandler!=this)return;
                if(error!=null){failure(owner,error);return;}
                view=new StyleChoiceService.View(choices,view.surface(),view.catalog());render();
                owner.sendMessage(Text.literal("Estilo salvo."),false);
            },owner.getServer());
        }catch(Exception error){failure(owner,error);}
    }
    private static void failure(ServerPlayerEntity player,Throwable error){while(error.getCause()!=null)error=error.getCause();player.sendMessage(Text.literal("Estilos: "+error.getMessage()),false);}
    @Override public void onClosed(PlayerEntity player){closed=true;super.onClosed(player);}
}
