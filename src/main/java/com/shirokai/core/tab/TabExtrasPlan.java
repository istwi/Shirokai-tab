package com.shirokai.core.tab;

import com.shirokai.core.styles.StyleEngine;
import java.math.BigDecimal;
import java.util.*;

public final class TabExtrasPlan {
    private final TabExtrasConfig config;
    private final StyleEngine styles;
    private final StyleEngine.Compiled prefix,suffix,label,value,title,progress;
    public TabExtrasPlan(TabExtrasConfig config,StyleEngine styles){
        this.config=config;this.styles=styles;
        prefix=compile(config.nametags().prefix(),128);suffix=compile(config.nametags().suffix(),128);
        label=compile(config.belowName().label(),128);value=compile(config.belowName().value(),32);
        title=compile(config.bossbar().title(),256);progress=compile(config.bossbar().progress(),32);
        // Numeric channels must be literal/placeholder values, never formatted or animated.
        for(var numeric:List.of(value,progress))if(!numeric.animations().isEmpty()||numeric.nodes().stream().anyMatch(n->n instanceof StyleEngine.Group))
            throw new IllegalArgumentException("Valor numérico não permite formatação/animação");
        if(value.variables().isEmpty())score(styles.render(value,k->"",0,false).getString());
        if(progress.variables().isEmpty())progress(styles.render(progress,k->"",0,false).getString());
    }
    private StyleEngine.Compiled compile(String source,int limit){var result=styles.compile(source);if(styles.literalSize(result)>limit)throw new IllegalArgumentException("Template de extras excede"+limit);return result;}
    public static int score(String value){
        if(!value.matches("-?[0-9]{1,10}"))throw new IllegalArgumentException("BELOW_NAME exige inteiro disponível");
        try{return Integer.parseInt(value);}catch(NumberFormatException error){throw new IllegalArgumentException("BELOW_NAME fora de int32",error);}
    }
    public static float progress(String value){
        if(value.length()>32||!value.matches("[0-9]+(?:\\.[0-9]+)?"))throw new IllegalArgumentException("Progresso exige decimal0..1 disponível");
        var number=new BigDecimal(value);
        if(number.compareTo(BigDecimal.ZERO)<0||number.compareTo(BigDecimal.ONE)>0)throw new IllegalArgumentException("Progresso fora de0..1");
        return number.floatValue();
    }
    public TabExtrasConfig config(){return config;}public StyleEngine styles(){return styles;}
    public StyleEngine.Compiled prefix(){return prefix;}public StyleEngine.Compiled suffix(){return suffix;}
    public StyleEngine.Compiled label(){return label;}public StyleEngine.Compiled value(){return value;}
    public StyleEngine.Compiled title(){return title;}public StyleEngine.Compiled progress(){return progress;}
}
