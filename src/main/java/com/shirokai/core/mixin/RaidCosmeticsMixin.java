package com.shirokai.core.mixin;

import com.shirokai.core.cosmetics.CosmeticService;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Collection;

/** Optional integration with our Raids victory transition, after victory was saved.
 * Only observes its existing participant collection; no battle/inventory/state writes.
 * Descriptor verified against Raids 0.1.24-quizguard.1 and 0.1.25-returnspawn.1.
 */
@Pseudo
@Mixin(targets="com.shirokai.raids.RaidVictoryTransition",remap=false)
public abstract class RaidCosmeticsMixin {
    @Inject(method="begin(Ljava/lang/String;Ljava/util/Collection;)V",at=@At("RETURN"),require=0,remap=false)
    private void shirokai$celebrateRaid(String name,Collection<?> viewers,CallbackInfo ci) {
        for(Object viewer:viewers)if(viewer instanceof ServerPlayerEntity player){
            var service=CosmeticService.active(player.getServer());
            if(service!=null)try{service.celebrate(player,"raid");}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger("shirokai-core").warn("Celebração de raid indisponível",e);}
        }
    }
}
