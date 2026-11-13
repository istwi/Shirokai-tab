package com.shirokai.core.motd;

import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;

/** Published text is never mutated. Netty performs no game/provider/config access. */
public final class MotdBridge {
    private static volatile Text description;
    private MotdBridge(){}
    public static void publish(Text value){description=value==null?null:value.copy();}
    public static ServerMetadata decorate(ServerMetadata original){
        Text snapshot=description;
        return snapshot==null?original:new ServerMetadata(snapshot.copy(),original.players(),original.version(),original.favicon(),original.secureChatEnforced());
    }
}
