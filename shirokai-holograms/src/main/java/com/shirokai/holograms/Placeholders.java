package com.shirokai.holograms;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

/** Optional APIs are linked only when installed. Values are bounded and inserted as literal components. */
public final class Placeholders {
    public static final String KEY_PATTERN="[a-z0-9_.-]+:[a-z0-9_./-]+(?: [a-zA-Z0-9_.:/ -]{1,128})?";
    private final MinecraftServer server; private final Logger logger; private boolean papi;
    public Placeholders(MinecraftServer server,Logger logger){this.server=server;this.logger=logger;papi=FabricLoader.getInstance().isModLoaded("placeholder-api");}
    public String resolve(String key,ServerPlayerEntity player,String fallback) {
        String value=switch(key){
            case "server:online","shirokai:online" -> Integer.toString(server.getCurrentPlayerCount());
            case "server:max" -> Integer.toString(server.getMaxPlayerCount());
            case "player:name","shirokai:player" -> player.getGameProfile().getName();
            case "player:uuid","shirokai:uuid" -> player.getUuidAsString();
            case "player:world" -> player.getServerWorld().getRegistryKey().getValue().toString();
            default -> null;
        };
        if(value==null && papi)try{value=Papi.resolve(key,player);}catch(RuntimeException|LinkageError error){papi=false;logger.error("[holograms] Text Placeholder API incompatível; usando fallback",error);}
        if(value==null)return fallback;
        value=value.replaceAll("[\\p{Cc}]","");
        return value.codePointCount(0,value.length())>512?value.substring(0,value.offsetByCodePoints(0,512)):value;
    }
    private static final class Papi {
        static String resolve(String key,ServerPlayerEntity player) {
            int split=key.indexOf(' ');var id=Identifier.tryParse(split<0?key:key.substring(0,split));
            if(id==null || !eu.pb4.placeholders.api.Placeholders.getPlaceholders().containsKey(id))return null;
            var result=eu.pb4.placeholders.api.Placeholders.parsePlaceholder(id,split<0?null:key.substring(split+1),eu.pb4.placeholders.api.PlaceholderContext.of(player));
            return result.isValid()?result.text().getString():null;
        }
    }
}
