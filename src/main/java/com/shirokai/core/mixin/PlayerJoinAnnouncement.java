package com.shirokai.core.mixin;

import com.shirokai.core.chat.ConnectionAnnouncements;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(PlayerManager.class)
public abstract class PlayerJoinAnnouncement {
    @ModifyArgs(method="onPlayerConnect",at=@At(value="INVOKE",target="Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"))
    private void shirokai$join(Args args,ClientConnection connection,ServerPlayerEntity player,ConnectedClientData data){
        args.set(0,ConnectionAnnouncements.replace(args.get(0),player,true));
    }
}
