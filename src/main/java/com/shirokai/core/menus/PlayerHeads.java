package com.shirokai.core.menus;

import com.mojang.authlib.GameProfile;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;

/** Reuse the authenticated session's texture properties; no profile/network lookup. */
public final class PlayerHeads {
    private PlayerHeads(){}
    public static void apply(ItemStack item,ServerPlayerEntity player){
        if(!item.isOf(Items.PLAYER_HEAD))return;
        var source=player.getGameProfile();var profile=new GameProfile(source.getId(),source.getName());
        profile.getProperties().putAll(source.getProperties());item.set(DataComponentTypes.PROFILE,new ProfileComponent(profile));
    }
}
