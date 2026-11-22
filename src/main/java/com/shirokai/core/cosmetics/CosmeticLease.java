package com.shirokai.core.cosmetics;

import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;

/** Server-thread-only handoff. Old Essentials cannot run duplicate effects silently. */
final class CosmeticLease implements AutoCloseable {
    private final Object token=new Object();private Method detach,allowed;
    void attach()throws ReflectiveOperationException{
        if(!FabricLoader.getInstance().isModLoaded("shirokai_essentials"))return;
        var api=Class.forName("dev.shirokai.essentials.api.EssentialsBridge");
        if(!api.getMethod("cosmeticsProtocol").invoke(null).equals(1))throw new IllegalStateException("Atualize o Essentials do pacote de migração");
        detach=api.getMethod("detachCosmetics",Object.class);allowed=api.getMethod("cosmeticsAllowed",UUID.class,boolean.class);
        api.getMethod("attachCosmetics",Object.class).invoke(null,token);
    }
    boolean allowed(UUID id,boolean battle){if(allowed==null)return true;try{return (boolean)allowed.invoke(null,id,battle);}catch(ReflectiveOperationException|RuntimeException e){return false;}}
    @Override public void close(){if(detach!=null)try{detach.invoke(null,token);}catch(ReflectiveOperationException ignored){}}
}
