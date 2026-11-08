package com.shirokai.core.titles;

import com.shirokai.core.storage.SqliteStorage;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** All policy and cache access run on serverExecutor; only repository work runs on JDBC worker. */
public final class TitleService {
    private final SqliteStorage storage;
    private final Executor serverExecutor;
    private final Predicate<UUID> cacheable;
    private final BiConsumer<String, Throwable> errors;
    private final Clock clock;
    private final TitleRules rules;
    private final Map<UUID, TitleState> cache = new HashMap<>();
    private final Set<UUID> refreshing = new HashSet<>();
    private com.shirokai.core.presentation.SurfaceRenderer presentation=com.shirokai.core.presentation.SurfaceRenderer.literal();
    public void presentation(com.shirokai.core.presentation.SurfaceRenderer renderer){presentation=renderer;}
    public boolean customizationEnabled(){return presentation.customizationEnabled();}
    public void openCustomization(net.minecraft.server.network.ServerPlayerEntity player){presentation.openCustomization(player,com.shirokai.core.presentation.Surface.TITLE,null);}
    public boolean menuPresentationEnabled(){return presentation.menusEnabled();}
    public net.minecraft.text.Text present(UUID player,com.shirokai.core.presentation.Surface surface,String value){return presentation.render(player,surface,value);}
    public net.minecraft.text.Text display(UUID player,TitleDefinition definition){return presentation.renderTitle(player,definition);}
    private Function<UUID,com.shirokai.core.permissions.PermissionService.Metadata> metadata = id -> com.shirokai.core.permissions.PermissionService.Metadata.unavailable();
    public void bindRanks(Function<UUID,com.shirokai.core.permissions.PermissionService.Metadata> provider){metadata=Objects.requireNonNull(provider);}
    /** Live context-aware metadata: no saved rank, SQL query or player selection. */
    public Optional<TitleDefinition> rank(UUID player){
        var group=metadata.apply(player).group();
        if(group==null || group.isBlank())return Optional.empty();
        // Exact primary group wins over inherited permissions (including wildcard permissions).
        var configured=catalog.definitions().values().stream()
            .filter(t->t.category()==TitleDefinition.Category.ROLE)
            .filter(t->t.id().equalsIgnoreCase(group) || ("group."+group).equalsIgnoreCase(t.permission()))
            .sorted(Comparator.comparingInt(TitleDefinition::priority).reversed().thenComparing(TitleDefinition::id)).findFirst();
        if(configured.isPresent())return configured;
        String normalized=group.toLowerCase(Locale.ROOT);
        String safeId=normalized.matches("[a-z0-9][a-z0-9_-]{0,63}") && !normalized.startsWith("clan_")?normalized:"rank_"+Integer.toUnsignedString(group.hashCode(),36);
        String label=group.codePoints().filter(c->!Character.isISOControl(c)).limit(32).collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString();
        return Optional.of(new TitleDefinition(safeId,label,"["+label.toUpperCase(Locale.ROOT)+"]","Cargo fixo LuckPerms",TitleDefinition.Category.ROLE,0,"group."+safeId,false,true,true,true));
    }
    public net.minecraft.text.Text visualPresentation(UUID player){return rank(player).map(def->display(player,def)).orElseGet(net.minecraft.text.Text::empty);}
    public List<TitleDefinition> chatDefinitions(UUID player){
        var result=new ArrayList<TitleDefinition>();rank(player).ifPresent(result::add);
        cached(player).ifPresent(state->state.chatIds().stream().limit(2-result.size()).map(catalog(player).definitions()::get).filter(Objects::nonNull).forEach(result::add));
        return List.copyOf(result);
    }
    public List<net.minecraft.text.Text> chatPresentation(UUID player){return chatDefinitions(player).stream().map(def->display(player,def)).toList();}
    private TitleCatalog catalog;
    private Consumer<UUID> changed = id -> {};
    public void onChange(Consumer<UUID> listener) { changed = changed.andThen(listener); }
    private Function<UUID,Optional<String>> clanName=player->Optional.empty();
    private Function<UUID,Optional<TitleDefinition>> dynamic=player->Optional.empty();
    private Function<UUID,CompletableFuture<Void>> refresh=player->CompletableFuture.completedFuture(null);
    public TitleService(SqliteStorage storage, TitleCatalog catalog, BiPredicate<UUID, String> permissions,
                        Executor serverExecutor, Predicate<UUID> cacheable, Clock clock, BiConsumer<String, Throwable> errors) {
        this.storage = storage; this.catalog = catalog; this.rules = new TitleRules(permissions,(player,id)->dynamic.apply(player).map(title->title.id().equals(id)).orElse(false));
        this.serverExecutor = serverExecutor; this.cacheable = cacheable; this.clock = clock; this.errors = errors;
    }
    public TitleCatalog catalog() { return catalog; }
    public TitleCatalog catalog(UUID player){
        var definitions=new HashMap<>(catalog.definitions());dynamic.apply(player).ifPresent(title->definitions.put(title.id(),title));
        return new TitleCatalog(1,definitions);
    }
    public void bindClans(Function<UUID,Optional<TitleDefinition>> provider,Function<UUID,CompletableFuture<Void>> refresh){bindClans(provider,refresh,id->provider.apply(id).map(TitleDefinition::name));}
    public void bindClans(Function<UUID,Optional<TitleDefinition>> provider,Function<UUID,CompletableFuture<Void>> refresh,Function<UUID,Optional<String>> name){this.dynamic=provider;this.refresh=refresh;this.clanName=name;}
    public Optional<String> clanName(UUID player){return clanName.apply(player);}
    public void refreshCached(UUID player){if(cacheable.test(player))view(player).whenCompleteAsync((state,error)->{if(error!=null)errors.accept("Atualização de clã "+player,error);},serverExecutor);}
    public TitleRules rules() { return rules; }
    public long now() { return clock.millis(); }
    public void replaceCatalog(TitleCatalog value) { catalog = value; revalidateCached(); }
    public void forget(UUID player) { cache.remove(player); }
    private TitleState remember(TitleState state) {
        if (cacheable.test(state.player())) {
            var previous = cache.get(state.player());
            if (previous == null || previous.revision() <= state.revision()) {
                cache.put(state.player(), state);
                if (!state.equals(previous)) changed.accept(state.player());
            }
        }
        return rules.normalize(state, catalog(state.player()), now());
    }
    private static boolean sameSelections(TitleState first, TitleState second) {
        return Objects.equals(first.selectedVisual(), second.selectedVisual()) && first.chatIds().equals(second.chatIds());
    }
    public CompletableFuture<TitleState> view(UUID player) {
        return refresh.apply(player).thenComposeAsync(v->storage.titleState(player),serverExecutor).thenComposeAsync(state -> {
            var normalized = rules.normalize(state, catalog(state.player()), now());
            // Offline permissions may not be loaded; never erase an offline player's selections from a diagnostic read.
            return !cacheable.test(player) || sameSelections(state, normalized) ? CompletableFuture.completedFuture(state) : storage.saveTitles(normalized);
        }, serverExecutor).thenApplyAsync(this::remember, serverExecutor);
    }
    public CompletableFuture<TitleState> equip(UUID player, String id, boolean chat) {
        return refresh.apply(player).thenComposeAsync(v->storage.titleState(player),serverExecutor).thenComposeAsync(state -> {
                var normalized=rules.normalize(state,catalog(player),now());
                if(chat && rank(player).isPresent() && !normalized.chatIds().contains(id) && !normalized.chatIds().isEmpty())
                    throw new IllegalArgumentException("O cargo ocupa uma identificação; remova a tag opcional antes de escolher outra");
                return storage.saveTitles(rules.equip(state, catalog(player), id, chat, now()));
            }, serverExecutor)
                .thenApplyAsync(this::remember, serverExecutor);
    }
    public CompletableFuture<TitleState> clear(UUID player) {
        return storage.titleState(player).thenComposeAsync(state -> storage.saveTitles(
                new TitleState(player, state.revision(), state.grants(), null, List.of())), serverExecutor)
                .thenApplyAsync(this::remember, serverExecutor);
    }
    public CompletableFuture<TitleState> grant(UUID actor, UUID player, String id, long expiry) {
        requireDefinition(id);
        if (expiry != 0 && expiry <= now()) throw new IllegalArgumentException("Expiração deve estar no futuro");
        return storage.grantTitle(player, id, expiry, actor).thenComposeAsync(state -> view(player), serverExecutor);
    }
    public CompletableFuture<TitleState> revoke(UUID actor, UUID player, String id) {
        requireDefinition(id);
        return storage.revokeTitle(player, id, actor).thenComposeAsync(state -> view(player), serverExecutor);
    }
    private void requireDefinition(String id) {
        if (!catalog.definitions().containsKey(id)) throw new IllegalArgumentException("Título desconhecido: " + id);
    }
    /** Synchronous rendering path: never queries SQLite unless an invalid selection needs cleanup. */
    public Optional<TitleState> cached(UUID player) {
        var state = cache.get(player);
        if (state == null) return Optional.empty();
        var valid = rules.normalize(state, catalog(state.player()), now());
        if (!sameSelections(state, valid) && refreshing.add(player)) {
            view(player).whenCompleteAsync((ignored, error) -> {
                refreshing.remove(player);
                if (error != null) errors.accept("Revalidação de título " + player, error);
            }, serverExecutor);
        }
        return Optional.of(valid);
    }
    public void revalidateCached() { for (UUID player : List.copyOf(cache.keySet())) cached(player); }
    public Optional<String> placeholder(UUID player, String key) {
        if (player == null) return Optional.empty();
        if(key.equals("shirokai:selected_title"))return rank(player).map(TitleDefinition::display);
        int index=switch(key){case "shirokai:chat_tag_1"->0;case "shirokai:chat_tag_2"->1;default->-1;};
        var tags=chatDefinitions(player);
        return index>=0&&index<tags.size()?Optional.of(tags.get(index).display()):Optional.empty();
    }
}
