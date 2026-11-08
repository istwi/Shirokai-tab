package com.shirokai.core.mixin;

import com.shirokai.core.scoreboard.SidebarPackets;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe only; never cancels, rewrites or schedules another mod's packets. */
@Mixin(ServerCommonNetworkHandler.class)
public abstract class SidebarPacketObserver {
    @Inject(method = "send", at = @At("HEAD"))
    private void shirokai$observe(Packet<?> packet, PacketCallbacks callbacks, CallbackInfo ci) {
        com.shirokai.core.tab.TabExtrasPackets.observe((ServerCommonNetworkHandler)(Object)this,packet);
        SidebarPackets.observe((ServerCommonNetworkHandler)(Object)this, packet);
    }
}
