package com.shirokai.core.messages;

import com.shirokai.core.chat.Channel;
import com.shirokai.core.clans.ClanSnapshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import java.util.UUID;

/** Literal components only: player data is never interpreted as formatting markup. */
public final class Messages {
    private Messages() {}
    public static MutableText info(String message) { return info(Text.literal(message)); }
    public static MutableText info(Text message) {
        return Text.empty().append(Text.literal("[Shirokai] ").formatted(Formatting.GOLD))
            .append(Text.empty().formatted(Formatting.GRAY).append(message));
    }
    public static MutableText error(String message) {
        return Text.empty().append(Text.literal("[Shirokai] ").formatted(Formatting.GOLD))
            .append(Text.literal(message).formatted(Formatting.RED));
    }
    public static MutableText field(String label, Text value) {
        return Text.literal(label + ": ").formatted(Formatting.GRAY).append(value);
    }
    public static Text player(MinecraftServer server, UUID id) {
        return player(server,null,id);
    }
    public static Text player(MinecraftServer server, net.minecraft.server.network.ServerPlayerEntity viewer, UUID id) {
        var online=server.getPlayerManager().getPlayer(id);
        if(online!=null&&viewer!=null&&!com.shirokai.core.visibility.VisibilityService.canSee(server,viewer,online))online=null;
        return Text.literal(online==null?"Jogador offline":online.getGameProfile().getName())
            .formatted(Formatting.WHITE).styled(style->style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Text.literal(id.toString()))));
    }
    public static String role(ClanSnapshot.Role role) {
        if(role==null)return "Sem cargo";
        return switch(role){case LEADER->"Líder";case OFFICER->"Oficial";case MEMBER->"Membro";};
    }
    public static String channel(Channel channel) {
        return switch(channel){case GLOBAL->"Global";case LOCAL->"Local";case CLAN->"Clã";case STAFF->"Equipe";case PRIVATE->"Privado";};
    }
}
