package com.shirokai.core.clans;

import com.shirokai.core.permissions.PermissionService;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Permission snapshots and membership cache are confined to the server executor. */
public final class ClanService {
    private static final class State {
        ClanConfig config;
        com.shirokai.core.presentation.SurfaceRenderer presentation=com.shirokai.core.presentation.SurfaceRenderer.literal();
        Predicate<UUID> cacheable=id->false;
        final Map<UUID,Optional<ClanSnapshot>> cache=new HashMap<>();
        final List<Consumer<Set<UUID>>> listeners=new ArrayList<>();
        State(ClanConfig config){this.config=config;}
    }
    private final ClanRepository repository;
    private final PermissionService permissions;
    private final Executor server;
    private final State state;
    public ClanService(ClanRepository repository,PermissionService permissions,Executor server,ClanConfig config){this(repository,permissions,server,new State(config));}
    private ClanService(ClanRepository repository,PermissionService permissions,Executor server,State state){this.repository=repository;this.permissions=permissions;this.server=server;this.state=state;}
    public ClanService scoped(UUID expectedClan){return new ClanService(repository.scoped(expectedClan),permissions,server,state);}
    public void presentation(com.shirokai.core.presentation.SurfaceRenderer renderer){state.presentation=renderer;}
    public boolean menuPresentationEnabled(){return state.presentation.menusEnabled();}
    public net.minecraft.text.Text present(UUID subject,com.shirokai.core.presentation.Surface surface,String value){return state.presentation.render(subject,surface,value);}
    public ClanConfig config(){return state.config;}
    public void replaceConfig(ClanConfig value){state.config=value;}
    public void cacheOnline(Predicate<UUID> predicate){state.cacheable=predicate;}
    public void onChange(Consumer<Set<UUID>> listener){state.listeners.add(listener);}
    public void forget(UUID player){state.cache.remove(player);}
    public Optional<ClanSnapshot> cached(UUID player){return state.cache.getOrDefault(player,Optional.empty());}
    private void remember(UUID player,Optional<ClanSnapshot> clan){if(state.cacheable.test(player))state.cache.put(player,clan);}
    private CompletableFuture<ClanSnapshot> changed(CompletableFuture<ClanSnapshot> operation,Set<UUID> extras,boolean removed){
        return operation.thenApplyAsync(clan->{
            var affected=new HashSet<>(clan.members().keySet());affected.addAll(extras);
            for(UUID player:affected)remember(player,!removed&&clan.members().containsKey(player)?Optional.of(clan):Optional.empty());
            for(var listener:state.listeners)try{listener.accept(Set.copyOf(affected));}catch(Exception error){org.slf4j.LoggerFactory.getLogger("shirokai-core").error("[clans] Falha ao atualizar integração",error);}
            return clan;
        },server);
    }
    public int limit(UUID leader){
        int limit=config().defaultLimit();
        for(var entry:config().permissionLimits().entrySet())if(permissions.allowed(leader,entry.getKey()))limit=Math.max(limit,entry.getValue());
        return limit;
    }
    public CompletableFuture<Optional<ClanSnapshot>> membership(UUID player){return repository.membership(player).thenApplyAsync(clan->{remember(player,clan);return clan;},server);}
    public CompletableFuture<Optional<ClanRepository.TransferOffer>> transferOffer(UUID player){return repository.transferOffer(player);}
    public CompletableFuture<Map<String,Long>> invitations(UUID player){return repository.invitations(player);}
    public CompletableFuture<ClanSnapshot> own(UUID player){return repository.own(player).thenApplyAsync(clan->{remember(player,Optional.of(clan));return clan;},server);}
    public CompletableFuture<ClanSnapshot> find(String name){return repository.find(name);}
    public CompletableFuture<ClanSnapshot> create(UUID actor,String name,long playedSeconds){
        if(!config().creationEnabled())throw new IllegalStateException("Criação de clãs desativada");
        if(!permissions.allowed(actor,"shirokaicore.clan.create"))throw new IllegalStateException("Sem permissão shirokaicore.clan.create");
        if(playedSeconds<config().minimumPlaySeconds())throw new IllegalStateException("Tempo mínimo de jogo não atingido");
        return changed(repository.create(actor,config().name(name),config()),Set.of(actor),false);
    }
    public CompletableFuture<ClanSnapshot> identity(UUID actor,String value,boolean tag){return changed(repository.identity(actor,tag?config().tag(value):config().name(value),tag,config()),Set.of(actor),false);}
    public CompletableFuture<ClanSnapshot> invite(UUID actor,UUID target){return repository.invite(actor,target,config());}
    public CompletableFuture<ClanSnapshot> accept(UUID actor,String name){
        var operation=repository.find(name).thenComposeAsync(clan->permissions.prepare(clan.leader()).thenComposeAsync(v->repository.accept(actor,clan.id(),clan.leader(),limit(clan.leader())),server),server);
        return changed(operation,Set.of(actor),false);
    }
    public CompletableFuture<ClanSnapshot> decline(UUID actor,String name){return repository.decline(actor,name);}
    public CompletableFuture<ClanSnapshot> cancel(UUID actor,UUID target){return repository.cancel(actor,target,config());}
    public CompletableFuture<ClanSnapshot> leave(UUID actor){return changed(repository.leave(actor),Set.of(actor),false);}
    public CompletableFuture<ClanSnapshot> memberAction(UUID actor,UUID target,String action){return changed(repository.memberAction(actor,target,action,config()),Set.of(actor,target),false);}
    public CompletableFuture<ClanSnapshot> transfer(UUID actor,UUID target){return repository.requestTransfer(actor,target,config());}
    public CompletableFuture<ClanSnapshot> answerTransfer(UUID actor,boolean accept){return changed(repository.answerTransfer(actor,accept),Set.of(actor),false);}
    public CompletableFuture<ClanSnapshot> cancelDisband(UUID actor){return repository.cancelDisband(actor);}
    public CompletableFuture<ClanSnapshot> disband(UUID actor,boolean confirm){var operation=repository.disband(actor,confirm,config());return confirm?changed(operation,Set.of(actor),true):operation;}
    public CompletableFuture<Integer> currentLimit(ClanSnapshot clan){return permissions.prepare(clan.leader()).thenApplyAsync(v->limit(clan.leader()),server);}
}
