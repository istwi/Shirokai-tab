package com.shirokai.core.motd;

import com.shirokai.core.styles.StyleEngine;
import net.minecraft.text.Text;
import java.util.*;

/** Compiled and rendered only on the server thread. Ping handlers consume an immutable snapshot. */
public final class MotdPlan {
    private final MotdConfig config;
    private final StyleEngine styles;
    private final List<List<StyleEngine.Compiled>> frames;
    public MotdPlan(MotdConfig config,StyleEngine styles){
        this.config=config;this.styles=styles;
        frames=config.frames().stream().map(lines->lines.stream().map(styles::compile).toList()).toList();
        for(var frame:frames)for(var line:frame){
            if(styles.literalSize(line)>512)throw new IllegalArgumentException("MOTD: linha expandida excede 512 caracteres literais");
            if(!Set.of("server:online","server:max","server:version").containsAll(line.variables()))
                throw new IllegalArgumentException("MOTD: placeholders permitidos: server:online, server:max, server:version (sem jogador antes do login)");
        }
    }
    public MotdConfig config(){return config;}
    public Text render(long tick,int online,int maximum,String version){
        var values=Map.of("server:online",Integer.toString(online),"server:max",Integer.toString(maximum),"server:version",version);
        var frame=frames.get((int)Math.floorMod(Math.floorDiv(tick,config.frameTicks()),frames.size()));
        var result=Text.empty();
        for(var line:frame){if(!result.getSiblings().isEmpty())result.append("\n");result.append(styles.render(line,values::get,tick,true).copy());}
        return result;
    }
}
