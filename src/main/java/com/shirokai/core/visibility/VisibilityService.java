package com.shirokai.core.visibility;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;
import java.lang.reflect.Method;
import org.slf4j.Logger;

/** Optional audited Drex Vanish API. Entity tracking remains owned by Vanish itself. */
public final class VisibilityService {
    private static final Map<MinecraftServer,java.lang.ref.WeakReference<VisibilityService>> ACTIVE=new WeakHashMap<>();
    private final MinecraftServer server;private final Logger logger;
    private Method canSee,hidden;private boolean present,failed;private String status="ABSENT";
    public VisibilityService(MinecraftServer server,Logger logger){
        this.server=server;this.logger=logger;var mod=FabricLoader.getInstance().getModContainer("vanish").or(()->FabricLoader.getInstance().getModContainer("melius-vanish"));
        if(mod.isPresent()){
            present=true;String version=mod.get().getMetadata().getVersion().getFriendlyString();status="UNSUPPORTED "+version;
            if(version.equals("1.5.9+1.21.1"))try{
                var api=Class.forName("me.drex.vanish.api.VanishAPI");
                canSee=api.getMethod("canSeePlayer",ServerPlayerEntity.class,ServerPlayerEntity.class);
                hidden=api.getMethod("isVanished",Entity.class);status="READY "+version;
            }catch(ReflectiveOperationException|LinkageError error){failed=true;status="FAILED";logger.error("[vanish] API indisponível",error);}
        }
        ACTIVE.put(server,new java.lang.ref.WeakReference<>(this));logger.info("[vanish] {}",status);
    }
    public String status(){return status;}
    public boolean vanished(ServerPlayerEntity actor){
        if(!present)return false;if(hidden==null||failed)return true;
        try{return (boolean)hidden.invoke(null,actor);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){fail(error);return true;}
    }
    private boolean visible(ServerPlayerEntity viewer,ServerPlayerEntity actor){
        if(viewer==actor)return true;if(!present)return true;if(canSee==null||failed)return false;
        try{return (boolean)canSee.invoke(null,actor,viewer);}catch(ReflectiveOperationException|RuntimeException|LinkageError error){fail(error);return false;}
    }
    private void fail(Throwable error){if(!failed)logger.error("[vanish] Falha de visibilidade; não revelar jogadores",error);failed=true;status="FAILED";}
    public static boolean canSee(MinecraftServer server,ServerPlayerEntity viewer,ServerPlayerEntity actor){var policy=policy(server);return policy==null||policy.visible(viewer,actor);}
    public static boolean canSee(MinecraftServer server,UUID viewer,UUID actor){
        var policy=policy(server);if(policy==null||!policy.present)return true;
        if(Objects.equals(viewer,actor))return true;var v=server.getPlayerManager().getPlayer(viewer);var a=server.getPlayerManager().getPlayer(actor);
        return v!=null&&a!=null&&policy.visible(v,a);
    }
    public static boolean active(MinecraftServer server){var policy=policy(server);return policy!=null&&policy.present;}
    public static boolean isHidden(MinecraftServer server,ServerPlayerEntity actor){var policy=policy(server);return policy!=null&&policy.vanished(actor);}
    public static int count(MinecraftServer server,ServerPlayerEntity viewer,Collection<ServerPlayerEntity> players){return (int)players.stream().filter(actor->canSee(server,viewer,actor)).count();}
    private static VisibilityService policy(MinecraftServer server){var ref=ACTIVE.get(server);return ref==null?null:ref.get();}
    public void close(){var ref=ACTIVE.get(server);if(ref!=null&&ref.get()==this)ACTIVE.remove(server);}
}
