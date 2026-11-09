package com.shirokai.core.customization;

import com.shirokai.core.clans.*;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.presentation.Surface;
import net.minecraft.text.Text;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Policy/cache live on the server executor. Rendering performs no JDBC. */
public final class StyleChoiceService {
    public record View(StyleChoices choices, Surface surface, PresetCatalog catalog) {}
    private final StyleChoiceRepository repository;
    private final PermissionService permissions;
    private final ClanService clans;
    private final Executor server;
    private final Predicate<UUID> online;
    private final Map<UUID,StyleChoices> players=new HashMap<>(), clanChoices=new HashMap<>();
    private final Map<UUID,Object> sessions=new HashMap<>();
    private volatile PresetCatalog catalog;
    private Consumer<UUID> changed=id->{};
    public StyleChoiceService(StyleChoiceRepository repository,PresetCatalog catalog,PermissionService permissions,
                              ClanService clans,Executor server,Predicate<UUID> online) {
        this.repository=repository;this.catalog=catalog;this.permissions=permissions;this.clans=clans;this.server=server;this.online=online;
    }
    public void onChange(Consumer<UUID> listener) {changed=listener;}
    public PresetCatalog catalog(){return catalog;}
    public void replaceCatalog(PresetCatalog candidate){catalog=candidate;for(UUID id:sessions.keySet())changed.accept(id);}
    private void remember(StyleChoices value) {
        var cache=value.clan()?clanChoices:players;
        var previous=cache.get(value.owner());
        if(previous==null||previous.revision()<=value.revision())cache.put(value.owner(),value);
    }
    public CompletableFuture<Void> join(UUID player) {
        Object session=new Object();sessions.put(player,session);
        return repository.load(player,false).thenComposeAsync(personal->{
            if(sessions.get(player)!=session||!online.test(player))return CompletableFuture.completedFuture(null);
            remember(personal);
            if(clans==null)return CompletableFuture.completedFuture(null);
            return clans.membership(player).thenComposeAsync(clan->{
                if(clan.isEmpty())return CompletableFuture.completedFuture(null);
                var snapshot=clan.get();
                return permissions.prepare(snapshot.leader()).thenComposeAsync(v->repository.load(snapshot.id(),true),server)
                    .thenAcceptAsync(value->{if(sessions.get(player)==session&&online.test(player)){remember(value);changed.accept(player);}},server);
            },server);
        },server).thenRunAsync(()->{prune();if(sessions.get(player)==session)changed.accept(player);},server);
    }
    public void forget(UUID player){sessions.remove(player);players.remove(player);prune();}
    private void prune(){
        if(clans==null){clanChoices.clear();return;}
        var retained=new HashSet<UUID>();
        for(UUID player:sessions.keySet())clans.cached(player).ifPresent(c->retained.add(c.id()));
        clanChoices.keySet().retainAll(retained);
    }
    public CompletableFuture<View> view(UUID actor,Surface surface,UUID expectedClan) {
        if(!PresetCatalog.SURFACES.contains(surface))throw new IllegalArgumentException("Superfície sem escolha");
        if(surface==Surface.TITLE)return repository.load(actor,false).thenApplyAsync(c->new View(c,surface,catalog),server);
        if(clans==null)throw new IllegalStateException("Clãs indisponíveis");
        return clans.membership(actor).thenComposeAsync(optional->{
            var clan=optional.orElseThrow(()->new IllegalStateException("Você não pertence a um clã"));
            if(expectedClan!=null&&!expectedClan.equals(clan.id()))throw new IllegalStateException("Clã mudou; reabra o menu");
            if(!clan.leader().equals(actor))throw new IllegalStateException("Somente o líder pode personalizar o clã");
            return repository.load(clan.id(),true).thenApplyAsync(c->new View(c,surface,catalog),server);
        },server);
    }
    public boolean allowed(UUID actor,Surface surface,String id){return catalog.allowed(id,surface,node->permissions.allowed(actor,node));}
    public CompletableFuture<StyleChoices> choose(UUID actor,View view,String id,BooleanSupplier active) {
        // Called on the server thread immediately before enqueueing the transaction, not when the menu was drawn.
        if(!active.getAsBoolean()||view.catalog()!=catalog)throw new IllegalStateException("Tela/configuração mudou; reabra o menu");
        if(id!=null&&!allowed(actor,view.surface(),id))throw new IllegalStateException("Preset removido ou sem permissão: "+id);
        return repository.save(actor,view.choices(),view.surface(),id,()->catalog==view.catalog())
            .thenApplyAsync(value->{
                if(value.clan()) {
                    if(clans!=null)for(UUID member:List.copyOf(sessions.keySet()))
                        if(clans.cached(member).map(c->c.id().equals(value.owner())).orElse(false)){remember(value);changed.accept(member);}
                } else if(online.test(actor)&&sessions.containsKey(actor)){remember(value);changed.accept(actor);}
                return value;
            },server);
    }
    public PresetCatalog.Resolution resolution(UUID subject,Surface surface){
        if(surface==Surface.TITLE) {
            var choices=players.get(subject);
            return catalog.resolve(choices==null?null:choices.selections().get(surface),surface,node->permissions.allowed(subject,node));
        }
        var clan=clans==null?Optional.<ClanSnapshot>empty():clans.cached(subject);
        var choices=clan.map(c->clanChoices.get(c.id())).orElse(null);
        return catalog.resolve(choices==null?null:choices.selections().get(surface),surface,
            node->clan.map(c->permissions.allowed(c.leader(),node)).orElse(false));
    }
    private PersonalizationConfig policy=PersonalizationConfig.defaults();
    private Predicate<UUID> animations=id->false;
    public void policy(PersonalizationConfig value){policy=Objects.requireNonNull(value);}
    public void animationPreference(Predicate<UUID> preference){animations=preference;}
    public boolean animates(UUID subject,Surface surface,VisualTarget target){var rule=policy.surfaces().get(target);UUID owner=surface==Surface.TITLE?subject:clans==null?subject:clans.cached(subject).map(ClanSnapshot::leader).orElse(subject);return rule.enabled()&&rule.animated()&&animations.test(subject)&&(rule.permission().isEmpty()||permissions.allowed(owner,rule.permission()));}
    public Optional<Text> render(UUID subject,Surface surface,String literal){return render(subject,surface,literal,VisualTarget.MENU,0,false);}
    public Optional<Text> render(UUID subject,Surface surface,String literal,VisualTarget target,long tick,boolean animate) {
        if(!PresetCatalog.SURFACES.contains(surface))return Optional.empty();
        var rule=policy.surfaces().get(target);
        UUID entitlement=surface==Surface.TITLE?subject:clans==null?subject:clans.cached(subject).map(ClanSnapshot::leader).orElse(subject);
        if(!rule.enabled()||!rule.permission().isEmpty()&&!permissions.allowed(entitlement,rule.permission()))return Optional.empty();
        var selected=resolution(subject,surface);
        return selected.template()==null?Optional.empty():Optional.of(catalog.render(selected,literal,tick,animate&&rule.animated()&&animations.test(subject)));
    }
}
