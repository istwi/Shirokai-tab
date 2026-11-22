package com.shirokai.core.cosmetics;


import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.io.IOException;
import java.util.*;

/** Display-only inventory: packet actions cannot extract previews or move real items. */
public final class CosmeticScreen extends GenericContainerScreenHandler {
    private final ServerPlayerEntity owner;
    private final CosmeticService service;
    private final SimpleInventory display;
    private final Map<Integer,Runnable> actions=new HashMap<>();
    private String page;
    private CosmeticState.Preferences draft;
    private final long shownRevision;
    private static final int[] POSITIONS={10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
    public CosmeticScreen(int syncId,PlayerInventory inventory,ServerPlayerEntity owner,CosmeticService service,String page) {
        this(syncId,inventory,owner,service,page,new SimpleInventory(54));
    }
    private CosmeticScreen(int id,PlayerInventory inventory,ServerPlayerEntity owner,CosmeticService service,String page,SimpleInventory display) {
        super(ScreenHandlerType.GENERIC_9X6,id,inventory,display,6);this.owner=owner;this.service=service;this.page=page;this.display=display;shownRevision=service.revision(owner);
        try {var saved=service.state(owner).cosmeticPreferences;draft=new CosmeticState.Preferences();draft.speed=saved.speed;draft.size=saved.size;draft.density=saved.density;draft.direction=saved.direction;} catch(IOException e){draft=new CosmeticState.Preferences();service.error(owner,e);}render();
    }
    public static String title(String page) {
        return page.equals("main")?"Escolha uma categoria":page.equals("customize")?"Personalização":CosmeticCatalog.CATEGORIES.getOrDefault(page,"Cosméticos");
    }
    private static Item icon(String category) {
        return switch(category) {case "nick"->Items.NAME_TAG;case "trail"->Items.LEATHER_BOOTS;case "aura"->Items.NETHER_STAR;case "join"->Items.FIREWORK_ROCKET;case "victory"->Items.TOTEM_OF_UNDYING;case "raid"->Items.DRAGON_BREATH;default->Items.AMETHYST_SHARD;};
    }
    private void button(int slot,Item item,String name,List<String> lore,Formatting color,Runnable action) {
        var stack=new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME,Text.literal(name).styled(s->s.withColor(color).withItalic(false)));
        stack.set(DataComponentTypes.LORE,new LoreComponent(lore.stream().map(line->(Text)Text.literal(line).styled(s->s.withColor(Formatting.GRAY).withItalic(false))).toList()));
        display.setStack(slot,stack);if(action!=null)actions.put(slot,action);
    }
    public void refresh() { render(); }
    private void render() {
        actions.clear();
        for(int i=0;i<54;i++)button(i,Items.GRAY_STAINED_GLASS_PANE," ",List.of(),Formatting.WHITE,null);
        try {
            var state=service.state(owner);
            if(page.equals("main")) {
                int i=0;
                for(String category:CosmeticCatalog.ORDER) {
                    var e=CosmeticCatalog.find(state.cosmetics.get(category));
                    button(POSITIONS[i++],icon(category),title(category),List.of("Selecionado: "+(e==null?"Nenhum":e.label()),description(category),"Clique para ver as opções."),Formatting.AQUA,()->service.open(owner,category));
                }
                button(31,service.canCustomize(owner)?Items.COMPARATOR:Items.IRON_BARS,"Personalizar partículas",List.of(service.canCustomize(owner)?"Ajuste velocidade, tamanho, densidade e direção.":"Você não tem acesso à personalização.","Os limites são definidos pelo servidor."),service.canCustomize(owner)?Formatting.AQUA:Formatting.DARK_GRAY,service.canCustomize(owner)?()->service.open(owner,"customize"):null);
                button(49,state.cosmeticsHidden?Items.LIME_DYE:Items.BARRIER,state.cosmeticsHidden?"Reativar cosméticos":"Desativar tudo",List.of("Sua seleção será mantida.","Desativado: oculta seus efeitos e", "as partículas de outros jogadores para você."),state.cosmeticsHidden?Formatting.GREEN:Formatting.RED,()->{if(state.cosmeticsHidden)service.resume(owner);else service.disable(owner);render();});
            } else if(page.equals("customize")) {
                customization();
            } else {
                var entries=CosmeticCatalog.ENTRIES.stream().filter(e->e.category().equals(page)).toList();
                int i=0;
                for(var entry:entries) {
                    boolean unlocked=service.unlocked(owner,entry), equipped=entry.id().equals(state.cosmetics.get(entry.category()));
                    var lore=new ArrayList<String>();
                    lore.add(equipped?(!unlocked?"Selecionado · indisponível":state.cosmeticsHidden?"Selecionado · efeitos desativados":"Equipado"):unlocked?"Disponível":"Bloqueado");
                    if(!unlocked)lore.add(service.config().disabled.contains(entry.id())?"Desativado pelo servidor.":"Sem acesso a esta opção.");
                    lore.add(description(entry.category()));
                    lore.add("Permissão: shirokai.core.cosmetics."+entry.id());
                    lore.add("Clique para equipar. Não concede vantagens.");
                    if(entry.category().equals("nick"))lore.add("A cor afeta só seu nome; cargos são preservados.");
                    button(POSITIONS[i++],unlocked?icon(entry.category()):Items.IRON_BARS,entry.label(),lore,equipped?Formatting.GREEN:unlocked?Formatting.AQUA:Formatting.DARK_GRAY,()->{service.equip(owner,entry);render();});
                }
                button(49,Items.BARRIER,"Remover desta categoria",List.of("As outras categorias serão mantidas."),Formatting.RED,()->{service.remove(owner,page);render();});
            }
            if(!page.equals("main")&&!page.equals("customize"))button(45,Items.ARROW,"Voltar",List.of("Retornar às categorias."),Formatting.AQUA,()->service.open(owner,"main"));
            button(53,Items.OAK_DOOR,"Fechar",List.of(),Formatting.GRAY,owner::closeHandledScreen);
        } catch(IOException e) {service.error(owner,e);}
        sendContentUpdates();
    }
    private void customization() {
        button(10,Items.SUGAR,"Velocidade: "+level(draft.speed),List.of("Controla a animação da aura.","Clique para alternar."),Formatting.AQUA,()->{draft.speed=cycle(draft.speed,service.config().maxSpeed);preview();});
        button(12,Items.SLIME_BALL,"Tamanho: "+level(draft.size),List.of("Controla o raio da aura e dos eventos.","Clique para alternar."),Formatting.AQUA,()->{draft.size=cycle(draft.size,service.config().maxSize);preview();});
        button(14,Items.GLOWSTONE_DUST,"Densidade: "+level(draft.density),List.of("Controla quantas partículas são emitidas.","Nunca ultrapassa o limite do servidor."),Formatting.AQUA,()->{draft.density=cycle(draft.density,service.config().maxDensity);preview();});
        button(16,Items.COMPASS,"Direção: "+(draft.direction>0?"Horária":"Anti-horária"),List.of("Inverte o giro da aura."),Formatting.AQUA,()->{draft.direction=-draft.direction;preview();});
        button(29,Items.SPYGLASS,"Prévia",List.of("Exibe uma espiral temporária.","Nada é salvo."),Formatting.LIGHT_PURPLE,()->service.preview(owner,draft));
        button(31,Items.CLOCK,"Restaurar padrão",List.of("Velocidade, tamanho e densidade normais."),Formatting.YELLOW,()->{draft=new CosmeticState.Preferences();preview();});
        button(33,Items.LIME_DYE,"Aplicar",List.of("Salva estas escolhas para o seu UUID."),Formatting.GREEN,()->{service.preferences(owner,draft,shownRevision);service.open(owner,"main");});
        button(45,Items.BARRIER,"Cancelar",List.of("Descarta alterações não aplicadas."),Formatting.RED,()->service.open(owner,"main"));
    }
    private void preview(){service.preview(owner,draft);render();}
    private static int cycle(int value,int maximum){return value>=maximum?1:value+1;}
    private static String level(int value){return switch(value){case 1->"Baixa";case 3->"Alta";default->"Normal";};}
    private static String description(String category) {
        return switch(category) {case "nick"->"Altera a cor do nome; não muda seu nick.";case "trail"->"Partículas junto ao chão enquanto caminha.";case "aura"->"Partículas orbitando ao seu redor.";case "join"->"Efeito breve ao entrar no servidor.";case "victory"->"Celebração ao vencer PvP ou via integração.";case "raid"->"Celebração ativada pela integração de raid.";default->"Efeito visual.";};
    }
    @Override public boolean canUse(PlayerEntity player) {return player==owner&&owner.isAlive()&&service.canUse(owner);}
    @Override public void onSlotClick(int slot,int button,SlotActionType action,PlayerEntity player) {
        if(canUse(player)&&owner.currentScreenHandler==this&&action==SlotActionType.PICKUP&&(button==0||button==1)) {
            var actionHandler=actions.get(slot);if(actionHandler!=null)actionHandler.run();
        }
        if(owner.currentScreenHandler==this)syncState();
    }
    @Override public ItemStack quickMove(PlayerEntity player,int slot) {return ItemStack.EMPTY;}
}
