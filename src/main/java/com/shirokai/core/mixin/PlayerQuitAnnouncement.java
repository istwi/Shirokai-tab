package com.shirokai.core.mixin;

import com.shirokai.core.chat.ConnectionAnnouncements;
import net.minecraft.server.network.*;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class PlayerQuitAnnouncement {
    @Shadow public ServerPlayerEntity player;
    @ModifyArg(method="cleanUp",at=@At(value="INVOKE",target="Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"),index=0)
    private Text shirokai$quit(Text original){return ConnectionAnnouncements.replace(original,player,false);}
}
