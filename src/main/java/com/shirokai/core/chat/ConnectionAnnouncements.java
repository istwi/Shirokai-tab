package com.shirokai.core.chat;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

/** Replaces only vanilla join/quit announcements at their existing broadcast, without duplication. */
public final class ConnectionAnnouncements {
    private static volatile MinecraftChat current;
    private ConnectionAnnouncements(){}
    public static MinecraftChat install(MinecraftChat chat){var previous=current;current=chat;return previous;}
    public static void clear(MinecraftChat chat){if(current==chat)current=null;}
    public static boolean playerHoverInfo(){var chat=current;return chat==null||chat.appearance().playerHoverInfo();}
    public static Text replace(Text original,ServerPlayerEntity player,boolean joined){
        var chat=current;
        if(chat==null||!chat.appearance().connectionMessages())return original;
        if(!(original.getContent() instanceof TranslatableTextContent content))return original;
        String key=content.getKey();
        if(joined?!key.equals("multiplayer.player.joined")&&!key.equals("multiplayer.player.joined.renamed"):!key.equals("multiplayer.player.left"))return original;
        return format(player.getGameProfile().getName(),joined);
    }
    public static Text format(String player,boolean joined){
        return Text.empty().append(Text.literal(joined?"[+] ":"[-] ").formatted(joined?Formatting.GREEN:Formatting.RED))
            .append(Text.literal(player).formatted(Formatting.WHITE));
    }
}
