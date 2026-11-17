package com.shirokai.holograms;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import java.util.UUID;

public final class Permissions {
    private Permissions(){ }
    public static boolean allowed(ServerCommandSource source,String action) {
        if(source.getPlayer()==null)return source.hasPermissionLevel(4);
        if(!FabricLoader.getInstance().isModLoaded("luckperms"))return source.hasPermissionLevel(4);
        try{return LuckPerms.allowed(source.getPlayer().getUuid(),action);}catch(RuntimeException|LinkageError failure){return false;}
    }
    private static final class LuckPerms {
        static boolean allowed(UUID id,String action) {
            var user=net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(id);if(user==null)return false;
            var data=user.getCachedData().getPermissionData();
            return data.checkPermission("shirokai.holograms.admin").asBoolean()||data.checkPermission("shirokai.holograms."+action).asBoolean();
        }
    }
}
