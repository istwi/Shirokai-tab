package com.shirokai.core.profiles;

import com.shirokai.core.titles.*;
import com.shirokai.core.menus.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Read-only native menu. Each screen holds its validated layout snapshot across reload. */
public final class ProfileMenu extends GenericContainerScreenHandler {
    private final ServerPlayerEntity owner;
    private final TitleService titles;
    private final SimpleInventory display;
    private final MenuPlan plan;
    private final MenuConfig.Profile layout;
    private TitleState state;
    private List<TitleDefinition> entries=List.of();
    private int page;
    private boolean busy,closed;
    public ProfileMenu(int syncId,PlayerInventory inventory,ServerPlayerEntity owner,TitleService titles,TitleState state){this(syncId,inventory,owner,titles,state,titles.menus());}
    private ProfileMenu(int syncId,PlayerInventory inventory,ServerPlayerEntity owner,TitleService titles,TitleState state,MenuPlan plan){this(syncId,inventory,owner,titles,state,plan,new SimpleInventory(plan.config().profile().rows()*9));}
    private ProfileMenu(int syncId,PlayerInventory inventory,ServerPlayerEntity owner,TitleService titles,TitleState state,MenuPlan plan,SimpleInventory display){
        super(type(plan.config().profile().rows()),syncId,inventory,display,plan.config().profile().rows());
        this.owner=owner;this.titles=titles;this.state=state;this.plan=plan;this.layout=plan.config().profile();this.display=display;render();
    }
    private static ScreenHandlerType<GenericContainerScreenHandler> type(int rows){return switch(rows){case 1->ScreenHandlerType.GENERIC_9X1;case 2->ScreenHandlerType.GENERIC_9X2;case 3->ScreenHandlerType.GENERIC_9X3;case 4->ScreenHandlerType.GENERIC_9X4;case 5->ScreenHandlerType.GENERIC_9X5;default->ScreenHandlerType.GENERIC_9X6;};}
    public static void open(ServerPlayerEntity player,TitleService titles){
        player.sendMessage(Text.literal("Carregando perfil…"),false);var previous=player.currentScreenHandler;
        titles.view(player.getUuid()).whenCompleteAsync((state,error)->{
            if(player.isDisconnected()||player.currentScreenHandler!=previous)return;
            if(error!=null){player.sendMessage(Text.literal("Não foi possível carregar o perfil; tente novamente."),false);return;}
            var plan=titles.menus();
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId,inventory,ignored)->new ProfileMenu(syncId,inventory,player,titles,state,plan),plan.text(plan.config().profile().title(),context(player,titles,state,0))));
        },player.getServer());
    }
    private static Map<String,Text> context(ServerPlayerEntity player,TitleService titles,TitleState state,int page){
        var values=new HashMap<String,Text>();values.put("menu:player",Text.literal(player.getGameProfile().getName()));
        values.put("menu:rank",Text.literal(titles.rank(player.getUuid()).map(TitleDefinition::name).orElse("Nenhum")));
        values.put("menu:clan",Text.literal(titles.clanName(player.getUuid()).orElse("Nenhum")));
        values.put("menu:count",Text.literal(Integer.toString(state.chatIds().size())));values.put("menu:page",Text.literal(Integer.toString(page+1)));return values;
    }
    private ItemStack icon(String item,String name,List<String> lore,Map<String,Text> values){
        var stack=new ItemStack(Registries.ITEM.get(Identifier.of(item)));
        stack.set(DataComponentTypes.CUSTOM_NAME,plan.text(name,values));
        stack.set(DataComponentTypes.LORE,new LoreComponent(lore.stream().map(line->plan.text(line,values)).toList()));return stack;
    }
    private void render(){
        display.clear();
        entries=titles.catalog(owner.getUuid()).definitions().values().stream().filter(t->t.category()!=TitleDefinition.Category.ROLE&&t.chat())
            .sorted(Comparator.comparing(TitleDefinition::category).thenComparing(Comparator.comparingInt(TitleDefinition::priority).reversed()).thenComparing(TitleDefinition::id)).toList();
        int size=layout.tagSlots().size();page=Math.min(page,Math.max(0,(entries.size()-1)/size));
        var common=context(owner,titles,state,page);var active=titles.chatDefinitions(owner.getUuid()).stream().map(TitleDefinition::id).collect(java.util.stream.Collectors.toSet());
        for(int i=0;i<size&&page*size+i<entries.size();i++){
            var title=entries.get(page*size+i);var availability=titles.rules().availability(title,state,titles.now());
            var values=new HashMap<>(common);
            values.put("menu:tag",titles.menuPresentationEnabled()?titles.display(owner.getUuid(),title):Text.literal(title.display()));
            values.put("menu:description",Text.literal(title.description()));
            values.put("menu:availability",Text.literal(switch(availability){case UNLOCKED->"Disponível";case LOCKED->"Bloqueado";case EXPIRED->"Expirado";case UNAVAILABLE->"Indisponível";}));
            values.put("menu:category",Text.literal(title.category()==TitleDefinition.Category.CLAN?"Clã":"Especial"));
            values.put("menu:selection",Text.literal(state.chatIds().contains(title.id())?(active.contains(title.id())?"Equipado":"Salvo (fora do limite atual)"):"Não equipado"));
            var tags=layout.tags();display.setStack(layout.tagSlots().get(i),icon(availability==TitleRules.Availability.UNLOCKED?tags.unlockedItem():tags.lockedItem(),tags.name(),tags.lore(),values));
        }
        for(var entry:layout.buttons().entrySet()){
            if(entry.getKey().equals("styles")&&!titles.customizationEnabled())continue;
            var b=entry.getValue();display.setStack(b.slot(),icon(b.item(),b.name(),b.lore(),common));
        }
        sendContentUpdates();
    }
    @Override public boolean canUse(PlayerEntity player){return !closed&&player==owner&&!owner.isDisconnected();}
    @Override public ItemStack quickMove(PlayerEntity player,int slot){return ItemStack.EMPTY;}
    @Override public void onSlotClick(int slot,int button,SlotActionType action,PlayerEntity player){
        if(!canUse(player)||owner.currentScreenHandler!=this||busy||action!=SlotActionType.PICKUP||(button!=0&&button!=1))return;
        String control=layout.buttons().entrySet().stream().filter(e->e.getValue().slot()==slot).map(Map.Entry::getKey).findFirst().orElse("");
        if(control.equals("previous")){page=Math.max(0,page-1);render();return;}
        if(control.equals("next")){if((page+1)*layout.tagSlots().size()<entries.size())page++;render();return;}
        if(control.equals("back")){
            owner.closeHandledScreen();var server=owner.getServer();
            if(server.getCommandManager().getDispatcher().getRoot().getChild("menu")!=null)server.getCommandManager().executeWithPrefix(owner.getCommandSource(),"menu");
            else owner.sendMessage(Text.literal("Menu principal não está disponível neste servidor."),false);return;
        }
        if(control.equals("styles")){if(titles.customizationEnabled())titles.openCustomization(owner);return;}
        CompletableFuture<TitleState> operation;
        try{
            if(control.equals("clear"))operation=titles.clear(owner.getUuid());
            else if(control.equals("refresh"))operation=titles.view(owner.getUuid());
            else{int index=layout.tagSlots().indexOf(slot);if(index<0||page*layout.tagSlots().size()+index>=entries.size())return;operation=titles.equip(owner.getUuid(),entries.get(page*layout.tagSlots().size()+index).id(),true);}
        }catch(Exception error){failure(error);return;}
        busy=true;operation.whenCompleteAsync((value,error)->{
            busy=false;if(closed||owner.currentScreenHandler!=this)return;
            if(error!=null)failure(error);else{state=value;render();owner.sendMessage(Text.literal("Perfil atualizado."),false);}
        },owner.getServer());
    }
    private void failure(Throwable error){while(error.getCause()!=null)error=error.getCause();owner.sendMessage(Text.literal("Perfil: "+error.getMessage()).formatted(Formatting.RED),false);}
    @Override public void onClosed(PlayerEntity player){closed=true;display.clear();super.onClosed(player);}
}
