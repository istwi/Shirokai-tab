package com.shirokai.holograms;

import net.minecraft.text.*;
import java.text.BreakIterator;
import java.util.*;

/** Tick-based shared phase, no per-viewer timers. Translations/opacity interpolate in the vanilla client. */
public record AnimationFrame(double x,double y,int opacity,double fraction) {
    public static AnimationFrame at(Definitions.Motion motion,long elapsed,double textWidth) {
        if(motion.type()==Definitions.Animation.STATIC)return new AnimationFrame(0,0,255,1);
        double duration=Math.max(1,motion.durationTicks()/motion.speed());
        double cycle=duration+motion.pauseTicks();
        double time=Math.max(0,elapsed);
        boolean finished=motion.repetitions()>0 && time>=cycle*motion.repetitions();
        double p=finished?1:Math.min(1,(time%cycle)/duration);
        double ease=p*p*(3-2*p),half=(motion.width()+textWidth)/2;
        return switch(motion.type()) {
            case MARQUEE_LEFT -> new AnimationFrame(half*(1-2*p),0,255,1);
            case MARQUEE_RIGHT -> new AnimationFrame(half*(2*p-1),0,255,1);
            case SLIDE_LEFT -> new AnimationFrame(p>=1?0:-motion.width()*(1-ease),0,255,1);
            case SLIDE_RIGHT -> new AnimationFrame(motion.width()*(1-ease),0,255,1);
            case TYPEWRITER -> new AnimationFrame(0,0,255,p);
            case FADE_IN -> new AnimationFrame(0,0,(int)Math.round(255*p),1);
            case FADE_OUT -> new AnimationFrame(0,0,(int)Math.round(255*(1-p)),1);
            case BOUNCE -> new AnimationFrame(0,motion.width()*.1*Math.pow(Math.sin(Math.PI*p),2),255,1);
            case PING_PONG -> new AnimationFrame(-motion.width()/2*Math.cos(2*Math.PI*p),0,255,1);
            default -> new AnimationFrame(0,0,255,1);
        };
    }
    /** Preserve component styles and don't cut surrogate pairs or combining sequences. */
    public Text text(Text source) {
        if(opacity<4)return Text.empty(); // Vanilla treats opacity 0..3 as fully opaque.
        if(fraction>=1)return source;
        String plain=source.getString(); var iterator=BreakIterator.getCharacterInstance(Locale.ROOT);iterator.setText(plain);
        var ends=new ArrayList<Integer>(); for(int end=iterator.next();end!=BreakIterator.DONE;end=iterator.next())ends.add(end);
        int count=(int)Math.floor(ends.size()*fraction); int limit=count==0?0:ends.get(count-1);
        var result=Text.empty();int[] remaining={limit};
        source.visit((style,content)->{
            int take=Math.min(content.length(),remaining[0]);if(take>0)result.append(Text.literal(content.substring(0,take)).setStyle(style));remaining[0]-=take;return Optional.empty();
        },Style.EMPTY);return result;
    }
}
