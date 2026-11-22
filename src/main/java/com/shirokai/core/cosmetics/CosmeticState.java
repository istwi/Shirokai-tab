package com.shirokai.core.cosmetics;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.*;

public final class CosmeticState {
    public int schemaVersion=1;
    public Map<String,String> cosmetics=new TreeMap<>();
    public boolean cosmeticsHidden;
    public Preferences cosmeticPreferences=new Preferences();
    public static final class Preferences { public int speed=2,size=2,density=2,direction=1; }
    public CosmeticState copy(){return new Gson().fromJson(new Gson().toJson(this),CosmeticState.class);}
    public void validate() throws IOException {
        if(schemaVersion!=1||cosmetics==null||cosmetics.size()>20||cosmeticPreferences==null)throw new IOException("Perfil de cosméticos inválido");
        for(var e:cosmetics.entrySet())if(e.getKey()==null||e.getValue()==null||!e.getKey().matches("[a-z0-9_-]{1,64}")||!e.getValue().matches("[a-z0-9_-]{1,64}"))throw new IOException("Seleção inválida");
        var p=cosmeticPreferences;if(p.speed<1||p.speed>3||p.size<1||p.size>3||p.density<1||p.density>3||Math.abs(p.direction)!=1)throw new IOException("Preferências inválidas");
    }
}
