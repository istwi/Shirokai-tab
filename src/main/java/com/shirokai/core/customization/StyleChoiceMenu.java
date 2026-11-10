package com.shirokai.core.customization;

import com.shirokai.core.presentation.Surface;
import com.shirokai.core.menus.PanelLayout;
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
        this(syncId,inventory,owner,service,view,new SimpleInventory(45));
    }
    private StyleChoiceMenu(int syncId,PlayerInventory inventory,ServerPlayerEntity owner,StyleChoiceService service,StyleChoiceService.View view,SimpleInventory display){
        super(ScreenHandlerType.GENERIC_9X5,syncId,inventory,display,5);
        this.owner=owner;this.service=service;this.view=view;this.display=display;render();
    }
    public static void open(ServerPlayerEntity player,StyleChoiceService service,Surface surface,UUID expectedClan){
        var previous=player.currentScreenHandler;
        try {
            service.view(player.getUuid(),surface,expectedClan).whenCompleteAsync((view,error)->{
                if(player.isDisconnected()||player.currentScreenHandler!=previous)return;
                if(error!=null){failure(player,error);return;}
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory((sync,inventory,ignored)->
                    new StyleChoiceMenu(sync,inventory,player,service,view),Text.literal("✦ ESTILO DE CORES").formatted(net.minecraft.util.Formatting.GOLD)));
            },player.getServer());
        } catch(Exception error){failure(player,error);}
    }
    private void item(int slot,Item type,Text name,String...lore){
        var stack=new ItemStack(type);stack.set(DataComponentTypes.CUSTOM_NAME,name);
        stack.set(DataComponentTypes.LORE,new LoreComponent(Arrays.stream(lore).map(Text::literal).map(t->(Text)t).toList()));display.setStack(slot,stack);
    }
    public void refreshVisuals(){if(!closed&&!busy&&owner.currentScreenHandler==this)render();}
    private void render(){
        display.clear();PanelLayout.frame(display);
        var saved=view.choices().selections().get(view.surface());
        var status=view.catalog().resolve(saved,view.surface(),node->service.allowed(owner.getUuid(),view.surface(),saved));
        item(10,Items.PLAYER_HEAD,Text.literal(owner.getGameProfile().getName()),
            "Escolha salva: "+Objects.toString(saved,"padrão administrativo"),
            "Estado: "+switch(status.status()){case ADMINISTRATIVE->"Padrão do servidor";case ACTIVE->"Aplicado";case UNAVAILABLE->"Sem acesso no momento";case REMOVED->"Estilo indisponível";},
            "Sua escolha fica salva mesmo sem acesso ao estilo.",
            "Enquanto indisponível, usamos o estilo padrão.");
        com.shirokai.core.menus.PlayerHeads.apply(display.getStack(10),owner);
        entries=view.catalog().presets().entrySet().stream().filter(e->e.getValue().definition().surfaces().contains(view.surface())).map(Map.Entry::getKey).sorted().toList();
        page=Math.min(page,Math.max(0,(entries.size()-1)/PanelLayout.CONTENT.size()));
        for(int i=0;i<PanelLayout.CONTENT.size()&&page*PanelLayout.CONTENT.size()+i<entries.size();i++){
            String id=entries.get(page*PanelLayout.CONTENT.size()+i);var preset=view.catalog().presets().get(id);
            var preview=view.catalog().render(new PresetCatalog.Resolution(PresetCatalog.Status.ACTIVE,id,preset.template()),id,owner.getServer().getTicks(),service.animates(owner.getUuid(),view.surface(),VisualTarget.MENU)&&service.allowed(owner.getUuid(),view.surface(),id));
            item(PanelLayout.CONTENT.get(i),service.allowed(owner.getUuid(),view.surface(),id)?Items.NAME_TAG:Items.BARRIER,preview,
                "Acesso: "+(service.allowed(owner.getUuid(),view.surface(),id)?"Disponível":"Bloqueado"),"Seleção: "+(id.equals(saved)?"Escolhido":"Não selecionado"),"Clique para escolher");
        }
        if(page>0)item(36,Items.ARROW,Text.literal("Página anterior"));
        if((page+1)*PanelLayout.CONTENT.size()<entries.size())item(44,Items.ARROW,Text.literal("Próxima página"));
        item(19,Items.PAINTING,Text.literal("Estilo de cores"),"Clique para usar o padrão do servidor.","Página "+(page+1));
        item(28,Items.ARROW,Text.literal("Voltar ao menu"));
        sendContentUpdates();
    }
    @Override public boolean canUse(PlayerEntity player){return !closed&&player==owner&&!owner.isDisconnected();}
    @Override public ItemStack quickMove(PlayerEntity player,int slot){return ItemStack.EMPTY;}
    @Override public void onSlotClick(int slot,int button,SlotActionType type,PlayerEntity player){
        if(!canUse(player)||owner.currentScreenHandler!=this||busy||type!=SlotActionType.PICKUP||button!=0)return;
        if(slot<0||slot>=45||display.getStack(slot).isEmpty()||display.getStack(slot).isOf(Items.GRAY_STAINED_GLASS_PANE))return;
        if(slot==28){owner.closeHandledScreen();owner.getServer().getCommandManager().executeWithPrefix(owner.getCommandSource(),view.surface()==Surface.TITLE?"perfil":"clan");return;}
        if(slot==36){page=Math.max(0,page-1);render();return;}
        if(slot==44){if((page+1)*PanelLayout.CONTENT.size()<entries.size())page++;render();return;}
        String id;
        if(slot==19)id=null;
        else if(PanelLayout.CONTENT.contains(slot)&&page*PanelLayout.CONTENT.size()+PanelLayout.CONTENT.indexOf(slot)<entries.size())id=entries.get(page*PanelLayout.CONTENT.size()+PanelLayout.CONTENT.indexOf(slot));
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
