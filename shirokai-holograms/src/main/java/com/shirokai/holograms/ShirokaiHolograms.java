package com.shirokai.holograms;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.*;

public final class ShirokaiHolograms implements ModInitializer {
    private static final Logger LOGGER=LoggerFactory.getLogger("shirokai-holograms");
    private HologramService service;
    @Override public void onInitialize(){
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->new Commands(()->service).register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            service=new HologramService(server,new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("shirokai-holograms")),LOGGER);
            service.reload().exceptionally(error->{LOGGER.error("[holograms] Inicialização falhou; corrija JSON e use /hologram reload",error);return null;});
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{if(service!=null)service.tick();});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{if(service!=null)service.forget(handler.player.getUuid());});
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{if(service!=null)service.close();});
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{if(service!=null)try{service.awaitIo();}catch(InterruptedException e){Thread.currentThread().interrupt();}finally{service=null;}});
    }
}
