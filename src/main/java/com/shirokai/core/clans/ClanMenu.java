package com.shirokai.core.clans;

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
import com.shirokai.core.messages.Messages;
import net.minecraft.util.Formatting;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Native read-only menus; mutation uses fresh server/SQL authorization and expected clan ID. */
public final class ClanMenu extends GenericContainerScreenHandler {
    public enum Page { HOME, MEMBERS, INVITES, INVITE_PLAYER, MEMBER, DISBAND, TRANSFER, SENT_INVITES, IDENTITY }
    private record Data(Optional<ClanSnapshot> clan,Map<String,Long> invitations,Optional<ClanRepository.TransferOffer> transfer,int limit,Map<UUID,Long> sent){}
    private final ServerPlayerEntity owner;
    private final ClanService clans;
    private final SimpleInventory display;
    private final Map<Integer,UUID> targets=new HashMap<>();
    private final Map<Integer,String> invitations=new HashMap<>();
    private Data data;
    private Page page=Page.HOME;
    private UUID target;
    private int index;
    private boolean busy,closed;
    private ClanMenu(int syncId,PlayerInventory inventory,ServerPlayerEntity owner,ClanService clans,Data data){this(syncId,inventory,owner,clans,data,new SimpleInventory(54));}
    private ClanMenu(int syncId,PlayerInventory inventory,ServerPlayerEntity owner,ClanService clans,Data data,SimpleInventory display){
        super(ScreenHandlerType.GENERIC_9X6,syncId,inventory,display,6);this.owner=owner;this.clans=clans;this.data=data;this.display=display;render();
    }
    private static CompletableFuture<Data> load(UUID actor,ClanService service,java.util.concurrent.Executor server){
        return service.membership(actor).thenComposeAsync(clan->service.invitations(actor).thenComposeAsync(invites->service.transferOffer(actor).thenComposeAsync(transfer->
            (clan.isPresent()?service.currentLimit(clan.get()):CompletableFuture.completedFuture(0)).thenApply(limit->new Data(clan,invites,transfer,limit,Map.of())),server),server),server)
            .thenComposeAsync(data->data.clan().isEmpty()?CompletableFuture.completedFuture(data):service.scoped(data.clan().get().id()).sentInvitations(actor)
                .thenApply(sent->new Data(data.clan(),data.invitations(),data.transfer(),data.limit(),sent)),server);
    }
    public static void open(ServerPlayerEntity player,ClanService service){
        var previous=player.currentScreenHandler;
        load(player.getUuid(),service,player.getServer()).whenCompleteAsync((data,error)->{
            if(player.isDisconnected()||player.currentScreenHandler!=previous)return;
            if(error!=null){player.sendMessage(Text.literal("Não foi possível carregar o clã; tente novamente."),false);return;}
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId,inventory,ignored)->new ClanMenu(syncId,inventory,player,service,data),service.present(player.getUuid(),Surface.MENU_TITLE,"✦ CLÃ SHIROKAI")));
        },player.getServer());
    }
    private void item(int slot,Item type,String name,String...lore){
        var stack=new ItemStack(type);stack.set(DataComponentTypes.CUSTOM_NAME,clans.present(owner.getUuid(),Surface.MENU_NAME,name).copy().formatted(Formatting.AQUA));
        stack.set(DataComponentTypes.LORE,new LoreComponent(Arrays.stream(lore).map(line->(Text)clans.present(owner.getUuid(),Surface.MENU_LORE,line).copy().formatted(Formatting.GRAY)).toList()));display.setStack(slot,stack);
    }
    private String name(UUID player){var online=owner.getServer().getPlayerManager().getPlayer(player);return online==null?"Offline • "+player.toString().substring(0,8):online.getGameProfile().getName();}
    private ClanSnapshot own(){return data.clan().orElseThrow(()->new IllegalStateException("Você não pertence a um clã"));}
    private ClanService scoped(){return clans.scoped(own().id());}
    public Page page(){return page;}
    private void render(){
        if(data.clan().isEmpty() && page!=Page.HOME && page!=Page.INVITES)page=Page.HOME;
        display.clear();targets.clear();invitations.clear();
        item(49,Items.CLOCK,"Atualizar");item(50,Items.ARROW,"Voltar");
        item(45,Items.ARROW,"Página anterior");item(53,Items.ARROW,"Próxima página");
        switch(page){
            case HOME->{
                item(4,Items.BOOK,data.clan().map(ClanSnapshot::name).orElse("Sem clã"),data.clan().map(clan->"Sigla: "+Objects.toString(clan.tag(),"Não definida")).orElse("Criação gratuita: /clan create <nome>"),
                    data.clan().map(clan->"Membros: "+clan.members().size()+"/"+data.limit()).orElse("Você pode aceitar um convite abaixo."),
                    data.clan().map(clan->"Seu cargo: "+Messages.role(clan.members().get(owner.getUuid()))).orElse(""));
                if(clans.menuPresentationEnabled())data.clan().ifPresent(clan->{
                    var card=display.getStack(4);card.set(DataComponentTypes.CUSTOM_NAME,clans.presentClan(owner.getUuid(),clan,Surface.CLAN_NAME,clan.name()));
                    var lore=new ArrayList<Text>(card.get(DataComponentTypes.LORE).lines());
                    lore.set(0,Text.literal("Sigla: ").formatted(Formatting.GRAY).append(clans.presentClan(owner.getUuid(),clan,Surface.CLAN_TAG,Objects.toString(clan.tag(),"Não definida"))));
                    card.set(DataComponentTypes.LORE,new LoreComponent(lore));
                });
                item(22,Items.PAPER,"Convites recebidos",Integer.toString(data.invitations().size()));
                if(data.clan().isPresent()){
                    item(20,Items.PLAYER_HEAD,"Membros e cargos");item(24,Items.WRITABLE_BOOK,"Convidar jogador");
                    item(30,Items.NAME_TAG,"Editar nome e sigla","Escolha o nome ou a sigla do seu clã.","Alterações gratuitas, com intervalo entre edições.");
                    item(31,Items.PAPER,"Convites enviados",Integer.toString(data.sent().size()),"Clique em um convite para cancelar.");
                    if(clans.customizationEnabled()&&own().leader().equals(owner.getUuid())){item(28,Items.PAINTING,"Estilo do nome");item(29,Items.PAINTING,"Estilo da sigla");}
                    item(32,Items.OAK_DOOR,"Sair do clã");
                    if(own().leader().equals(owner.getUuid()))item(40,Items.TNT,"Excluir clã","Exige confirmação em outra tela.");
                }
                if(data.transfer().isPresent()){item(46,Items.EMERALD,"Aceitar liderança","Confirme a solicitação recebida.");item(47,Items.BARRIER,"Recusar liderança");}
            }
            case IDENTITY->{item(20,Items.NAME_TAG,"Editar nome completo");item(24,Items.PAPER,"Editar sigla");}
            case SENT_INVITES->{
                var entries=data.sent().keySet().stream().sorted().toList();index=Math.min(index,Math.max(0,(entries.size()-1)/36));
                for(int i=0;i<36&&index*36+i<entries.size();i++){UUID player=entries.get(index*36+i);targets.put(9+i,player);item(9+i,Items.PAPER,name(player),"Expira em: "+java.time.Instant.ofEpochMilli(data.sent().get(player)),"Clique para cancelar o convite.");}
            }
            case MEMBERS,INVITE_PLAYER->{
                var entries=page==Page.MEMBERS?own().members().keySet().stream().sorted().toList():owner.getServer().getPlayerManager().getPlayerList().stream()
                    .map(ServerPlayerEntity::getUuid).filter(id->!own().members().containsKey(id)).sorted().toList();
                index=Math.min(index,Math.max(0,(entries.size()-1)/36));
                for(int i=0;i<36&&index*36+i<entries.size();i++){UUID player=entries.get(index*36+i);targets.put(9+i,player);
                    item(9+i,Items.PLAYER_HEAD,name(player),page==Page.MEMBERS?"Cargo: "+Messages.role(own().members().get(player)):"Clique para convidar");}
            }
            case INVITES->{
                var entries=data.invitations().keySet().stream().sorted().toList();index=Math.min(index,Math.max(0,(entries.size()-1)/36));
                for(int i=0;i<36&&index*36+i<entries.size();i++){String clan=entries.get(index*36+i);invitations.put(9+i,clan);item(9+i,Items.PAPER,clan,"Esquerdo: aceitar","Direito: recusar");}
            }
            case MEMBER->{
                item(4,Items.PLAYER_HEAD,name(target));item(20,Items.GOLD_INGOT,"Promover");item(22,Items.IRON_INGOT,"Rebaixar");item(24,Items.BARRIER,"Expulsar");
                if(own().leader().equals(owner.getUuid()))item(40,Items.NETHER_STAR,"Solicitar transferência de liderança","Exige aceitação do destinatário.");
            }
            case DISBAND->{item(4,Items.TNT,"EXCLUIR "+own().name(),"Todos os membros serão desvinculados.","Somente confirme se deseja excluir este clã.");item(22,Items.REDSTONE_BLOCK,"CONFIRMAR EXCLUSÃO");item(24,Items.EMERALD_BLOCK,"Cancelar");}
            case TRANSFER->{item(4,Items.NETHER_STAR,"TRANSFERIR LIDERANÇA",name(target),"O destinatário ainda precisará aceitar.");item(22,Items.EMERALD_BLOCK,"Confirmar solicitação");item(24,Items.BARRIER,"Cancelar");}
        }
        sendContentUpdates();
    }
    private void navigate(Page page){this.page=page;index=0;render();}
    private void action(Supplier<CompletableFuture<ClanSnapshot>> operation,Page destination){
        busy=true;
        var expected=data.clan().map(ClanSnapshot::id);
        try{
            operation.get().thenComposeAsync(result->load(owner.getUuid(),clans,owner.getServer()),owner.getServer()).whenCompleteAsync((value,error)->{
                busy=false;if(closed||owner.currentScreenHandler!=this)return;
                if(error!=null){failure(error);return;}
                data=value;page=expected.equals(value.clan().map(ClanSnapshot::id))?destination:Page.HOME;index=0;render();owner.sendMessage(Messages.info("Clã atualizado."),false);
            },owner.getServer());
        }catch(Exception error){busy=false;failure(error);}
    }
    private void refresh(){
        busy=true;load(owner.getUuid(),clans,owner.getServer()).whenCompleteAsync((value,error)->{
            busy=false;if(closed||owner.currentScreenHandler!=this)return;if(error!=null){failure(error);return;}
            data=value;page=Page.HOME;index=0;render();
        },owner.getServer());
    }
    @Override public boolean canUse(PlayerEntity player){return !closed&&player==owner&&!owner.isDisconnected();}
    @Override public ItemStack quickMove(PlayerEntity player,int slot){return ItemStack.EMPTY;}
    @Override public void onSlotClick(int slot,int button,SlotActionType type,PlayerEntity player){
        if(!canUse(player)||owner.currentScreenHandler!=this||busy||type!=SlotActionType.PICKUP||(button!=0&&button!=1))return;
        try{
            if(slot==49){refresh();return;}
            if(slot==50){if(page==Page.HOME)owner.closeHandledScreen();else navigate(Page.HOME);return;}
            if(slot==45){index=Math.max(0,index-1);render();return;}if(slot==53){index++;render();return;}
            switch(page){
                case HOME->{
                    if((slot==28||slot==29)&&data.clan().isPresent()&&clans.customizationEnabled())clans.openCustomization(owner,slot==28?Surface.CLAN_NAME:Surface.CLAN_TAG,own().id());
                    else if(slot==22)navigate(Page.INVITES);
                    else if(slot==30&&data.clan().isPresent())navigate(Page.IDENTITY);
                    else if(slot==31&&data.clan().isPresent())navigate(Page.SENT_INVITES);
                    else if(slot==20&&data.clan().isPresent())navigate(Page.MEMBERS);
                    else if(slot==24&&data.clan().isPresent())navigate(Page.INVITE_PLAYER);
                    else if(slot==32&&data.clan().isPresent())action(()->scoped().leave(owner.getUuid()),Page.HOME);
                    else if(slot==40&&data.clan().isPresent())action(()->scoped().disband(owner.getUuid(),false),Page.DISBAND);
                    else if(slot==46&&data.transfer().isPresent())action(()->clans.scoped(data.transfer().get().clan()).answerTransfer(owner.getUuid(),true),Page.HOME);
                    else if(slot==47&&data.transfer().isPresent())action(()->clans.scoped(data.transfer().get().clan()).answerTransfer(owner.getUuid(),false),Page.HOME);
                }
                case IDENTITY->{if(slot==20||slot==24)ClanIdentityEditor.open(owner,clans,own(),slot==24);}
                case SENT_INVITES->{if(targets.containsKey(slot)){UUID recipient=targets.get(slot);action(()->scoped().cancel(owner.getUuid(),recipient),Page.SENT_INVITES);}}
                case MEMBERS->{if(targets.containsKey(slot)){target=targets.get(slot);navigate(Page.MEMBER);}}
                case INVITE_PLAYER->{if(targets.containsKey(slot)){UUID recipient=targets.get(slot);action(()->scoped().invite(owner.getUuid(),recipient).thenApplyAsync(clan->{
                    var online=owner.getServer().getPlayerManager().getPlayer(recipient);if(online!=null)online.sendMessage(Messages.info("Convite do clã "+clan.name()+". Use /clan invites."),false);return clan;
                },owner.getServer()),Page.INVITE_PLAYER);}}
                case INVITES->{if(invitations.containsKey(slot)){String clan=invitations.get(slot);action(()->button==0?clans.accept(owner.getUuid(),clan):clans.decline(owner.getUuid(),clan),Page.HOME);}}
                case MEMBER->{if(slot==20||slot==22||slot==24){String operation=slot==20?"promote":slot==22?"demote":"kick";action(()->scoped().memberAction(owner.getUuid(),target,operation),Page.HOME);}else if(slot==40)navigate(Page.TRANSFER);}
                case DISBAND->{if(slot==22)action(()->scoped().disband(owner.getUuid(),true),Page.HOME);else if(slot==24)action(()->scoped().cancelDisband(owner.getUuid()),Page.HOME);}
                case TRANSFER->{if(slot==22)action(()->scoped().transfer(owner.getUuid(),target).thenApplyAsync(clan->{
                    var online=owner.getServer().getPlayerManager().getPlayer(target);if(online!=null)online.sendMessage(Messages.info("Liderança solicitada. Aceite/recuse em /clan menu."),false);return clan;
                },owner.getServer()),Page.HOME);else if(slot==24)navigate(Page.MEMBER);}
            }
        }catch(Exception error){failure(error);}
    }
    private void failure(Throwable error){while(error.getCause()!=null)error=error.getCause();owner.sendMessage(Messages.error("Clã: "+error.getMessage()),false);}
    @Override public void onClosed(PlayerEntity player){closed=true;super.onClosed(player);}
}
