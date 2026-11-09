package com.shirokai.core.clans;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.shirokai.core.messages.Messages;
import java.util.*;

/** Vanilla rename text input. Click the left paper to save; never extracts items or spends XP. */
public final class ClanIdentityEditor extends AnvilScreenHandler {
    private final ServerPlayerEntity owner;
    private final ClanService clans;
    private final ClanSnapshot expected;
    private final boolean tag;
    private String draft;
    private boolean busy,closed;
    private ClanIdentityEditor(int sync,PlayerInventory inventory,ServerPlayerEntity owner,ClanService clans,ClanSnapshot expected,boolean tag){
        super(sync,inventory,ScreenHandlerContext.EMPTY);
        this.owner=owner;this.clans=clans;this.expected=expected;this.tag=tag;
        draft=Objects.toString(tag?expected.tag():expected.name(),"");
        var paper=new ItemStack(Items.PAPER);paper.set(DataComponentTypes.CUSTOM_NAME,Text.literal(draft.isEmpty()?"Sigla":draft));
        paper.set(DataComponentTypes.LORE,new LoreComponent(List.of(Text.literal("Digite acima e clique neste papel para salvar."),Text.literal("Esc cancela. Não custa experiência."))));
        input.setStack(0,paper);output.clear();sendContentUpdates();
    }
    public static void open(ServerPlayerEntity player,ClanService clans,ClanSnapshot expected,boolean tag){
        if(!clans.menuEnabled()){player.sendMessage(Text.literal("O menu de clã está temporariamente desativado."),false);return;}
        try {
            ClanPolicy.require(expected,player.getUuid(),tag?"tag":"rename",clans.config());
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((sync,inventory,ignored)->
                new ClanIdentityEditor(sync,inventory,player,clans,expected,tag),Text.literal("Digite; clique no papel para salvar")));
        }catch(Exception error){player.sendMessage(Messages.error("Não foi possível editar o clã: "+error.getMessage()),false);}
    }
    @Override public boolean setNewItemName(String value){
        if(owner==null||closed||busy||value==null||value.length()>64||value.codePoints().anyMatch(Character::isISOControl))return false;
        draft=value;return true;
    }
    @Override public void updateResult(){if(output!=null)output.clear();}
    @Override protected boolean canTakeOutput(PlayerEntity player,boolean present){return false;}
    @Override public boolean canUse(PlayerEntity player){return clans.menuEnabled()&&!closed&&player==owner&&!owner.isDisconnected();}
    @Override public ItemStack quickMove(PlayerEntity player,int slot){return ItemStack.EMPTY;}
    @Override public void onSlotClick(int slot,int button,SlotActionType type,PlayerEntity player){
        if(!canUse(player)||owner.currentScreenHandler!=this||busy||slot!=0||button!=0||type!=SlotActionType.PICKUP)return;
        try{
            var operation=clans.identityFromMenu(owner.getUuid(),draft,tag,expected);busy=true;
            operation.whenCompleteAsync((clan,error)->{
                busy=false;if(!canUse(owner)||owner.currentScreenHandler!=this)return;
                if(error!=null){while(error.getCause()!=null)error=error.getCause();owner.sendMessage(Messages.error("Não foi possível editar o clã: "+error.getMessage()),false);return;}
                owner.sendMessage(Messages.info(tag?"Sigla do clã atualizada.":"Nome do clã atualizado."),false);ClanMenu.open(owner,clans);
            },owner.getServer());
        }catch(Exception error){owner.sendMessage(Messages.error("Não foi possível editar o clã: "+error.getMessage()),false);}
    }
    @Override public void onClosed(PlayerEntity player){
        closed=true;
        // ForgingScreenHandler returns input items on close; remove our display-only paper first.
        input.clear();output.clear();super.onClosed(player);
    }
}
