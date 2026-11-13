package com.shirokai.core.integrations;

import com.shirokai.core.CoreRuntime;
import com.shirokai.core.chat.Channel;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;

/** Optional Essentials protocol 1. No API dependency or disk access on render. */
public final class EssentialsAccess implements AutoCloseable {
    private static final Map<MinecraftServer,EssentialsAccess> ACTIVE=new WeakHashMap<>();
    public static final List<String> KEYS=List.of("nickname","displayname","realname","afk","afk_suffix","fly","god","homes","home_limit","mail","teleport_seconds","kits_ready","kit_seconds","kit_pending");
    private final MinecraftServer server;private final CoreRuntime core;
    private Class<?> api;private Method snapshot,rejection,accepts,activity,delivered,detach,ignored,unignore;
    private final Map<UUID,Map<String,String>> cache=new HashMap<>();private long cacheTick=-1;
    private boolean present,attached;private String status="ABSENT";
    public EssentialsAccess(MinecraftServer server,CoreRuntime core){
        this.server=server;this.core=core;ACTIVE.put(server,this);
        present=FabricLoader.getInstance().isModLoaded("shirokai_essentials");if(!present)return;
        try{
            api=Class.forName("dev.shirokai.essentials.api.EssentialsBridge");
            if(!Integer.valueOf(1).equals(api.getMethod("protocol").invoke(null)))throw new NoSuchMethodException("Unsupported protocol");
            if(!Boolean.TRUE.equals(api.getMethod("enabled").invoke(null))){present=false;status="DISABLED by Essentials";return;}
            ignored=api.getMethod("ignored",UUID.class,UUID.class);unignore=api.getMethod("unignore",UUID.class,UUID.class);
            snapshot=api.getMethod("snapshot",UUID.class);rejection=api.getMethod("rejection",UUID.class);
            accepts=api.getMethod("accepts",UUID.class,UUID.class,boolean.class);activity=api.getMethod("activity",UUID.class);
            delivered=api.getMethod("delivered",UUID.class,UUID.class,String.class);detach=api.getMethod("detach",Object.class);
            status="READY protocol=1";
        }catch(ReflectiveOperationException|LinkageError error){status="UNSUPPORTED; update Essentials to 0.3.0+";snapshot=null;}
    }
    public String status(){return status;}
    private Object invoke(Method method,Object...args){
        if(!server.isOnThread())throw new IllegalStateException("Essentials bridge requires server thread");
        if(method==null)throw new IllegalStateException(status);
        try{return method.invoke(null,args);}catch(ReflectiveOperationException|LinkageError error){throw new IllegalStateException("Essentials bridge unavailable",error);}
    }
    public void connect(){
        if(snapshot==null||!core.running("chat"))return;
        try{
            BiFunction<UUID,Map<String,String>,String> router=(id,request)->{
                if(!server.isOnThread()||!core.running("chat"))return "Chat Core indisponível";
                try{
                    var service=core.chat().service();String action=request.get("action");
                    if("visible".equals(action))return Boolean.toString(com.shirokai.core.visibility.VisibilityService.canSee(server,id,UUID.fromString(request.get("target"))));
                    if("message".equals(action))service.privateMessage(id,UUID.fromString(request.get("target")),request.get("text"),core.chat()::system);
                    else if("reply".equals(action))service.privateMessage(id,service.replyTarget(id),request.get("text"),core.chat()::system);
                    else if("unignore".equals(action)){
                        service.unignore(id,UUID.fromString(request.get("target"))).whenCompleteAsync((v,error)->{var p=server.getPlayerManager().getPlayer(id);if(error!=null&&p!=null)p.sendMessage(net.minecraft.text.Text.literal("Não foi possível remover ignore do Core"),false);},server);
                    }else return "Operação Core desconhecida";
                    return "";
                }catch(RuntimeException error){return error.getMessage()==null?"Chat Core indisponível":error.getMessage();}
            };
            api.getMethod("attach",Object.class,BiFunction.class).invoke(null,this,router);attached=true;
        }catch(ReflectiveOperationException|LinkageError error){status="FAILED attach";}
    }
    @SuppressWarnings("unchecked") private Map<String,String> values(UUID id){
        if(!present||snapshot==null)return Map.of();
        if(cacheTick!=server.getTicks()){cache.clear();cacheTick=server.getTicks();}
        return cache.computeIfAbsent(id,key->{try{return Map.copyOf((Map<String,String>)invoke(snapshot,key));}catch(RuntimeException error){return Map.of();}});
    }
    public Optional<String> value(UUID id,String key){return Optional.ofNullable(values(id).get(key));}
    public static String display(ServerPlayerEntity player){var bridge=ACTIVE.get(player.getServer());return bridge==null?player.getGameProfile().getName():bridge.value(player.getUuid(),"displayname").orElse(player.getGameProfile().getName());}
    public static String afkSuffix(ServerPlayerEntity player){var bridge=ACTIVE.get(player.getServer());return bridge==null?"":bridge.value(player.getUuid(),"afk_suffix").orElse("");}
    public void check(UUID id){if(!present)return;String error=(String)invoke(rejection,id);if(!error.isEmpty())throw new IllegalStateException(error);}
    public boolean accepts(UUID receiver,UUID sender,boolean privateMessage){return !present||Boolean.TRUE.equals(invoke(accepts,receiver,sender,privateMessage));}
    public boolean ignored(UUID player,UUID target){return present&&Boolean.TRUE.equals(invoke(ignored,player,target));}
    public void unignore(UUID player,UUID target){if(present)invoke(unignore,player,target);}
    public void activity(UUID id){if(present)invoke(activity,id);}
    public void delivered(UUID from,UUID to,String text){if(present)invoke(delivered,from,to,text);}
    @Override public void close(){if(attached)try{invoke(detach,this);}catch(RuntimeException ignored){}ACTIVE.remove(server,this);cache.clear();}
}
