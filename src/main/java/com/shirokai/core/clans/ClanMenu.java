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
    public enum Page { HOME, MEMBERS, INVITES, INVITE_PLAYER, MEMBER, DISBAND, TRANSFER, SENT_INVITES, IDENTITY, STYLES }
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
    private ClanMenu(int syncId,PlayerInventory inventory,ServerPlayerEntity owner,ClanService clans,Data data){this(syncId,inventory,owner,clans,data,new SimpleInventory(45));}
    private ClanMenu(int syncId,PlayerInventory inventory,ServerPlayerEntity owner,ClanService clans,Data data,SimpleInventory display){
        super(ScreenHandlerType.GENERIC_9X5,syncId,inventory,display,5);this.owner=owner;this.clans=clans;this.data=data;this.display=display;render();
    }
    private static CompletableFuture<Data> load(UUID actor,ClanService service,java.util.concurrent.Executor server){
        return service.membership(actor).thenComposeAsync(clan->service.invitations(actor).thenComposeAsync(invites->service.transferOffer(actor).thenComposeAsync(transfer->
            (clan.isPresent()?service.currentLimit(clan.get()):CompletableFuture.completedFuture(0)).thenApply(limit->new Data(clan,invites,transfer,limit,Map.of())),server),server),server)
            .thenComposeAsync(data->data.clan().isEmpty()?CompletableFuture.completedFuture(data):service.scoped(data.clan().get().id()).sentInvitations(actor)
                .thenApply(sent->new Data(data.clan(),data.invitations(),data.transfer(),data.limit(),sent)),server);
    }
    public static void open(ServerPlayerEntity player,ClanService service){
        if(!service.menuEnabled()){player.sendMessage(Text.literal("O menu de clã está temporariamente desativado. Use /clan info ou os demais comandos de clã."),false);return;}
        var previous=player.currentScreenHandler;
        load(player.getUuid(),service,player.getServer()).whenCompleteAsync((data,error)->{
            if(!service.menuEnabled()||player.isDisconnected()||player.currentScreenHandler!=previous)return;
            if(error!=null){player.sendMessage(Text.literal("Não foi possível carregar o clã; tente novamente."),false);return;}
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId,inventory,ignored)->new ClanMenu(syncId,inventory,player,service,data),service.present(player.getUuid(),Surface.MENU_TITLE,"✦ CLÃ SHIROKAI")));
        },player.getServer());
    }
    private void item(int slot,Item type,String name,String...lore){
        var stack=new ItemStack(type);stack.set(DataComponentTypes.CUSTOM_NAME,clans.present(owner.getUuid(),Surface.MENU_NAME,name).copy().formatted(Formatting.AQUA).styled(style->style.withItalic(false)));
        stack.set(DataComponentTypes.LORE,new LoreComponent(Arrays.stream(lore).map(line->(Text)clans.present(owner.getUuid(),Surface.MENU_LORE,line).copy().formatted(Formatting.GRAY).styled(style->style.withItalic(false))).toList()));display.setStack(slot,stack);
    }
    private void decoration(int slot,Item type){
        var stack=new ItemStack(type);stack.set(DataComponentTypes.CUSTOM_NAME,Text.literal(" "));display.setStack(slot,stack);
    }
    private boolean paged(){return page==Page.MEMBERS||page==Page.INVITES||page==Page.INVITE_PLAYER||page==Page.SENT_INVITES;}
    private void frame(){
        if(page==Page.HOME){
            for(int slot=0;slot<45;slot++)if(slot!=24&&slot!=25&&slot!=30&&slot!=31&&slot!=32&&slot!=33)decoration(slot,Items.GRAY_STAINED_GLASS_PANE);
            item(19,Items.COMPASS,"Atualizar painel","Recarregar as informações do clã.");
            item(28,Items.REDSTONE_BLOCK,"Fechar aba");
        }else{
            for(int slot=0;slot<9;slot++)decoration(slot,Items.GRAY_STAINED_GLASS_PANE);
            for(int slot=36;slot<45;slot++)decoration(slot,Items.GRAY_STAINED_GLASS_PANE);
            item(40,Items.COMPASS,"Atualizar painel");item(41,Items.ARROW,"Voltar ao painel");
            if(paged()&&index>0)item(36,Items.ARROW,"Página anterior");
        }
    }
    private String name(UUID player){var online=owner.getServer().getPlayerManager().getPlayer(player);return online==null||!com.shirokai.core.visibility.VisibilityService.canSee(owner.getServer(),owner,online)?"Offline • "+player.toString().substring(0,8):online.getGameProfile().getName();}
    private ClanSnapshot own(){return data.clan().orElseThrow(()->new IllegalStateException("Você não pertence a um clã"));}
    private ClanService scoped(){return clans.scoped(own().id());}
    public Page page(){return page;}
    public void refreshVisuals(){if(!closed&&!busy&&page==Page.HOME&&owner.currentScreenHandler==this)render();}
    private void render(){
        if(data.clan().isEmpty() && page!=Page.HOME && page!=Page.INVITES)page=Page.HOME;
        display.clear();targets.clear();invitations.clear();
        frame();
        switch(page){
            case HOME->{
                item(10,Items.PLAYER_HEAD,"Informações do clã",
                    data.clan().map(c->"Nome: "+c.name()).orElse("Você ainda não pertence a um clã."),
                    data.clan().map(c->"Sigla: "+Objects.toString(c.tag(),"Não definida")).orElse("Crie um clã: /clan create <nome>"),
                    data.clan().map(c->"Membros: "+c.members().size()+"/"+data.limit()).orElse("Ou aceite um convite recebido."),
                    data.clan().map(c->"Seu cargo: "+Messages.role(c.members().get(owner.getUuid()))).orElse(""));
                if(clans.menuPresentationEnabled())data.clan().ifPresent(clan->{
                    var card=display.getStack(10);var lore=new ArrayList<Text>(card.get(DataComponentTypes.LORE).lines());
                    lore.set(0,Text.literal("Nome: ").formatted(Formatting.GRAY).append(clans.presentClan(owner.getUuid(),clan,Surface.CLAN_NAME,clan.name())));
                    lore.set(1,Text.literal("Sigla: ").formatted(Formatting.GRAY).append(clans.presentClan(owner.getUuid(),clan,Surface.CLAN_TAG,Objects.toString(clan.tag(),"Não definida"))));
                    card.set(DataComponentTypes.LORE,new LoreComponent(lore));
                });
                item(13,Items.NETHER_STAR,"Convites recebidos",Integer.toString(data.invitations().size()),"Clique para aceitar ou recusar.");
                if(data.clan().isPresent()){
                    item(12,Items.NETHER_STAR,"Membros e cargos","Consulte e gerencie os membros do clã.");
                    item(14,Items.NETHER_STAR,"Convidar jogador","Convide novos aliados para o clã.");
                    item(15,Items.NETHER_STAR,"Editar nome e sigla","Personalize a identidade do seu clã.");
                    item(16,Items.NETHER_STAR,"Convites enviados",Integer.toString(data.sent().size()),"Clique em um convite para cancelar.");
                    item(21,Items.NETHER_STAR,"Histórico do clã","Consulte as ações registradas.");
                    if(clans.customizationEnabled()&&own().leader().equals(owner.getUuid()))item(22,Items.NETHER_STAR,"Estilos do clã","Escolha o estilo do nome ou da sigla.");
                    item(23,Items.NETHER_STAR,"Sair do clã","Encerrar sua participação neste clã.");
                    if(own().leader().equals(owner.getUuid())){
                        item(34,Items.BARRIER,"Excluir clã","Exige confirmação em outra tela.");
                        display.getStack(34).set(DataComponentTypes.CUSTOM_NAME,Text.literal("Excluir clã").formatted(Formatting.RED));
                    }
                }
                if(data.transfer().isPresent()){item(37,Items.EMERALD,"Aceitar liderança","Confirme a solicitação recebida.");item(38,Items.BARRIER,"Recusar liderança");}
            }
            case STYLES->{item(20,Items.NETHER_STAR,"Estilo do nome");item(24,Items.NETHER_STAR,"Estilo da sigla");}
            case IDENTITY->{item(20,Items.NAME_TAG,"Editar nome completo");item(24,Items.PAPER,"Editar sigla");}
            case SENT_INVITES->{
                var entries=data.sent().keySet().stream().sorted().toList();index=Math.min(index,Math.max(0,(entries.size()-1)/27));
                for(int i=0;i<27&&index*27+i<entries.size();i++){UUID player=entries.get(index*27+i);targets.put(9+i,player);item(9+i,Items.PAPER,name(player),"Expira em: "+java.time.Instant.ofEpochMilli(data.sent().get(player)),"Clique para cancelar o convite.");}
            }
            case MEMBERS,INVITE_PLAYER->{
                var entries=page==Page.MEMBERS?own().members().keySet().stream().sorted().toList():owner.getServer().getPlayerManager().getPlayerList().stream()
                    .map(ServerPlayerEntity::getUuid).filter(id->!own().members().containsKey(id)&&com.shirokai.core.visibility.VisibilityService.canSee(owner.getServer(),owner.getUuid(),id)).sorted().toList();
                index=Math.min(index,Math.max(0,(entries.size()-1)/27));
                for(int i=0;i<27&&index*27+i<entries.size();i++){UUID player=entries.get(index*27+i);targets.put(9+i,player);
                    item(9+i,Items.PLAYER_HEAD,name(player),page==Page.MEMBERS?"Cargo: "+Messages.role(own().members().get(player)):"Clique para convidar");}
            }
            case INVITES->{
                var entries=data.invitations().keySet().stream().sorted().toList();index=Math.min(index,Math.max(0,(entries.size()-1)/27));
                for(int i=0;i<27&&index*27+i<entries.size();i++){String clan=entries.get(index*27+i);invitations.put(9+i,clan);item(9+i,Items.PAPER,clan,"Esquerdo: aceitar","Direito: recusar");}
            }
            case MEMBER->{
                item(4,Items.PLAYER_HEAD,name(target));item(20,Items.GOLD_INGOT,"Promover");item(22,Items.IRON_INGOT,"Rebaixar");item(24,Items.BARRIER,"Expulsar");
                if(own().leader().equals(owner.getUuid()))item(31,Items.NETHER_STAR,"Solicitar transferência de liderança","Exige aceitação do destinatário.");
            }
            case DISBAND->{item(4,Items.TNT,"EXCLUIR "+own().name(),"Todos os membros serão desvinculados.","Somente confirme se deseja excluir este clã.");item(22,Items.REDSTONE_BLOCK,"CONFIRMAR EXCLUSÃO");item(24,Items.EMERALD_BLOCK,"Cancelar");}
            case TRANSFER->{item(4,Items.NETHER_STAR,"TRANSFERIR LIDERANÇA",name(target),"O destinatário ainda precisará aceitar.");item(22,Items.EMERALD_BLOCK,"Confirmar solicitação");item(24,Items.BARRIER,"Cancelar");}
        }
        if(paged()){
            int count=switch(page){case MEMBERS->own().members().size();case INVITES->data.invitations().size();case SENT_INVITES->data.sent().size();default->(int)owner.getServer().getPlayerManager().getPlayerList().stream().filter(p->!own().members().containsKey(p.getUuid())&&com.shirokai.core.visibility.VisibilityService.canSee(owner.getServer(),owner,p)).count();};
            if((index+1)*27<count)item(44,Items.ARROW,"Próxima página");
            if(index==0)decoration(36,Items.GRAY_STAINED_GLASS_PANE);
            item(4,Items.MAP,switch(page){case MEMBERS->"Membros do clã";case INVITES->"Convites recebidos";case INVITE_PLAYER->"Convidar jogador";default->"Convites enviados";},"Página "+(index+1),"Total de entradas: "+count);
            if(count==0)item(22,Items.GLASS_BOTTLE,"Nada por aqui ainda","Não há entradas nesta lista.");
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
    @Override public boolean canUse(PlayerEntity player){return clans.menuEnabled()&&!closed&&player==owner&&!owner.isDisconnected();}
    @Override public ItemStack quickMove(PlayerEntity player,int slot){return ItemStack.EMPTY;}
    @Override public void onSlotClick(int slot,int button,SlotActionType type,PlayerEntity player){
        if(!canUse(player)||owner.currentScreenHandler!=this||busy||type!=SlotActionType.PICKUP||(button!=0&&button!=1))return;
        if(slot<0||slot>=45||display.getStack(slot).isEmpty()||display.getStack(slot).isOf(Items.BLACK_STAINED_GLASS_PANE)||display.getStack(slot).isOf(Items.CYAN_STAINED_GLASS_PANE)||display.getStack(slot).isOf(Items.GRAY_STAINED_GLASS_PANE))return;
        try{
            if(slot==(page==Page.HOME?19:40)){refresh();return;}
            if(slot==(page==Page.HOME?28:41)){if(page==Page.HOME)owner.closeHandledScreen();else navigate(Page.HOME);return;}
            if(slot==36){index=Math.max(0,index-1);render();return;}if(slot==44){index++;render();return;}
            switch(page){
                case HOME->{
                    if(slot==13)navigate(Page.INVITES);
                    else if(slot==15&&data.clan().isPresent())navigate(Page.IDENTITY);
                    else if(slot==16&&data.clan().isPresent())navigate(Page.SENT_INVITES);
                    else if(slot==12&&data.clan().isPresent())navigate(Page.MEMBERS);
                    else if(slot==14&&data.clan().isPresent())navigate(Page.INVITE_PLAYER);
                    else if(slot==21&&data.clan().isPresent())ClanHistoryMenu.open(owner,clans,own().id(),0);
                    else if(slot==22&&data.clan().isPresent()&&clans.customizationEnabled())navigate(Page.STYLES);
                    else if(slot==23&&data.clan().isPresent())action(()->scoped().leave(owner.getUuid()),Page.HOME);
                    else if(slot==34&&data.clan().isPresent())action(()->scoped().disband(owner.getUuid(),false),Page.DISBAND);
                    else if(slot==37&&data.transfer().isPresent())action(()->clans.scoped(data.transfer().get().clan()).answerTransfer(owner.getUuid(),true),Page.HOME);
                    else if(slot==38&&data.transfer().isPresent())action(()->clans.scoped(data.transfer().get().clan()).answerTransfer(owner.getUuid(),false),Page.HOME);
                }
                case STYLES->{if((slot==20||slot==24)&&data.clan().isPresent()&&clans.customizationEnabled())clans.openCustomization(owner,slot==20?Surface.CLAN_NAME:Surface.CLAN_TAG,own().id());}
                case IDENTITY->{if(slot==20||slot==24)ClanIdentityEditor.open(owner,clans,own(),slot==24);}
                case SENT_INVITES->{if(targets.containsKey(slot)){UUID recipient=targets.get(slot);action(()->scoped().cancel(owner.getUuid(),recipient),Page.SENT_INVITES);}}
                case MEMBERS->{if(targets.containsKey(slot)){target=targets.get(slot);navigate(Page.MEMBER);}}
                case INVITE_PLAYER->{if(targets.containsKey(slot)){UUID recipient=targets.get(slot);action(()->scoped().invite(owner.getUuid(),recipient).thenApplyAsync(clan->{
                    var online=owner.getServer().getPlayerManager().getPlayer(recipient);if(online!=null)online.sendMessage(Messages.info("Convite do clã "+clan.name()+". Use /clan invites."),false);return clan;
                },owner.getServer()),Page.INVITE_PLAYER);}}
                case INVITES->{if(invitations.containsKey(slot)){String clan=invitations.get(slot);action(()->button==0?clans.accept(owner.getUuid(),clan):clans.decline(owner.getUuid(),clan),Page.HOME);}}
                case MEMBER->{if(slot==20||slot==22||slot==24){String operation=slot==20?"promote":slot==22?"demote":"kick";action(()->scoped().memberAction(owner.getUuid(),target,operation),Page.HOME);}else if(slot==31)navigate(Page.TRANSFER);}
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
