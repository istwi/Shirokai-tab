package com.shirokai.core.mixin;

import com.shirokai.core.motd.MotdBridge;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.network.ServerQueryNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerQueryNetworkHandler.class)
public abstract class ServerListMotd {
    @ModifyArg(method="onRequest",at=@At(value="INVOKE",target="Lnet/minecraft/network/packet/s2c/query/QueryResponseS2CPacket;<init>(Lnet/minecraft/server/ServerMetadata;)V"),index=0)
    private ServerMetadata shirokai$motd(ServerMetadata metadata){return MotdBridge.decorate(metadata);}
}
