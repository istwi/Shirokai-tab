package com.shirokai.core.cosmetics;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

final class CosmeticBattles {
    static boolean blocked(ServerPlayerEntity player){
        if(!FabricLoader.getInstance().isModLoaded("cobblemon"))return false;
        try{return CobblemonCheck.blocked(player);}catch(RuntimeException|LinkageError e){return true;}
    }
    private static final class CobblemonCheck {
        static boolean blocked(ServerPlayerEntity player){return com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer(player)!=null;}
    }
}
