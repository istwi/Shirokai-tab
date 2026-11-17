package com.shirokai.holograms;

import com.google.gson.*;
import net.minecraft.text.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Advances measured from default 1.21.1 bitmap and Unihex providers, not character counts/spaces. */
public final class FontWidths {
    private static final double[] MISSING={6,1};
    private static final Map<Integer, double[]> WIDTHS = load();
    private static Map<Integer,double[]> load() {
        try(var in=FontWidths.class.getResourceAsStream("/vanilla-widths.json")) {
            if(in==null)throw new IOException("vanilla-widths.json ausente");
            var object=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            var result=new HashMap<Integer,double[]>();object.entrySet().forEach(e->result.put(Integer.valueOf(e.getKey()),new double[]{e.getValue().getAsJsonArray().get(0).getAsDouble(),e.getValue().getAsJsonArray().get(1).getAsDouble()}));return Map.copyOf(result);
        } catch(IOException error){throw new ExceptionInInitializerError(error);}
    }
    public static double width(Text text) {
        double[] total={0};
        text.visit((style,content)->{content.codePoints().forEach(cp->{var glyph=WIDTHS.getOrDefault(cp,MISSING);total[0]+=glyph[0]+(style.isBold()?glyph[1]:0);});return Optional.empty();},Style.EMPTY);
        return Math.ceil(total[0]);
    }
    public static double offset(Definitions.Alignment alignment,double width,double areaWidth) {
        return switch(alignment){case LEFT -> (width-areaWidth)/2;case RIGHT -> (areaWidth-width)/2;case CENTER -> 0;};
    }
}
