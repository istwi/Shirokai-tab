package com.shirokai.core.tab;

import com.shirokai.core.clans.ClanService;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.placeholders.PlaceholderService;
import com.shirokai.core.storage.SqliteStorage;
import com.shirokai.core.titles.TitleService;
import com.shirokai.core.styles.StyleEngine;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** World/network APIs and caches are server-thread confined. No SQLite in update(). */
public final class TabRuntime implements AutoCloseable{
    private static final class View{Text header,footer;final Map<UUID,Text> names=new HashMap<>();}
    private final MinecraftServer server;private final PermissionService permissions;private final PlaceholderService placeholders;private final TitleService titles;private final ClanService clans;private final SqliteStorage storage;
    private final Supplier<Collection<ServerPlayerEntity>> online;private final Supplier<String> fallback;private final Logger logger;private final TeamOwnership teams;
    private final Map<UUID,View> views=new HashMap<>();private final Map<UUID,Boolean> animations=new HashMap<>();private final Map<UUID,Long> errors=new HashMap<>();
    private TabPlan plan;private long tick,headerPackets,namePackets,nameEntries,failedUpdates;private Long globalErrorTick;private boolean closed;
    public TabRuntime(MinecraftServer server,TabPlan plan,PermissionService permissions,PlaceholderService placeholders,TitleService titles,ClanService clans,SqliteStorage storage,Supplier<String> fallback,Logger logger){this(server,plan,permissions,placeholders,titles,clans,storage,fallback,logger,()->List.copyOf(server.getPlayerManager().getPlayerList()));}
    public TabRuntime(MinecraftServer server,TabPlan plan,PermissionService permissions,PlaceholderService placeholders,TitleService titles,ClanService clans,SqliteStorage storage,Supplier<String> fallback,Logger logger,Supplier<Collection<ServerPlayerEntity>> online){
        this.server=server;this.plan=plan;this.permissions=permissions;this.placeholders=placeholders;this.titles=titles;this.clans=clans;this.storage=storage;this.fallback=fallback;this.logger=logger;this.online=online;teams=new TeamOwnership(server.getScoreboard(),message->logger.warn("[tab] Ordenação ignorada: {}",message));
    }
    public TabPlan plan(){return plan;}
    public boolean animationsEnabled(UUID id){return animations.getOrDefault(id,false);}
    public void replacePlan(TabPlan candidate){if(candidate.config().requiresRestartComparedTo(plan.config()))throw new IllegalArgumentException("Ownership do TAB exige reinício");plan=candidate;update();}
    private ServerPlayerEntity player(UUID id){return online.get().stream().filter(p->p.getUuid().equals(id)&&!p.isDisconnected()).findFirst().orElse(null);}
    public void join(UUID id){
        animations.put(id,false);if(storage!=null)storage.preferences(id).whenCompleteAsync((prefs,error)->{
            if(error!=null){logger.warn("[tab] Preferência indisponível "+id,error);return;}if(!closed&&player(id)!=null){animations.put(id,prefs.animationsEnabled());update();}
        },server);
    }
    public void forget(UUID id){views.remove(id);animations.remove(id);errors.remove(id);}
    public CompletableFuture<Boolean> toggleAnimations(UUID id){
        if(storage==null)return CompletableFuture.failedFuture(new IllegalStateException("Persistência de preferências indisponível"));boolean enabled=!animations.getOrDefault(id,false);
        return storage.setAnimations(id,enabled).thenApplyAsync(v->{if(!closed&&player(id)!=null){animations.put(id,enabled);update();}return enabled;},server);
    }
    public void tick(){if(!closed&&++tick%plan.config().updateTicks()==0)update();}
    public void refresh(UUID id){views.remove(id);update();}
    private String resolve(String key,ServerPlayerEntity subject,ServerPlayerEntity viewer,PermissionService.Metadata metadata,int count){
        String title=titles==null?"":titles.placeholder(subject.getUuid(),"shirokai:selected_title").orElse("");
        return switch(key){
            case "shirokai:player"->subject.getGameProfile().getName();case "shirokai:uuid"->subject.getUuidAsString();case "shirokai:viewer"->viewer.getGameProfile().getName();
            case "shirokai:online"->Integer.toString(count);case "shirokai:world"->subject.getWorld().getRegistryKey().getValue().toString();case "shirokai:ping"->Integer.toString(subject.networkHandler.getLatency());
            case "shirokai:selected_title"->title;case "shirokai:title_prefix"->title.isEmpty()?"":title+" ";
            case "shirokai:clan"->clans==null?fallback.get():clans.cached(subject.getUuid()).map(c->c.name()).orElse(fallback.get());
            case "shirokai:clan_tag"->clans==null?fallback.get():clans.cached(subject.getUuid()).map(c->c.tag()).orElse(fallback.get());
            case "shirokai:group"->Objects.requireNonNullElse(metadata.group(),fallback.get());case "luckperms:prefix"->metadata.prefix();case "luckperms:suffix"->metadata.suffix();
            default->placeholders==null?fallback.get():placeholders.render("%"+key+"%",new PlaceholderService.Context(subject.getUuid(),subject.getGameProfile().getName(),count),fallback.get());
        };
    }
    private Text resolveRich(String key,ServerPlayerEntity subject,ServerPlayerEntity viewer,PermissionService.Metadata metadata,int count){
        if(titles!=null&&(key.equals("shirokai:selected_title")||key.equals("shirokai:title_prefix"))){
            Text title=titles.visualPresentation(subject.getUuid());return key.equals("shirokai:title_prefix")&&!title.getString().isEmpty()?Text.empty().append(title).append(Text.literal(" ")):title;
        }
        return Text.literal(resolve(key,subject,viewer,metadata,count));
    }
    private Text lines(List<StyleEngine.Compiled> templates,Function<String,Text> resolver,boolean animate){
        var output=Text.empty();int length=0;for(int i=0;i<templates.size();i++){var line=plan.styles().renderRich(templates.get(i),resolver,tick,animate);length+=line.getString().codePointCount(0,line.getString().length());if(length>2048)throw new IllegalArgumentException("Header/footer excede2048 caracteres");if(i>0)output.append(Text.literal("\n"));output.append(line);}return output;
    }
    private static PlayerListS2CPacket.Entry entry(ServerPlayerEntity subject,Text name){
        var base=new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME,subject).getEntries().getFirst();
        return new PlayerListS2CPacket.Entry(base.profileId(),base.profile(),base.listed(),base.latency(),base.gameMode(),name,base.chatSession());
    }
    public void update(){
        if(closed)return;
        try{updateFrames();}catch(RuntimeException failure){failedUpdates++;if(globalErrorTick==null||tick-globalErrorTick>=200){globalErrorTick=tick;logger.warn("[tab] Falha isolada de atualização",failure);}}
    }
    private void updateFrames(){
        if(closed)return;var players=online.get().stream().filter(p->!p.isDisconnected()).toList();var active=new HashSet<UUID>();players.forEach(p->active.add(p.getUuid()));views.keySet().retainAll(active);animations.keySet().retainAll(active);errors.keySet().retainAll(active);
        var metadata=new HashMap<UUID,PermissionService.Metadata>();for(var player:players)metadata.put(player.getUuid(),permissions.metadata(player.getUuid()));
        var rules=new HashMap<UUID,Optional<TabPlan.Name>>();
        for(var subject:players)rules.put(subject.getUuid(),plan.name(metadata.get(subject.getUuid()),subject.getWorld().getRegistryKey().getValue().toString(),node->permissions.allowed(subject.getUuid(),node)));
        if(plan.config().manageTeams())teams.reconcile(players.stream().map(p->{int priority=rules.get(p.getUuid()).map(n->n.definition().priority()).orElse(metadata.get(p.getUuid()).weight());return new TeamOwnership.Player(p.getUuid(),p.getGameProfile().getName(),priority);}).toList());
        var values=new HashMap<UUID,Map<String,Text>>();var commonNames=new HashMap<UUID,Text[]>();
        for(var subject:players)values.put(subject.getUuid(),new HashMap<>());
        for(var viewer:players)try{
            var view=views.computeIfAbsent(viewer.getUuid(),id->new View());var meta=metadata.get(viewer.getUuid());boolean animate=animations.getOrDefault(viewer.getUuid(),false);
            var header=plan.header(meta,viewer.getWorld().getRegistryKey().getValue().toString(),node->permissions.allowed(viewer.getUuid(),node));Function<String,Text> resolver=key->key.equals("shirokai:viewer")?Text.literal(viewer.getGameProfile().getName()):values.get(viewer.getUuid()).computeIfAbsent(key,k->resolveRich(k,viewer,viewer,meta,players.size()));
            Text top=lines(header.header(),resolver,animate),bottom=lines(header.footer(),resolver,animate);
            var changed=new ArrayList<PlayerListS2CPacket.Entry>();var desired=new HashMap<UUID,Text>();
            if(plan.config().managePlayerNames())for(var subject:players){
                var foreignTeam=server.getScoreboard().getScoreHolderTeam(subject.getGameProfile().getName());boolean foreign=subject.getPlayerListName()!=null||foreignTeam!=null&&!teams.owns(foreignTeam);
                if(foreign){if(view.names.containsKey(subject.getUuid()))changed.add(entry(subject,subject.getPlayerListName()));continue;}
                var subjectMeta=metadata.get(subject.getUuid());var template=rules.get(subject.getUuid()).map(TabPlan.Name::template).orElse(plan.defaultName());
                boolean moving=animate&&animations.getOrDefault(subject.getUuid(),false);Text[] shared=commonNames.computeIfAbsent(subject.getUuid(),id->new Text[2]);int variant=moving?1:0;
                Function<String,Text> nameResolver=key->key.equals("shirokai:viewer")?Text.literal(viewer.getGameProfile().getName()):values.get(subject.getUuid()).computeIfAbsent(key,k->resolveRich(k,subject,viewer,subjectMeta,players.size()));
                Text name=template.variables().contains("shirokai:viewer")?plan.styles().renderRich(template,nameResolver,tick,moving):shared[variant];
                if(name==null){name=plan.styles().renderRich(template,nameResolver,tick,moving);shared[variant]=name;}
                if(name.getString().codePointCount(0,name.getString().length())>256)throw new IllegalArgumentException("Nome no TAB excede256 caracteres");desired.put(subject.getUuid(),name);
                if(!Objects.equals(view.names.get(subject.getUuid()),name))changed.add(entry(subject,name));
            }
            if(!Objects.equals(view.header,top)||!Objects.equals(view.footer,bottom)){viewer.networkHandler.sendPacket(new PlayerListHeaderS2CPacket(top,bottom));view.header=top;view.footer=bottom;headerPackets++;}
            if(!changed.isEmpty()){viewer.networkHandler.sendPacket(TabPackets.names(changed));namePackets++;nameEntries+=changed.size();}
            view.names.clear();view.names.putAll(desired);
        }catch(RuntimeException failure){failedUpdates++;Long previous=errors.get(viewer.getUuid());if(previous==null||tick-previous>=200){errors.put(viewer.getUuid(),tick);logger.warn("[tab] Atualização preservada para "+viewer.getUuid(),failure);}}
    }
    public String status(){return "viewers="+views.size()+" headers="+headerPackets+" namePackets="+namePackets+" nameEntries="+nameEntries+" teams="+teams.ownedTeams()+" conflicts="+teams.conflicts()+" errors="+failedUpdates;}
    @Override public void close(){
        if(closed)return;closed=true;try{for(var viewer:online.get()){
            var view=views.get(viewer.getUuid());if(view==null||viewer.isDisconnected())continue;
            viewer.networkHandler.sendPacket(new PlayerListHeaderS2CPacket(Text.empty(),Text.empty()));var reset=new ArrayList<PlayerListS2CPacket.Entry>();
            for(UUID id:view.names.keySet()){var subject=player(id);if(subject!=null)reset.add(entry(subject,subject.getPlayerListName()));}if(!reset.isEmpty())viewer.networkHandler.sendPacket(TabPackets.names(reset));
        }
        }finally{teams.close();views.clear();animations.clear();errors.clear();}
    }
}
