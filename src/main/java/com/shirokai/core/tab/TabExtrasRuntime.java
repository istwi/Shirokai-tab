package com.shirokai.core.tab;

import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.scoreboard.SidebarPackets;
import net.minecraft.entity.boss.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.scoreboard.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import java.util.*;
import java.util.function.*;

/** All game/network access on the server thread; no database reads. */
public final class TabExtrasRuntime implements AutoCloseable {
    @FunctionalInterface public interface Resolver { Text resolve(String key,ServerPlayerEntity subject,ServerPlayerEntity viewer); }
    private record Listing(boolean baseline,boolean sent,long revision){}
    private static final class View {
        final ServerPlayerEntity player;
        final Map<UUID,Listing> listings=new HashMap<>();final Set<UUID> relinquishedListings=new HashSet<>();
        ServerBossBar boss;boolean bossLost,belowLost;
        ScoreboardObjective below;Text label;Map<String,Integer> scores=Map.of();
        long belowRevision,resetRevision;
        View(ServerPlayerEntity player){this.player=player;}
    }
    private final MinecraftServer server;private final PermissionService permissions;private final Resolver resolver;
    private final Predicate<UUID> animations;private final Logger logger;
    private final Map<UUID,View> views=new HashMap<>();
    private Map<UUID,ServerPlayerEntity> subjects=Map.of();
    private TabExtrasPlan plan;private long tick,packets,conflicts,errors,lastErrorTick=-200;private boolean closed;
    public TabExtrasRuntime(MinecraftServer server,TabExtrasPlan plan,PermissionService permissions,Resolver resolver,Predicate<UUID> animations,Logger logger){
        this.server=server;this.plan=plan;this.permissions=permissions;this.resolver=resolver;this.animations=animations;this.logger=logger;
    }
    public TabExtrasPlan plan(){return plan;}
    public void replacePlan(TabExtrasPlan candidate){
        if(candidate.config().requiresRestartComparedTo(plan.config()))throw new IllegalArgumentException("Ownership de extras do TAB exige reinício");
        plan=candidate;
    }
    private Text render(com.shirokai.core.styles.StyleEngine.Compiled template,ServerPlayerEntity subject,ServerPlayerEntity viewer,int max){
        Text result=plan.styles().renderRich(template,key->resolver.resolve(key,subject,viewer),tick,animations.test(subject.getUuid())&&animations.test(viewer.getUuid()));
        if(result.getString().codePointCount(0,result.getString().length())>max)throw new IllegalArgumentException("Texto de extras excede"+max);
        return result;
    }
    public TeamOwnership.Player nametag(ServerPlayerEntity subject,int priority,long tick){
        this.tick=tick;
        Text prefix=render(plan.prefix(),subject,subject,128),suffix=render(plan.suffix(),subject,subject,128);
        return new TeamOwnership.Player(subject.getUuid(),subject.getGameProfile().getName(),priority,prefix,suffix,AbstractTeam.VisibilityRule.valueOf(plan.config().nametags().visibility()));
    }
    private void send(View view,Packet<?> packet){TabExtrasPackets.send(view.player.networkHandler,packet);packets++;}
    public void update(List<ServerPlayerEntity> players,long tick){
        if(closed)return;if(!server.isOnThread())throw new IllegalStateException("Extras exigem thread do servidor");this.tick=tick;subjects=players.stream().collect(java.util.stream.Collectors.toMap(ServerPlayerEntity::getUuid,p->p));
        var active=new HashSet<UUID>();players.forEach(p->active.add(p.getUuid()));
        for(UUID id:List.copyOf(views.keySet()))if(!active.contains(id))forget(id);
        for(var player:players){
            var view=views.get(player.getUuid());
            if(view!=null&&view.player!=player){forget(player.getUuid());view=null;}
            if(view==null){view=new View(player);views.put(player.getUuid(),view);if(plan.config().belowName().enabled())SidebarPackets.retain(player.networkHandler);}
            View current=view;
            if(plan.config().listed().enabled())attempt(()->listed(current,players));
            if(plan.config().belowName().enabled())attempt(()->below(current,players));
            if(plan.config().bossbar().enabled())attempt(()->boss(current));
        }
    }
    private void attempt(Runnable operation){try{operation.run();}catch(RuntimeException error){errors++;if(tick-lastErrorTick>=200){lastErrorTick=tick;logger.warn("[tab-extras] Último frame válido preservado",error);}}}
    private static PlayerListS2CPacket.Entry entry(ServerPlayerEntity subject,boolean listed){
        var base=new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_LISTED,subject).getEntries().getFirst();
        return new PlayerListS2CPacket.Entry(base.profileId(),base.profile(),listed,base.latency(),base.gameMode(),base.displayName(),base.chatSession());
    }
    private void listed(View view,List<ServerPlayerEntity> players){
        var observed=TabExtrasPackets.state(view.player.networkHandler);
        if(observed.overflow()){view.relinquishedListings.addAll(view.listings.keySet());view.listings.clear();return;}
        var active=new HashSet<UUID>();var changes=new ArrayList<PlayerListS2CPacket.Entry>();
        for(var subject:players){
            UUID id=subject.getUuid();active.add(id);var state=observed.listed().get(id);var old=view.listings.get(id);
            if(old!=null&&(state==null||state.revision()!=old.revision()||state.removed())){
                view.listings.remove(id);view.relinquishedListings.add(id);conflicts++;continue;
            }
            if(view.relinquishedListings.contains(id)||state==null||state.removed())continue;
            boolean baseline=old==null?state.value():old.baseline();
            var config=plan.config().listed();
            boolean hidden=!id.equals(view.player.getUuid())&&permissions.allowed(id,config.hiddenPermission())&&!permissions.allowed(view.player.getUuid(),config.seeHiddenPermission());
            boolean desired=baseline&&!hidden&&(!config.sameWorldOnly()||subject.getWorld()==view.player.getWorld());
            boolean previous=old==null?baseline:old.sent();
            if(desired!=previous)changes.add(entry(subject,desired));
            if(desired!=baseline||old!=null)view.listings.put(id,new Listing(baseline,desired,state.revision()));
        }
        view.listings.keySet().retainAll(active);
        if(!changes.isEmpty())send(view,TabExtrasPackets.listed(changes));
    }
    private void releaseBelow(View view,SidebarPackets.State foreign){
        if(view.below==null)return;
        if(!foreign.overflow()&&!foreign.names().contains(view.below.getName())&&server.getScoreboard().getNullableObjective(view.below.getName())==null)
            send(view,new ScoreboardObjectiveUpdateS2CPacket(view.below,ScoreboardObjectiveUpdateS2CPacket.REMOVE_MODE));
        view.below=null;view.label=null;view.scores=Map.of();
    }
    private void below(View view,List<ServerPlayerEntity> players){
        var foreign=SidebarPackets.state(view.player.networkHandler);var worldBoard=server.getScoreboard();
        if(view.below!=null&&(foreign.names().contains(view.below.getName())||worldBoard.getNullableObjective(view.below.getName())!=null)){
            view.belowLost=true;view.below=null;view.scores=Map.of();conflicts++;
        }
        if(foreign.overflow())view.belowLost=true;
        boolean occupied=foreign.slots().containsKey(ScoreboardDisplaySlot.BELOW_NAME)||worldBoard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME)!=null;
        if(view.belowLost||occupied){if(occupied&&view.below!=null)conflicts++;releaseBelow(view,foreign);return;}
        if(view.belowRevision!=foreign.belowRevision()){releaseBelow(view,foreign);view.belowRevision=foreign.belowRevision();}
        Text label=render(plan.label(),view.player,view.player,128);
        var scores=new LinkedHashMap<String,Integer>();
        var roster=TabExtrasPackets.state(view.player.networkHandler).listed();
        for(var subject:players){
            var listing=roster.get(subject.getUuid());var own=view.listings.get(subject.getUuid());
            // Do not reintroduce a player removed from the viewer's roster by another mod.
            if(listing!=null&&(listing.removed()||!listing.value())||own!=null&&!own.sent())continue;
            scores.put(subject.getGameProfile().getName(),TabExtrasPlan.score(render(plan.value(),subject,view.player,32).getString()));
        }
        if(view.below==null){
            String name;do{name="sk"+UUID.randomUUID().toString().replace("-","").substring(0,14);}while(foreign.names().contains(name)||worldBoard.getNullableObjective(name)!=null);
            view.below=new ScoreboardObjective(new Scoreboard(),name,ScoreboardCriterion.DUMMY,label,ScoreboardCriterion.RenderType.INTEGER,false,null);
            send(view,new ScoreboardObjectiveUpdateS2CPacket(view.below,ScoreboardObjectiveUpdateS2CPacket.ADD_MODE));
            send(view,new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.BELOW_NAME,view.below));view.label=label;
        }else if(!Objects.equals(view.label,label)){
            view.below.setDisplayName(label);send(view,new ScoreboardObjectiveUpdateS2CPacket(view.below,ScoreboardObjectiveUpdateS2CPacket.UPDATE_MODE));view.label=label;
        }
        for(String name:view.scores.keySet())if(!scores.containsKey(name))send(view,new ScoreboardScoreResetS2CPacket(name,view.below.getName()));
        for(var row:scores.entrySet())if(view.resetRevision!=foreign.resetRevision()||!Objects.equals(view.scores.get(row.getKey()),row.getValue()))
            send(view,new ScoreboardScoreUpdateS2CPacket(row.getKey(),view.below.getName(),row.getValue(),Optional.empty(),Optional.empty()));
        view.scores=Map.copyOf(scores);view.resetRevision=foreign.resetRevision();
    }
    private void releaseBoss(View view){
        if(view.boss==null)return;
        var foreign=TabExtrasPackets.state(view.player.networkHandler);
        if(!foreign.overflow()&&!foreign.bossIds().contains(view.boss.getUuid()))send(view,BossBarS2CPacket.remove(view.boss.getUuid()));
        view.boss=null;
    }
    private void boss(View view){
        var foreign=TabExtrasPackets.state(view.player.networkHandler);var config=plan.config().bossbar();
        if(foreign.overflow()||view.boss!=null&&foreign.bossIds().contains(view.boss.getUuid())){
            if(!view.bossLost)conflicts++;view.bossLost=true;view.boss=null;
        }
        boolean matches=TabPlan.matches(config.condition(),permissions.metadata(view.player.getUuid()),view.player.getWorld().getRegistryKey().getValue().toString(),node->permissions.allowed(view.player.getUuid(),node));
        if(view.bossLost||!matches){releaseBoss(view);return;}
        Text title=render(plan.title(),view.player,view.player,256);
        float progress=TabExtrasPlan.progress(render(plan.progress(),view.player,view.player,32).getString());
        var color=BossBar.Color.valueOf(config.color());var style=BossBar.Style.valueOf(config.style());
        if(view.boss==null){
            do{view.boss=new ServerBossBar(title,color,style);}while(foreign.bossIds().contains(view.boss.getUuid()));
            view.boss.setPercent(progress);send(view,BossBarS2CPacket.add(view.boss));
        }else{
            if(!Objects.equals(view.boss.getName(),title)){view.boss.setName(title);send(view,BossBarS2CPacket.updateName(view.boss));}
            if(view.boss.getPercent()!=progress){view.boss.setPercent(progress);send(view,BossBarS2CPacket.updateProgress(view.boss));}
            if(view.boss.getColor()!=color||view.boss.getStyle()!=style){view.boss.setColor(color);view.boss.setStyle(style);send(view,BossBarS2CPacket.updateStyle(view.boss));}
        }
    }
    public void forget(UUID id){
        var view=views.remove(id);if(view==null)return;
        if(!view.player.isDisconnected()){
            var observed=TabExtrasPackets.state(view.player.networkHandler);var restore=new ArrayList<PlayerListS2CPacket.Entry>();
            if(!observed.overflow())for(var row:view.listings.entrySet()){
                var state=observed.listed().get(row.getKey());var value=row.getValue();
                if(state!=null&&!state.removed()&&state.revision()==value.revision()&&value.sent()!=value.baseline()){
                    var player=subjects.get(row.getKey());if(player!=null&&!player.isDisconnected())restore.add(entry(player,value.baseline()));
                }
            }
            if(!restore.isEmpty())send(view,TabExtrasPackets.listed(restore));
            releaseBelow(view,SidebarPackets.state(view.player.networkHandler));releaseBoss(view);
        }
        if(plan.config().belowName().enabled())SidebarPackets.release(view.player.networkHandler);
        if(view.player.isDisconnected())TabExtrasPackets.forget(view.player.networkHandler);
    }
    public String status(){return "extrasViewers="+views.size()+" extraPackets="+packets+" extraConflicts="+conflicts+" extraErrors="+errors;}
    @Override public void close(){if(closed)return;closed=true;for(UUID id:List.copyOf(views.keySet()))attempt(()->forget(id));subjects=Map.of();}
}
