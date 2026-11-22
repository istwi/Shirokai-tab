package com.shirokai.core.cosmetics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
public final class CosmeticNames {
    private CosmeticNames(){}
    public static Text target(MinecraftServer server,String name){
        for(var p:server.getPlayerManager().getPlayerList())if(p.getGameProfile().getName().equalsIgnoreCase(name)||com.shirokai.core.integrations.EssentialsAccess.display(p).equals(name))return Text.literal(name).formatted(CosmeticService.color(p));
        return Text.literal(name).formatted(Formatting.WHITE);
    }
}
