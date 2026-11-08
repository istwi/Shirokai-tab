package com.shirokai.smoke.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.*;

/** Test-only connection fixture: no real login/authentication claim, never included in production. */
@Mixin(PlayerManager.class)
public interface SmokePlayers {
    @Accessor("players") List<ServerPlayerEntity> shirokai$players();
    @Accessor("playerMap") Map<UUID, ServerPlayerEntity> shirokai$playerMap();
}
