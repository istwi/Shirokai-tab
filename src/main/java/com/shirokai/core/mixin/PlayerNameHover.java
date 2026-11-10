package com.shirokai.core.mixin;

import com.shirokai.core.chat.ConnectionAnnouncements;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Remove only vanilla player identity tooltips as names are built, not signed message bodies. */
@Mixin(Entity.class)
public abstract class PlayerNameHover {
    @Inject(method="getHoverEvent",at=@At("HEAD"),cancellable=true)
    private void shirokai$playerHover(CallbackInfoReturnable<HoverEvent> result){
        if((Object)this instanceof ServerPlayerEntity&&!ConnectionAnnouncements.playerHoverInfo())result.setReturnValue(null);
    }
}
