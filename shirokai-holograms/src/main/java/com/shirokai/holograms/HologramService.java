package com.shirokai.holograms;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;

/** All game and plan access stays on the server thread; the single IO executor owns filesystem work. */
public final class HologramService implements AutoCloseable {
    private final MinecraftServer server;private final ConfigStore store;private final Logger logger;
    private final Placeholders placeholders;
    private final ExecutorService io=Executors.newSingleThreadExecutor(r->new Thread(r,"shirokai-holograms-io"));
    private final Map<UUID,Viewer> viewers=new HashMap<>();
    private HologramPlan plan;private boolean busy,closed;private long tick,epoch;
    private static final class Visible {
        final List<DisplayLine> displays=new ArrayList<>();List<Text> text=List.of();double areaWidth;long refreshed=Long.MIN_VALUE;final Map<Integer,Long> frames=new HashMap<>();
    }
    private static final class Viewer {
        final ServerPlayerEntity player;final String dimension;final Map<String,Visible> visible=new HashMap<>();
        Viewer(ServerPlayerEntity p){player=p;dimension=p.getServerWorld().getRegistryKey().getValue().toString();}
        void clear(){visible.values().forEach(v->v.displays.forEach(DisplayLine::destroy));visible.clear();}
    }
    public HologramService(MinecraftServer server,ConfigStore store,Logger logger) {
        this.server=server;this.store=store;this.logger=logger;placeholders=new Placeholders(server,logger);
    }
    public HologramPlan plan(){if(plan==null)throw new IllegalStateException("Hologramas indisponíveis; confira o console e use /hologram reload");return plan;}
    public Definitions.Hologram get(String id){var h=plan().entries().get(id);if(h==null)throw new IllegalArgumentException("Holograma inexistente: "+id);return h.definition();}
    public CompletableFuture<Void> reload(){
        if(closed||busy)return CompletableFuture.failedFuture(new IllegalStateException("Operação em andamento; tente novamente"));
        busy=true;
        return finish(CompletableFuture.supplyAsync(()->{try{return new HologramPlan(store.load());}catch(Exception e){throw new CompletionException(e);}},io));
    }
    public CompletableFuture<Void> change(UnaryOperator<List<Definitions.Hologram>> edit) {
        if(closed||busy)return CompletableFuture.failedFuture(new IllegalStateException("Operação em andamento; tente novamente"));
        final HologramPlan candidate;
        try {
            var snapshot=plan().snapshot();var definitions=new Definitions.Holograms(1,edit.apply(new ArrayList<>(snapshot.holograms().holograms())));
            candidate=new HologramPlan(new ConfigStore.Snapshot(snapshot.settings(),snapshot.animations(),definitions));
        }catch(Exception e){return CompletableFuture.failedFuture(e);}
        busy=true;
        return finish(CompletableFuture.supplyAsync(()->{try{store.save(candidate.snapshot().holograms());return candidate;}catch(Exception e){throw new CompletionException(e);}},io));
    }
    private CompletableFuture<Void> finish(CompletableFuture<HologramPlan> work) {
        var result=new CompletableFuture<Void>();
        work.whenComplete((candidate,error)->server.execute(()->{
            busy=false;
            if(error!=null){logger.error("[holograms] Configuração não aplicada; estado ativo preservado",error);result.completeExceptionally(error);return;}
            if(closed){result.completeExceptionally(new IllegalStateException("Servidor encerrando; edição salva será lida no próximo boot"));return;}
            viewers.values().forEach(Viewer::clear);viewers.clear();plan=candidate;epoch=tick;
            for(var h:plan.entries().values()) {
                boolean exists=false;for(var world:server.getWorlds())if(world.getRegistryKey().getValue().toString().equals(h.definition().world()))exists=true;
                if(!exists)logger.warn("[holograms] {} aguarda dimensão indisponível {}",h.definition().id(),h.definition().world());
            }
            logger.info("[holograms] {} hologramas carregados",plan.entries().size());result.complete(null);
        }));return result;
    }
    public void tick(){tick++;if(plan==null||closed)return;var settings=plan.snapshot().settings();
        if(tick%settings.updateTicks()!=0)return;
        var connected=new HashSet<UUID>();
        for(var player:server.getPlayerManager().getPlayerList()) {connected.add(player.getUuid());update(player);}
        viewers.entrySet().removeIf(e->{if(connected.contains(e.getKey()))return false;e.getValue().clear();return true;});
    }
    public void update(ServerPlayerEntity player) {
        if(plan==null||closed)return;
        var settings=plan.snapshot().settings();var view=viewers.get(player.getUuid());
        String dimension=player.getServerWorld().getRegistryKey().getValue().toString();
        if(view==null||view.player!=player||!view.dimension.equals(dimension)) {if(view!=null)view.clear();view=new Viewer(player);viewers.put(player.getUuid(),view);}
        var wanted=new HashSet<String>();
        if(settings.enabled())for(var compiled:plan.entries().values()) {
            var h=compiled.definition();if(!h.world().equals(dimension)||player.getPos().squaredDistanceTo(new Vec3d(h.x(),h.y(),h.z()))>settings.viewDistance()*settings.viewDistance())continue;
            wanted.add(h.id());var visible=view.visible.computeIfAbsent(h.id(),k->new Visible());
            try{render(player,compiled,visible);}catch(RuntimeException e){visible.displays.forEach(DisplayLine::destroy);visible.displays.clear();logger.error("[holograms] Falha ao renderizar {}",h.id(),e);}
        }
        view.visible.entrySet().removeIf(e->{if(wanted.contains(e.getKey()))return false;e.getValue().displays.forEach(DisplayLine::destroy);return true;});
    }
    private void render(ServerPlayerEntity player,HologramPlan.Compiled compiled,Visible visible) {
        var settings=plan.snapshot().settings();var h=compiled.definition();boolean refresh=visible.text.isEmpty()||tick-visible.refreshed>=settings.placeholderTicks();
        if(refresh){visible.text=plan.render(compiled,key->placeholders.resolve(key,player,settings.unavailablePlaceholder()));visible.areaWidth=visible.text.stream().mapToDouble(FontWidths::width).max().orElse(0);visible.refreshed=tick;}
        boolean fresh=visible.displays.isEmpty();
        if(fresh)for(var line:h.lines())visible.displays.add(new DisplayLine(player,h));
        for(int i=0;i<h.lines().size();i++) {
            var line=h.lines().get(i);var motion=plan.snapshot().animations().presets().get(line.animation());
            long elapsed=tick-epoch;
            int interval=((motion.intervalTicks()+settings.updateTicks()-1)/settings.updateTicks())*settings.updateTicks();
            long frameNumber=elapsed/interval;
            boolean finished=motion.repetitions()>0 && elapsed>=(motion.durationTicks()/motion.speed()+motion.pauseTicks())*motion.repetitions();
            if(!fresh&&!refresh&&(motion.type()==Definitions.Animation.STATIC || Objects.equals(visible.frames.get(i),frameNumber) || finished&&visible.frames.getOrDefault(i,-1L)*interval>=(motion.durationTicks()/motion.speed()+motion.pauseTicks())*motion.repetitions()))continue;
            visible.frames.put(i,frameNumber);
            Text base=visible.text.get(i);double width=FontWidths.width(base);
            var frame=AnimationFrame.at(motion,elapsed,width);var text=frame.text(base);
            double aligned=FontWidths.offset(line.alignment()==null?h.alignment():line.alignment(),width,visible.areaWidth);
            // Keep the full-line anchor stable while typewriter reveals glyphs from the left.
            if(motion.type()==Definitions.Animation.TYPEWRITER)aligned+=(FontWidths.width(text)-width)/2;
            double duration=Math.max(1,motion.durationTicks()/motion.speed());double cycle=duration+motion.pauseTicks();
            boolean reset=elapsed%cycle<Math.max(settings.updateTicks(),motion.intervalTicks());
            visible.displays.get(i).update(text,aligned+frame.x(),-i*h.lineSpacing()+frame.y(),frame.opacity(),interval,reset);
        }
    }
    public void forget(UUID id){var view=viewers.remove(id);if(view!=null)view.clear();}
    @Override public void close(){closed=true;viewers.values().forEach(Viewer::clear);viewers.clear();io.shutdown();}
    public void awaitIo() throws InterruptedException {if(!io.awaitTermination(10,TimeUnit.SECONDS))logger.warn("[holograms] IO ainda encerrando");}
}
