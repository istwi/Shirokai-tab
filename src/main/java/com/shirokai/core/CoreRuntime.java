package com.shirokai.core;

import com.shirokai.core.config.*;
import com.shirokai.core.customization.*;
import com.shirokai.core.titles.*;
import com.shirokai.core.clans.*;
import com.shirokai.core.chat.*;
import com.shirokai.core.styles.*;
import com.shirokai.core.presentation.*;
import com.shirokai.core.tab.*;
import com.shirokai.core.scoreboard.*;
import com.shirokai.core.integrations.*;
import com.shirokai.core.module.*;
import com.shirokai.core.permissions.*;
import com.shirokai.core.placeholders.PlaceholderService;
import com.shirokai.core.storage.SqliteStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CoreRuntime implements AutoCloseable {
    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
    private final MinecraftServer server;
    private final ConfigService configs;
    private final ModuleManager modules;
    private final ExecutorService io = Executors.newSingleThreadExecutor(task -> new Thread(task, "shirokai-config"));
    private final AtomicBoolean reloading = new AtomicBoolean();
    private final Logger logger;
    private volatile CoreConfig config;
    private volatile boolean closed;
    private SqliteStorage storage;
    private PermissionService permissions;
    private PlaceholderService placeholders;
    private TitleService titles;
    private ClanService clans;
    private MinecraftChat chat;
    private StyleEngine styles;
    private PresentationRuntime presentation;
    private StyleChoiceService customization;
    private TabRuntime tab;
    private ScoreboardRuntime scoreboard;
    private IntegrationRuntime integrations;
    private com.shirokai.core.motd.MotdPlan motd;
    private com.shirokai.core.menus.MenuPlan menus;
    private com.shirokai.core.visibility.VisibilityService visibility;
    public String vanishStatus(){return visibility==null?"DISABLED":visibility.status();}
    private int titleTicks;
    private long visualTicks;
    private com.shirokai.core.customization.PersonalizationConfig personalization;
    private final Path dataDirectory;

    public CoreRuntime(MinecraftServer server, Logger logger) throws Exception {
        this.server = server; this.logger = logger;
        visibility=new com.shirokai.core.visibility.VisibilityService(server,logger);
        dataDirectory = FabricLoader.getInstance().getGameDir().resolve("shirokai-core");
        configs = new ConfigService(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/core.json"));
        config = configs.load();
        menus=new com.shirokai.core.menus.MenuPlan(com.shirokai.core.menus.MenuConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/menus.json")));
        menus.validateItems();
        personalization=com.shirokai.core.customization.PersonalizationConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/personalization.json"));
        modules = new ModuleManager((id, error) -> logger.error("[{}] Lifecycle falhou", id, error));
        modules.register(module("storage", Set.of(), () -> {
            storage = new SqliteStorage(dataDirectory.resolve(config.databaseFile()));
            storage.ready().whenComplete((ignored, error) -> {
                if (error == null) logger.info("[storage] SQLite pronto: {}", config.databaseFile());
                else logger.error("[storage] SQLite indisponível; demais módulos independentes continuam", error);
            });
        }, () -> { if (storage != null) storage.close(); }));
        modules.register(module("permissions", Set.of(), () -> {
            if (FabricLoader.getInstance().isModLoaded("luckperms")) {
                try { permissions = new LuckPermsPermissions(); }
                catch (LinkageError | IllegalStateException failure) {
                    throw new Exception("LuckPerms presente mas API indisponível/incompatível", failure);
                }
            } else {
                permissions = new PermissionService() {
                    public boolean allowed(java.util.UUID player, String permission) { return false; }
                    public String provider() { return "vanilla-op4 (admin fallback only)"; }
                };
                logger.warn("[permissions] LuckPerms ausente: apenas administração vanilla nível 4 disponível");
            }
        }, () -> {}));
        modules.register(module("placeholders", Set.of(), () -> placeholders = new PlaceholderService(
                (key, error) -> logger.warn("[placeholders] Falha em {}", key, error)), () -> {}));
        modules.register(module("titles", Set.of("storage", "permissions"), () -> {
            var path = FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/titles.json");
            var catalog = TitleCatalog.load(path);
            titles = new TitleService(storage, catalog, permissions::allowed, server,
                    id -> server.getPlayerManager().getPlayer(id) != null, java.time.Clock.systemUTC(),
                    (message, error) -> logger.warn("[titles] " + message, error));
            titles.bindRanks(permissions::metadata);
            titles.menus(menus);
            titles.frameSource(server::getTicks,id->tab!=null&&tab.animationsEnabled(id));
            if (placeholders != null) {
                for (String key : java.util.List.of("shirokai:selected_title", "shirokai:chat_tag_1", "shirokai:chat_tag_2"))
                    placeholders.register(key, context -> titles.placeholder(context.playerId(), key));
            }
        }, () -> {}));
        modules.register(module("profiles", Set.of("titles"), () -> {}, () -> {}));
        modules.register(module("clans", Set.of("storage", "permissions"), () -> {
            ClanConfig clanConfig = ClanConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/clans.json"));
            clans = new ClanService(new ClanRepository(storage, java.time.Clock.systemUTC()), permissions, server, clanConfig);
            clans.menuEnabled(menus.config().clanMenuEnabled());
            clans.frameSource(server::getTicks,id->tab!=null&&tab.animationsEnabled(id));
            clans.cacheOnline(id->server.getPlayerManager().getPlayer(id)!=null);
            if(titles!=null){
                titles.bindClans(id->clans.cached(id).flatMap(ClanTitle::from),id->clans.membership(id).thenApply(value->null),id->clans.cached(id).map(ClanSnapshot::name));
                clans.onChange(affected->affected.forEach(titles::refreshCached));
            }
            clans.onChange(affected->{if(scoreboard!=null)scoreboard.invalidate();if(integrations!=null)affected.forEach(integrations::invalidate);});
        }, () -> {}));
        modules.register(module("chat",Set.of("storage","permissions"),()->{
            var chatConfig=ChatConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/chat.json"));
            chat=new MinecraftChat(server,storage,permissions,running("titles")?titles:null,running("clans")?clans:null,chatConfig,logger);
            chat.appearance(ChatAppearanceConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/chat-appearance.json")));
            ConnectionAnnouncements.install(chat);
            new ChatCommands(()->this).configureVanilla(server.getCommandManager().getDispatcher(),chatConfig.takeVanillaPrivateCommands());
            logger.info("[chat] Core controla roteamento; desative modules.chat para usar outro proprietário de chat");
        },()->{if(chat!=null)ConnectionAnnouncements.clear(chat);}));
        modules.register(module("integrations", Set.of("placeholders", "permissions"), () -> {
            var integrationConfig = IntegrationConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/integrations.json"));
            integrations = new IntegrationRuntime(server, integrationConfig, placeholders, permissions, () -> running("clans") ? clans : null, () -> config.unavailablePlaceholder(), logger);
            if(chat!=null){
                chat.shares().providers(integrations::partyShare,integrations::walletShare);
                integrations.trainer(chat::trainer);
                chat.summaryValues((id,key)->{
                    var p=server.getPlayerManager().getPlayer(id);
                    return p==null?"N/D":placeholders.resolve(key,new PlaceholderService.Context(id,p.getGameProfile().getName(),0)).orElse("N/D");
                });
            }
        }, () -> { if (integrations != null) integrations.close(); }));
        modules.register(module("styles",Set.of(),()->{
            styles=new StyleEngine(StyleConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/styles.json")));
            var visual=PresentationConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/presentation.json"));
            presentation=new PresentationRuntime(server,new PresentationPlan(visual,styles,chat==null?ChatConfig.defaults():chat.service().config()),permissions,titles);
            var presets=PresetCatalog.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/presets.json"),styles);
            if(running("storage")&&running("permissions")) {
                customization=new StyleChoiceService(new StyleChoiceRepository(storage),presets,permissions,running("clans")?clans:null,server,id->server.getPlayerManager().getPlayer(id)!=null);
                customization.policy(personalization);customization.animationPreference(id->tab!=null&&tab.animationsEnabled(id));
                customization.onChange(id->{if(scoreboard!=null)scoreboard.invalidate();});
                presentation.customization(customization);
                if(clans!=null)clans.onChange(affected->affected.forEach(id->{if(server.getPlayerManager().getPlayer(id)!=null)customization.join(id).whenComplete((v,e)->{if(e!=null)logger.warn("[styles] Refresh falhou "+id,e);});}));
            }
            if(titles!=null)titles.presentation(presentation);if(clans!=null)clans.presentation(presentation);if(chat!=null)chat.presentation(presentation);
        },()->{}));
        modules.register(module("tab",Set.of("styles","permissions"),()->{
            var tabConfig=TabConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/tab.json"));
            tab=new TabRuntime(server,new TabPlan(tabConfig,styles),permissions,placeholders,running("titles")?titles:null,running("clans")?clans:null,running("storage")?storage:null,()->config.unavailablePlaceholder(),logger);
            tab.configureExtras(new TabExtrasPlan(TabExtrasConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/tab-extras.json")),styles));
            if(tab.extrasPlan().config().nametags().enabled())logger.warn("[tab] Nametags opt-in usam teams globais próprios; teams externos serão preservados");
            if(tabConfig.manageTeams())logger.warn("[tab] Teams habilitados explicitamente: Core usa teams de ordenação sem remover teams externos");
        },()->{if(tab!=null)tab.close();}));
        modules.register(module("scoreboard", Set.of("styles", "permissions"), () -> {
            var boardConfig = ScoreboardConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/scoreboard.json"));
            scoreboard = new ScoreboardRuntime(server, new ScoreboardPlan(boardConfig, styles), permissions, placeholders,
                running("titles") ? titles : null, running("clans") ? clans : null, running("storage") ? storage : null,
                () -> config.unavailablePlaceholder(), tab == null ? null : tab::animationsEnabled, logger);
            if (titles != null) titles.onChange(id -> scoreboard.invalidate());
        }, () -> { if (scoreboard != null) scoreboard.close(); }));
        modules.register(module("administration", Set.of("permissions"), () -> {}, () -> {}));
        for (String id : new String[]{"storage", "permissions", "placeholders", "titles", "profiles", "clans", "chat", "integrations", "styles", "tab", "scoreboard", "administration"}) {
            modules.start(id, config.enabled(id));
            logger.info("[{}] {}", id, modules.states().get(id));
        }
        try {
            motd=new com.shirokai.core.motd.MotdPlan(com.shirokai.core.motd.MotdConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/motd.json")),styles==null?new StyleEngine(StyleConfig.defaults()):styles);
            updateMotd();
        } catch(Exception error){logger.error("[motd] Configuração inválida; MOTD vanilla preservado",error);}
    }
    private static CoreModule module(String id, Set<String> dependencies, CheckedAction start, CheckedAction stop) {
        return new CoreModule() {
            public String id() { return id; }
            public Set<String> dependencies() { return dependencies; }
            public void start() throws Exception { start.run(); }
            public void stop() throws Exception { stop.run(); }
        };
    }
    public StyleChoiceService customization(){return running("styles")?customization:null;}
    public IntegrationRuntime integrations(){if(!running("integrations")||integrations==null)throw new IllegalStateException("Integrações indisponíveis");return integrations;}
    public ScoreboardRuntime scoreboard(){if(!running("scoreboard")||scoreboard==null)throw new IllegalStateException("Scoreboard indisponível");return scoreboard;}
    public TabRuntime tab(){if(!running("tab")||tab==null)throw new IllegalStateException("TAB indisponível");return tab;}
    public MinecraftChat chat(){if(!running("chat")||chat==null)throw new IllegalStateException("Chat indisponível");return chat;}
    public ClanService clans() {
        if (!running("clans") || clans == null) throw new IllegalStateException("Módulo clans indisponível");
        return clans;
    }
    public TitleService titles() {
        if (!running("titles") || titles == null) throw new IllegalStateException("Módulo titles indisponível");
        return titles;
    }
    public void playerJoined(java.util.UUID player) {
        if(customization()!=null)customization.join(player).whenComplete((v,e)->{if(e!=null)logger.warn("[styles] Escolhas indisponíveis "+player,e);});
        if(running("integrations"))integrations.join(player);
        if(running("tab"))tab.join(player);
        if(running("scoreboard"))scoreboard.join(player).whenComplete((v,error)->{if(error!=null)logger.warn("[scoreboard] Preferência indisponível "+player,error);});
        if(running("clans"))clans.membership(player).whenComplete((value,error)->{if(error!=null)logger.warn("[clans] Falha ao carregar associação "+player,error);});
        if(running("chat"))chat.service().join(player).whenComplete((value,error)->{if(error!=null)logger.warn("[chat] Falha ao carregar perfil "+player,error);});
        if (running("clans")) clans.invitations(player).whenCompleteAsync((invites,error) -> {
            if(error!=null){logger.warn("[clans] Falha ao consultar convites "+player,error);return;}
            var online=server.getPlayerManager().getPlayer(player);
            if(online!=null && !invites.isEmpty()) online.sendMessage(com.shirokai.core.messages.Messages.info("Você tem "+invites.size()+" convite(s) de clã. Veja em /clan invites."),false);
        },server);
        if (running("titles")) titles.view(player).whenComplete((value, error) -> {
            if (error != null) logger.warn("[titles] Falha ao carregar perfil " + player, error);
        });
    }
    public void playerLeft(java.util.UUID player) { if(customization!=null)customization.forget(player); if (titles != null) titles.forget(player); if(clans!=null)clans.forget(player); if(chat!=null)chat.service().leave(player);if(tab!=null)tab.forget(player);if(scoreboard!=null)scoreboard.forget(player);if(integrations!=null)integrations.forget(player); }
    private net.minecraft.text.Text renderMotd(com.shirokai.core.motd.MotdPlan plan){
        return plan.config().enabled()?plan.render(visualTicks,(int)server.getPlayerManager().getPlayerList().stream().filter(p->!com.shirokai.core.visibility.VisibilityService.isHidden(server,p)).count(),server.getMaxPlayerCount(),server.getVersion()):null;
    }
    private void updateMotd(){
        if(motd!=null)com.shirokai.core.motd.MotdBridge.publish(renderMotd(motd));
    }
    public void tick() {
        if(visualTicks%5==0)updateMotd();
        if(++visualTicks%5==0&&customization!=null&&customization.catalog().hasAnimations())for(var player:server.getPlayerManager().getPlayerList()){
            if(player.currentScreenHandler instanceof com.shirokai.core.profiles.ProfileMenu menu)menu.refreshVisuals();
            else if(player.currentScreenHandler instanceof com.shirokai.core.customization.StyleChoiceMenu menu)menu.refreshVisuals();
            else if(player.currentScreenHandler instanceof com.shirokai.core.clans.ClanMenu menu)menu.refreshVisuals();
        }
        if(running("integrations"))integrations.tick();
        if(running("tab"))tab.tick();
        if(running("scoreboard"))scoreboard.tick();
        if (running("titles") && ++titleTicks >= 100) { titleTicks = 0; titles.revalidateCached(); }
    }
    public CoreConfig config() { return config; }
    public ModuleManager modules() { return modules; }
    public PermissionService permissions() { return permissions; }
    public PlaceholderService placeholders() { return placeholders; }
    public Path dataDirectory() { return dataDirectory; }
    public SqliteStorage storage() {
        if (storage == null || !running("storage")) throw new IllegalStateException("Módulo storage indisponível");
        return storage;
    }
    public boolean running(String id) { return modules.states().get(id) == ModuleManager.State.RUNNING; }
    public String storageStatus() {
        if (storage == null) return "DISABLED";
        var ready = storage.ready();
        return ready.isCompletedExceptionally() ? "FAILED" : ready.isDone() ? "READY (fila=" + storage.pendingOperations() + ")" : "STARTING";
    }
    /** Parse on IO worker; publish only validated immutable config on server thread. */
    public CompletableFuture<Void> reload() {
        if (closed || !reloading.compareAndSet(false, true))
            return CompletableFuture.failedFuture(new IllegalStateException("Reload indisponível ou já em andamento"));
        var result = new CompletableFuture<Void>();
        CompletableFuture.supplyAsync(() -> {
            try {
                CoreConfig core = configs.load();
                var personalConfig=com.shirokai.core.customization.PersonalizationConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/personalization.json"));
                var menuPlan=new com.shirokai.core.menus.MenuPlan(com.shirokai.core.menus.MenuConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/menus.json")));
                TitleCatalog catalog = titles == null ? null : TitleCatalog.load(
                        FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/titles.json"));
                ClanConfig clanConfig = clans == null ? null : ClanConfig.load(
                        FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/clans.json"));
                ChatConfig chatConfig=chat==null?null:ChatConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/chat.json"));
                var chatAppearance=chat==null?null:ChatAppearanceConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/chat-appearance.json"));
                StyleEngine candidateStyles=styles==null?null:new StyleEngine(StyleConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/styles.json")));
                TabPlan tabPlan=tab==null?null:new TabPlan(TabConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/tab.json")),candidateStyles);
                var extrasPlan=tab==null?null:new TabExtrasPlan(TabExtrasConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/tab-extras.json")),candidateStyles);
                var scoreboardPlan=scoreboard==null?null:new ScoreboardPlan(ScoreboardConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/scoreboard.json")),candidateStyles);
                var presentationPlan=presentation==null?null:new PresentationPlan(PresentationConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/presentation.json")),candidateStyles,chatConfig==null?ChatConfig.defaults():chatConfig);
                var presetCatalog=presentation==null?null:PresetCatalog.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/presets.json"),candidateStyles);
                var integrationConfig=integrations==null?null:IntegrationConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/integrations.json"));
                var motdPlan=new com.shirokai.core.motd.MotdPlan(com.shirokai.core.motd.MotdConfig.load(FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/motd.json")),candidateStyles==null?new StyleEngine(StyleConfig.defaults()):candidateStyles);
                return new ReloadCandidate(motdPlan,core,catalog,clanConfig,chatConfig,candidateStyles,tabPlan,scoreboardPlan,integrationConfig,presentationPlan,presetCatalog,extrasPlan,chatAppearance,menuPlan,personalConfig);
            } catch (Exception failure) { throw new CompletionException(failure); }
        }, io).whenComplete((candidate, error) -> {
            if (closed) { reloading.set(false); result.completeExceptionally(new IllegalStateException("Servidor encerrando")); return; }
            server.execute(() -> {
                try {
                    if (closed) throw new IllegalStateException("Servidor encerrando");
                    if (error != null) throw new CompletionException(error);
                    if (candidate.core().requiresRestartComparedTo(config))
                        throw new IllegalStateException("Alteração de módulos ou databaseFile exige reinício; configuração ativa preservada");
                    if(chat!=null&&candidate.chat().takeVanillaPrivateCommands()!=chat.service().config().takeVanillaPrivateCommands())
                        throw new IllegalStateException("Alteração de takeVanillaPrivateCommands exige reinício");
                    if(tab!=null&&candidate.tab().config().requiresRestartComparedTo(tab.plan().config()))throw new IllegalStateException("Alteração de ownership do TAB exige reinício");
                    if(tab!=null&&candidate.extras().config().requiresRestartComparedTo(tab.extrasPlan().config()))throw new IllegalStateException("Ownership de extras do TAB exige reinício");
                    if(integrations!=null&&candidate.integrations().requiresRestartComparedTo(integrations.config()))throw new IllegalStateException("Ativação de integrações exige reinício");
                    candidate.menus().validateItems();
                    var candidateMotd=renderMotd(candidate.motd());
                    menus=candidate.menus();personalization=candidate.personalization();if(customization!=null)customization.policy(personalization);
                    if(titles!=null)titles.menus(menus);
                    if(clans!=null)clans.menuEnabled(menus.config().clanMenuEnabled());
                    if(!menus.config().clanMenuEnabled())for(var player:server.getPlayerManager().getPlayerList())
                        if(player.currentScreenHandler instanceof com.shirokai.core.clans.ClanMenu || player.currentScreenHandler instanceof com.shirokai.core.clans.ClanIdentityEditor || player.currentScreenHandler instanceof com.shirokai.core.clans.ClanHistoryMenu)player.closeHandledScreen();
                    motd=candidate.motd();com.shirokai.core.motd.MotdBridge.publish(candidateMotd);
                    config = candidate.core();
                    if (titles != null) titles.replaceCatalog(candidate.titles());
                    if (clans != null) clans.replaceConfig(candidate.clans());
                    if(chat!=null){chat.service().replaceConfig(candidate.chat());chat.appearance(candidate.chatAppearance());}
                    if(integrations!=null)integrations.replaceConfig(candidate.integrations());
                    if(customization!=null)customization.replaceCatalog(candidate.presets());
                    if(presentation!=null)presentation.replacePlan(candidate.presentation());
                    styles=candidate.styles();if(tab!=null){tab.configureExtras(candidate.extras());tab.replacePlan(candidate.tab());}if(scoreboard!=null)scoreboard.replacePlan(candidate.scoreboard());
                    logger.info("[core] Configuração recarregada com segurança");
                    result.complete(null);
                } catch (Exception failure) { result.completeExceptionally(failure); }
                finally { reloading.set(false); }
            });
        });
        return result;
    }
    private record ReloadCandidate(com.shirokai.core.motd.MotdPlan motd,CoreConfig core,TitleCatalog titles,ClanConfig clans,ChatConfig chat,StyleEngine styles,TabPlan tab,ScoreboardPlan scoreboard,IntegrationConfig integrations,PresentationPlan presentation,PresetCatalog presets,TabExtrasPlan extras,ChatAppearanceConfig chatAppearance,com.shirokai.core.menus.MenuPlan menus,com.shirokai.core.customization.PersonalizationConfig personalization) {}
    public void prepareStop(){com.shirokai.core.motd.MotdBridge.publish(null);try{if(integrations!=null)integrations.close();}catch(Exception error){logger.error("[integrations] Falha ao encerrar adaptadores",error);}try{if(scoreboard!=null)scoreboard.close();}catch(Exception error){logger.error("[scoreboard] Falha ao liberar recursos",error);}try{if(tab!=null)tab.close();}catch(Exception error){logger.error("[tab] Falha ao liberar recursos antes do save",error);}}
    @Override public void close() throws Exception {
        closed = true;
        if(visibility!=null)visibility.close();
        io.shutdown();
        modules.close();
        if (!io.awaitTermination(10, TimeUnit.SECONDS)) logger.warn("[core] Worker de configuração ainda encerrando");
        logger.info("[core] Encerramento concluído: {}", modules.states());
    }
}
