package com.shirokai.core.cosmetics;

import com.shirokai.core.CoreRuntime;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.particle.*;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import java.io.IOException;
import java.util.*;

/** Server-thread-only visuals: no inventory edits, potion effects, damage or flight. */
public final class CosmeticService {
    private final CoreRuntime app;
    private final MinecraftServer server;
    private final CosmeticRepository repository;
    private final CosmeticLease lease=new CosmeticLease();
    private final Map<UUID,Long> revisions=new HashMap<>();
    private final Set<UUID> saving=new HashSet<>();
    private final Map<UUID,Object> sessions=new HashMap<>();
    private final Map<UUID,java.util.concurrent.CompletableFuture<Void>> loading=new HashMap<>();
    private final java.util.concurrent.CompletableFuture<Void> ready;
    private boolean closed;
    private String status="STARTING";
    private static final Map<MinecraftServer,CosmeticService> ACTIVE=new java.util.WeakHashMap<>();
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("shirokai-core");
    private CosmeticSettings config=new CosmeticSettings();
    private final Map<UUID,CosmeticState> selected=new HashMap<>();
    private final Map<UUID,Vec3d> trailAnchors=new HashMap<>();
    private final Map<UUID,Double> trailCredits=new HashMap<>();
    private final Map<UUID,String> worlds=new HashMap<>();
    private final Map<UUID,Long> lastCelebration=new HashMap<>();
    private long ticks;
    private long nextErrorLog;
    private int globalBudget, playerBudget;
    private int rotation;
    public CosmeticService(CoreRuntime app, MinecraftServer server) {
        this.app=app;this.server=server;
        repository=new CosmeticRepository(app.storage(),server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("shirokai-essentials/players"));
        ready=repository.initialize(configPath(),net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("shirokai-essentials/cosmetics.json"))
            .thenAcceptAsync(value->{
                if(closed)return;
                try {lease.attach();config=value;ACTIVE.put(server,this);status="READY";for(var p:server.getPlayerManager().getPlayerList())join(p.getUuid());}
                catch(ReflectiveOperationException|RuntimeException e){status="FAILED: atualize o Essentials do pacote";LOG.error("[cosmetics] Handoff recusado; efeitos Core suspensos",e);}
            },server);
        ready.whenCompleteAsync((v,error)->{if(error!=null){status="FAILED";LOG.error("[cosmetics] Inicialização recusada; arquivos preservados",error);}},server);
    }
    private java.nio.file.Path configPath(){return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("shirokai-core/cosmetics.json");}
    public CosmeticSettings config(){return config;}
    public String status(){return status;}
    public java.util.concurrent.CompletableFuture<Void> ready(){return status.equals("READY")?java.util.concurrent.CompletableFuture.completedFuture(null):ready.copy();}
    public java.util.concurrent.CompletableFuture<Void> reload(){return repository.initialize(configPath(),net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("shirokai-essentials/cosmetics.json")).thenAcceptAsync(value->{
        if(closed)return;
        if(!status.equals("READY"))try{lease.attach();ACTIVE.put(server,this);status="READY";}catch(ReflectiveOperationException e){throw new java.util.concurrent.CompletionException(e);}
        config=value;for(var id:selected.keySet())app.cosmeticsChanged(id);for(var p:server.getPlayerManager().getPlayerList())if(!selected.containsKey(p.getUuid()))join(p.getUuid());
    },server);}
    private ServerPlayerEntity online(UUID id){return server.getPlayerManager().getPlayerList().stream().filter(p->p.getUuid().equals(id)).findFirst().orElse(null);}
    public java.util.concurrent.CompletableFuture<Void> join(UUID id){
        if(closed)return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("Cosméticos encerrados"));
        if(selected.containsKey(id))return java.util.concurrent.CompletableFuture.completedFuture(null);
        if(loading.containsKey(id))return loading.get(id);
        var token=new Object();sessions.put(id,token);
        var task=ready().thenCompose(v->{if(!status.equals("READY"))throw new IllegalStateException(status);return repository.load(id);}).thenAcceptAsync(profile->{
            if(closed||sessions.get(id)!=token)return;var p=online(id);if(p==null)return;
            selected.put(id,profile.state());revisions.put(id,profile.revision());remember(p);resetTrail(p);entrances.put(id,ticks+40);app.cosmeticsChanged(id);
        },server);
        loading.put(id,task);task.whenCompleteAsync((v,e)->{if(sessions.get(id)==token)loading.remove(id);if(e!=null)LOG.warn("[cosmetics] Perfil indisponível "+id,e);},server);return task;
    }
    public void forget(UUID id){sessions.remove(id);loading.remove(id);selected.remove(id);revisions.remove(id);trailAnchors.remove(id);trailCredits.remove(id);worlds.remove(id);entrances.remove(id);lastCelebration.remove(id);}
    public void close(){closed=true;ACTIVE.remove(server,this);lease.close();selected.clear();sessions.clear();loading.clear();trailAnchors.clear();trailCredits.clear();worlds.clear();entrances.clear();}
    private final Map<UUID,Long> entrances=new HashMap<>();
    public static Text message(String text) {return Text.literal("✦ ").formatted(Formatting.AQUA).append(Text.literal("ZORYKAI ").formatted(Formatting.WHITE,Formatting.BOLD)).append(Text.literal("» "+text).formatted(Formatting.GRAY));}
    void error(ServerPlayerEntity p,Exception error) {
        LOG.error("Falha em cosméticos; seleção anterior preservada",error);
        if(p!=null)p.sendMessage(message("Não foi possível salvar. A seleção anterior foi preservada."),false);
    }
    public boolean canUse(ServerPlayerEntity p) {return !closed&&status.equals("READY")&&config.enabled&&app.permissions().allowed(p.getUuid(),"shirokai.core.cosmetics.use");}
    public boolean canCustomize(ServerPlayerEntity p) {return canUse(p)&&app.permissions().allowed(p.getUuid(),"shirokai.core.cosmetics.customize");}
    public boolean unlocked(ServerPlayerEntity p,CosmeticCatalog.Entry e) {
        return canUse(p)&&!config.disabled.contains(e.id())&&app.permissions().allowed(p.getUuid(),"shirokai.core.cosmetics."+e.id())&&(!e.category().equals("nick")||config.nickEnabled);
    }
    public CosmeticState state(ServerPlayerEntity p) throws IOException {
        var state=selected.get(p.getUuid());if(state==null)throw new IOException("Perfil carregando; tente novamente");return state;
    }
    public void open(ServerPlayerEntity p,String page) {
        if(!canUse(p)){p.sendMessage(message("Cosméticos indisponíveis ou sem permissão."),false);return;}
        if(!selected.containsKey(p.getUuid())){var previous=p.currentScreenHandler;join(p.getUuid()).whenCompleteAsync((v,e)->{if(e!=null)error(p,new IOException(e));else if(!closed&&online(p.getUuid())==p&&p.currentScreenHandler==previous)open(p,page);},server);return;}
        p.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory((id,inventory,player)->new CosmeticScreen(id,inventory,p,this,page),Text.literal("✦ Cosméticos · "+CosmeticScreen.title(page)).formatted(Formatting.DARK_AQUA)));
    }
    public void equip(ServerPlayerEntity p,CosmeticCatalog.Entry e) {
        if(!unlocked(p,e)){p.sendMessage(message("Esta opção está bloqueada."),false);return;}
        save(p,d->{d.cosmetics.put(e.category(),e.id());d.cosmeticsHidden=false;});
    }
    public void remove(ServerPlayerEntity p,String category) {save(p,d->d.cosmetics.remove(category));}
    public void disable(ServerPlayerEntity p) {save(p,d->d.cosmeticsHidden=true);}
    public void resume(ServerPlayerEntity p) {save(p,d->d.cosmeticsHidden=false);}
    public long revision(ServerPlayerEntity p){return revisions.getOrDefault(p.getUuid(),-1L);}
    public void preferences(ServerPlayerEntity p,CosmeticState.Preferences value,long expected) {
        if(revision(p)!=expected){p.sendMessage(message("A seleção mudou. Reabra a personalização."),false);return;}
        preferences(p,value);
    }
    public void preferences(ServerPlayerEntity p,CosmeticState.Preferences value) {
        if(!canCustomize(p)){p.sendMessage(message("Você não tem permissão para personalizar."),false);return;}
        if(value.speed<1||value.speed>config.maxSpeed||value.size<1||value.size>config.maxSize||value.density<1||value.density>config.maxDensity||Math.abs(value.direction)!=1){p.sendMessage(message("Personalização fora dos limites do servidor."),false);return;}
        save(p,d->{d.cosmeticPreferences.speed=value.speed;d.cosmeticPreferences.size=value.size;d.cosmeticPreferences.density=value.density;d.cosmeticPreferences.direction=value.direction;});
    }
    public void preview(ServerPlayerEntity p,CosmeticState.Preferences value) {
        if(!canCustomize(p)||!visible(p))return;
        if(globalBudget<=0)return;playerBudget=Math.min(config.maxParticlesPerPlayer,12);
        value=bounded(value);
        double radius=.25+value.size*.2, phase=ticks*(.04*value.speed)*value.direction;
        for(int i=0;i<Math.min(12,value.density*4);i++){double progress=i/(double)Math.max(1,value.density*4-1),a=phase+progress*Math.PI*4*value.direction;emit(p,"stars",p.getPos().add(Math.cos(a)*radius,2-progress*1.9,Math.sin(a)*radius));}
    }
    private void save(ServerPlayerEntity p,java.util.function.Consumer<CosmeticState> change) {
        if(!canUse(p)||online(p.getUuid())!=p)return;
        var id=p.getUuid();if(!selected.containsKey(id)||saving.contains(id)){p.sendMessage(message("Aguarde o salvamento anterior."),false);return;}
        var next=selected.get(id).copy();change.accept(next);var token=sessions.get(id);saving.add(id);
        repository.save(id,revisions.get(id),next).whenCompleteAsync((profile,e)->{
            saving.remove(id);var current=online(id);if(closed||sessions.get(id)!=token||current==null)return;
            if(e!=null){error(current,new IOException(e));return;}
            selected.put(id,profile.state());revisions.put(id,profile.revision());app.cosmeticsChanged(id);
            current.sendMessage(message("Seleção de cosméticos atualizada."),false);
            if(current.currentScreenHandler instanceof CosmeticScreen screen)screen.refresh();
        },server);
    }
    private boolean visible(ServerPlayerEntity p) {
        if(!canUse(p)||!p.isAlive()||p.isSpectator()||p.isInvisible()||com.shirokai.core.visibility.VisibilityService.isHidden(server,p)||!lease.allowed(p.getUuid(),config.suppressInBattle))return false;
        return !config.suppressInBattle||!CosmeticBattles.blocked(p);
    }
    private CosmeticCatalog.Entry equipped(ServerPlayerEntity p,String category) {
        var state=selected.get(p.getUuid());if(state==null||state.cosmeticsHidden)return null;
        var e=CosmeticCatalog.find(state.cosmetics.get(category));return e!=null&&e.category().equals(category)&&unlocked(p,e)?e:null;
    }
    private void remember(ServerPlayerEntity p) {worlds.put(p.getUuid(),p.getServerWorld().getRegistryKey().getValue().toString());}
    public void tick(MinecraftServer server) {
        if(closed||!status.equals("READY"))return;
        ticks++;globalBudget=config.maxParticlesPerTick;
        var players=server.getPlayerManager().getPlayerList();
        if(players.isEmpty())return;
        if(ticks%10==0&&players.stream().anyMatch(p->{var e=equipped(p,"nick");return e!=null&&e.style().equals("rainbow");}))app.cosmeticAnimationFrame();
        // Rotate priority so a busy server cannot permanently starve the last players.
        for(int i=0;i<players.size();i++) {
            var p=players.get((i+rotation)%players.size());
            try {
                if(!visible(p)||selected.get(p.getUuid())==null||selected.get(p.getUuid()).cosmeticsHidden) {remember(p);resetTrail(p);entrances.remove(p.getUuid());continue;}
                playerBudget=config.maxParticlesPerPlayer;
                if(entrances.getOrDefault(p.getUuid(),Long.MAX_VALUE)<=ticks) {entrances.remove(p.getUuid());burst(p,"join",p.getPos());}
                String oldWorld=worlds.get(p.getUuid());
                boolean same=p.getServerWorld().getRegistryKey().getValue().toString().equals(oldWorld);
                Vec3d anchor=trailAnchors.get(p.getUuid());double trailDistance=anchor==null?0:p.getPos().squaredDistanceTo(anchor);
                var trail=equipped(p,"trail");
                if(!same||!p.isOnGround()||trail==null||trailDistance>=16)resetTrail(p);
                else if(trailDistance>.0004) {
                    var pref=preferences(p);boolean heart=trail.style().equals("heart");
                    double rate=(config.trailBaseDensity*pref.density*trailMultiplier(trail.style()))/(double)config.trailIntervalTicks;
                    if(heart)rate=Math.max(rate,config.trailMinimumPerTick*pref.density)*.25;
                    double credit=trailCredits.getOrDefault(p.getUuid(),0d)+rate;
                    int earned=(int)Math.floor(credit);trailCredits.put(p.getUuid(),credit-earned);
                    int minimum=heart?0:config.trailMinimumPerTick*pref.density*trailMultiplier(trail.style());
                    trail(p,anchor,p.getPos(),Math.max(earned,minimum));trailAnchors.put(p.getUuid(),p.getPos());
                }
                if(ticks%config.intervalTicks==0)shape(p,"aura",p.getPos());
                remember(p);
            } catch(RuntimeException e) {if(ticks>=nextErrorLog){error(null,e);nextErrorLog=ticks+1200;}remember(p);}
        }
        rotation=(rotation+1)%players.size();
    }
    public boolean celebrate(ServerPlayerEntity p,String category) {
        if(!Set.of("raid","victory").contains(category)||!visible(p)||equipped(p,category)==null)return false;
        long next=lastCelebration.getOrDefault(p.getUuid(),0L);if(ticks<next)return false;
        lastCelebration.put(p.getUuid(),ticks+config.celebrationCooldownSeconds*20L);
        playerBudget=config.maxParticlesPerPlayer;
        // Event calls share the tick budget; they never grant items or rewards.
        burst(p,category,p.getPos());return true;
    }
    private void burst(ServerPlayerEntity p,String category,Vec3d origin) {
        var e=equipped(p,category);if(e==null)return;
        var pref=preferences(p);boolean celebration=category.equals("victory")||category.equals("raid");double scale=bodyScale(p);
        int count=pref.density*12;double radius=(celebration?2+pref.size*.8:.4+pref.size*.2)*scale;
        if(celebration)for(var viewer:p.getServerWorld().getPlayers()){
            var state=selected.get(viewer.getUuid());
            if(state!=null&&!state.cosmeticsHidden&&com.shirokai.core.visibility.VisibilityService.canSee(server,viewer,p)&&viewer.squaredDistanceTo(p)<=config.viewDistance*config.viewDistance)
                viewer.playSoundToPlayer(net.minecraft.sound.SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,net.minecraft.sound.SoundCategory.PLAYERS,.8f,1f);
        }
        for(int i=0;i<count;i++){double a=i*Math.PI*2/count;emit(p,e.style(),origin.add(Math.cos(a)*radius,(.15+i*((celebration?3.5:1.45)/count))*scale,Math.sin(a)*radius));}
    }
    private void shape(ServerPlayerEntity p,String category,Vec3d origin) {
        var e=equipped(p,category);if(e==null)return;
        var pref=preferences(p);double scale=bodyScale(p),phase=ticks*(.04*pref.speed)*pref.direction,radius=(.25+pref.size*.2)*scale;
        switch(category) {
            case "trail" -> {
                double yaw=Math.toRadians(p.getBodyYaw());int count=Math.min(config.maxParticlesPerPlayer,config.trailBaseDensity*pref.density*trailMultiplier(e.style()));trail(p,origin.add(Math.sin(yaw)*-.6*scale,0,Math.cos(yaw)*.6*scale),origin,count);
            }
            case "aura" -> {int count=pref.density*2;for(int i=0;i<count;i++){double progress=i/(double)Math.max(1,count-1),a=phase+progress*Math.PI*4*pref.direction;emit(p,e.style(),origin.add(Math.cos(a)*radius,(2-progress*1.9)*scale,Math.sin(a)*radius));}}
        }
    }
    private void resetTrail(ServerPlayerEntity p) {trailAnchors.put(p.getUuid(),p.getPos());trailCredits.put(p.getUuid(),0d);}
    private void trail(ServerPlayerEntity p,Vec3d from,Vec3d to,int planned) {
        var e=equipped(p,"trail");if(e==null)return;var pref=preferences(p);
        Vec3d delta=to.subtract(from);double horizontal=Math.sqrt(delta.x*delta.x+delta.z*delta.z);if(horizontal<.001)return;
        int count=Math.min(config.maxParticlesPerPlayer,e.style().equals("heart")?planned:Math.max(planned,(int)Math.ceil(horizontal/.12)));
        double rightX=-delta.z/horizontal,rightZ=delta.x/horizontal;
        double scale=bodyScale(p),lead=Math.min(.12*scale,horizontal*.75);Vec3d projected=to.add(delta.x/horizontal*lead,0,delta.z/horizontal*lead);
        for(int i=0;i<count;i++){
            boolean endpoint=i>=count-2;double side=endpoint?0:(i%2==0?-1:1)*.11*pref.size*scale;
            Vec3d center=count==1||i==count-2?to:i==count-1?projected:from.add(delta.multiply((i+1d)/(count-1d)));
            emit(p,e.style(),center.add(rightX*side,.035*scale,rightZ*side));
        }
    }
    static double bodyScale(ServerPlayerEntity player) {return Math.max(.0625,player.getScale());}
    private CosmeticState.Preferences preferences(ServerPlayerEntity p) {
        var state=selected.get(p.getUuid());return bounded(state==null||state.cosmeticPreferences==null?new CosmeticState.Preferences():state.cosmeticPreferences);
    }
    private CosmeticState.Preferences bounded(CosmeticState.Preferences value) {
        var safe=new CosmeticState.Preferences();safe.speed=Math.max(1,Math.min(config.maxSpeed,value.speed));safe.size=Math.max(1,Math.min(config.maxSize,value.size));safe.density=Math.max(1,Math.min(config.maxDensity,value.density));safe.direction=value.direction<0?-1:1;return safe;
    }
    private static SimpleParticleType particle(String style) {
        return switch(style) {case "fire"->ParticleTypes.FLAME;case "water"->ParticleTypes.SPLASH;case "nature"->ParticleTypes.HAPPY_VILLAGER;case "electric"->ParticleTypes.ELECTRIC_SPARK;case "soul"->ParticleTypes.SOUL;case "snow"->ParticleTypes.SNOWFLAKE;case "heart"->ParticleTypes.HEART;case "magic"->ParticleTypes.WITCH;case "enchant"->ParticleTypes.ENCHANT;case "cloud"->ParticleTypes.CLOUD;case "cherry"->ParticleTypes.CHERRY_LEAVES;default->ParticleTypes.END_ROD;};
    }
    private static int trailMultiplier(String style) {return switch(style){case "water"->3;case "electric","soul","snow","magic","enchant"->2;default->1;};}
    private void emit(ServerPlayerEntity p,String style,Vec3d pos) {
        if(globalBudget<=0||playerBudget<=0)return;
        globalBudget--;playerBudget--;
        for(var viewer:p.getServerWorld().getPlayers()) {
            var state=selected.get(viewer.getUuid());
            if(state==null||state.cosmeticsHidden||!com.shirokai.core.visibility.VisibilityService.canSee(server,viewer,p))continue; // personal opt-out also hides other players' effects
            if(viewer.getPos().squaredDistanceTo(pos)<=config.viewDistance*config.viewDistance)
                p.getServerWorld().spawnParticles(viewer,particle(style),false,pos.x,pos.y,pos.z,1,0,0,0,0);
        }
    }
    private static final Formatting[] RAINBOW={Formatting.AQUA,Formatting.BLUE,Formatting.LIGHT_PURPLE,Formatting.RED,Formatting.GOLD,Formatting.YELLOW,Formatting.GREEN};
    public static CosmeticService active(MinecraftServer server){return ACTIVE.get(server);}
    public static Formatting color(ServerPlayerEntity p){
        var service=ACTIVE.get(p.getServer());if(service==null)return Formatting.WHITE;
        var entry=service.equipped(p,"nick");if(entry==null)return Formatting.WHITE;
        return switch(entry.style()){case "gold"->Formatting.GOLD;case "rainbow"->RAINBOW[(int)((service.ticks/10)%RAINBOW.length)];default->Formatting.AQUA;};
    }
    public static Formatting teamColor(ServerPlayerEntity p){var color=color(p);return color==Formatting.WHITE?Formatting.RESET:color;}
    public static Text name(ServerPlayerEntity p){return Text.literal(com.shirokai.core.integrations.EssentialsAccess.display(p)).formatted(Formatting.WHITE);}
}
