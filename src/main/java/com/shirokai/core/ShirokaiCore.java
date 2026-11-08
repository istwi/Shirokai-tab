package com.shirokai.core;

import com.shirokai.core.administration.AdminCommands;
import com.shirokai.core.profiles.ProfileCommands;
import com.shirokai.core.chat.ChatCommands;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShirokaiCore implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("shirokai-core");
    private CoreRuntime runtime;
    @Override public void onInitialize() {
        String version = FabricLoader.getInstance().getModContainer("shirokai-core").orElseThrow()
                .getMetadata().getVersion().getFriendlyString();
        var commands = new AdminCommands(() -> runtime, version);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            commands.register(dispatcher);
            new ProfileCommands(() -> runtime).register(dispatcher);
            new com.shirokai.core.clans.ClanCommands(() -> runtime).register(dispatcher);
            new ChatCommands(()->runtime).register(dispatcher);
            new com.shirokai.core.tab.TabCommands(()->runtime).register(dispatcher);
            new com.shirokai.core.scoreboard.ScoreboardCommands(()->runtime).register(dispatcher);
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            try { runtime = new CoreRuntime(server, LOGGER); }
            catch (Exception | LinkageError failure) { LOGGER.error("[core] Inicialização falhou; nenhum arquivo personalizado foi substituído", failure); }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{if(runtime!=null)runtime.prepareStop();});
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (runtime != null) {
                try { runtime.close(); } catch (Exception failure) { LOGGER.error("[core] Falha ao encerrar serviços", failure); }
                runtime = null;
            }
        });
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message,sender,params)->{
            if(runtime==null||!runtime.config().enabled("chat"))return true;
            if(!runtime.running("chat")){sender.sendMessage(net.minecraft.text.Text.literal("Chat indisponível"),false);return false;}
            return runtime.chat().signed(sender,message);
        });
        ServerMessageEvents.ALLOW_COMMAND_MESSAGE.register((message,source,params)->{
            if(source.getPlayer()==null||runtime==null||!runtime.config().enabled("chat"))return true;
            if(!runtime.running("chat")){source.sendError(net.minecraft.text.Text.literal("Chat indisponível"));return false;}
            return runtime.chat().signed(source.getPlayer(),message);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (runtime != null) runtime.playerJoined(handler.player.getUuid());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (runtime != null) runtime.playerLeft(handler.player.getUuid());
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> { if (runtime != null) runtime.tick(); });
        LOGGER.info("Shirokai Core {}: configuração, storage e administração registrados", version);
    }
}
