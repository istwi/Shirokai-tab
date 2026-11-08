package com.shirokai.core.chat;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import java.util.*;

/** Only sixteen color codes and reset; never tags, formatting actions, URLs or commands. */
public final class LegacyChatColors {
    private record Part(String text,Formatting color){}
    private LegacyChatColors(){}
    private static List<Part> parts(String source,Formatting base){
        var result=new ArrayList<Part>();var text=new StringBuilder();var color=base;
        for(int i=0;i<source.length();i++){
            char c=source.charAt(i);
            if(c=='&'&&i+1<source.length()){
                char next=Character.toLowerCase(source.charAt(i+1));
                if(next=='&'){text.append('&');i++;continue;}
                var candidate=Formatting.byCode(next);
                if(next=='r'||candidate!=null&&candidate.isColor()){
                    if(!text.isEmpty()){result.add(new Part(text.toString(),color));text.setLength(0);}
                    color=next=='r'?base:candidate;i++;continue;
                }
            }
            text.append(c);
        }
        if(!text.isEmpty())result.add(new Part(text.toString(),color));
        return result;
    }
    public static String plain(String source){var result=new StringBuilder();for(var part:parts(source,Formatting.WHITE))result.append(part.text());return result.toString();}
    public static Text render(String source,Formatting base){
        var result=Text.empty();for(var part:parts(source,base))result.append(Text.literal(part.text()).formatted(part.color()));return result;
    }
    public static boolean changes(String source){return !plain(source).equals(source);}
}
