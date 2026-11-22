package com.shirokai.core.cosmetics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
public final class CosmeticNames {
    private CosmeticNames(){}
    public static Text target(MinecraftServer server,String name){
        return Text.literal(name).formatted(Formatting.WHITE);
    }
}
