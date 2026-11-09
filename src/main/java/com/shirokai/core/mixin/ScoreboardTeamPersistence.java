package com.shirokai.core.mixin;

import com.shirokai.core.tab.TeamOwnership;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Filter only serialized transient teams; never detach live players or touch external teams. */
@Mixin(ScoreboardState.class)
public abstract class ScoreboardTeamPersistence {
    @Shadow @Final private Scoreboard scoreboard;
    @Inject(method="teamsToNbt",at=@At("RETURN"),cancellable=true)
    private void shirokai$temporaryTeams(RegistryWrapper.WrapperLookup registries,CallbackInfoReturnable<NbtList> callback){
        var serialized=callback.getReturnValue();var filtered=new NbtList();
        for(int i=0;i<serialized.size();i++){
            var entry=serialized.getCompound(i);
            if(!TeamOwnership.temporaryForSave(scoreboard,scoreboard.getTeam(entry.getString("Name"))))filtered.add(entry);
        }
        callback.setReturnValue(filtered);
    }
}
