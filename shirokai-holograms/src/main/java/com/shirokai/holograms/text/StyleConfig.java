package com.shirokai.holograms.text;

import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public record StyleConfig(int schemaVersion,Map<String,Animation> animations){
    public record Animation(int frameTicks,List<String> frames){
        public Animation{frames=List.copyOf(frames);if(frameTicks<5||frameTicks>1200||frames.isEmpty()||frames.size()>100||frames.stream().anyMatch(s->s==null||s.length()>2048))throw new IllegalArgumentException("Animação inválida");}
    }
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public StyleConfig{animations=Map.copyOf(animations);if(schemaVersion!=1||animations.size()>50)throw new IllegalArgumentException("styles.schemaVersion/animations inválidos");for(String id:animations.keySet())if(!id.matches("[a-z][a-z0-9_]{0,63}"))throw new IllegalArgumentException("ID de animação inválido");}
    public static StyleConfig defaults(){return new StyleConfig(1,Map.of("gold_wave",new Animation(20,List.of("<gold><bold>✦ SHIROKAI ✦</bold></gold>","<gradient:#ffcc33:#fff4b0><bold>✦ SHIROKAI ✦</bold></gradient>","<yellow><bold>✦ SHIROKAI ✦</bold></yellow>"))));}
    public static StyleConfig load(Path path)throws IOException{
        Files.createDirectories(path.getParent());if(!Files.exists(path))try{Files.writeString(path,GSON.toJson(defaults())+"\n",StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored){}
        if(Files.size(path)>262144)throw new IOException("styles.json excede256KiB");try{return parse(Files.readString(path));}catch(Exception e){throw new IOException(path+": "+e.getMessage(),e);}
    }
    public static StyleConfig parse(String input)throws IOException{
        var root=JsonValidation.object(input);JsonValidation.fields(root,Set.of("schemaVersion","animations"));JsonValidation.integer(root,"schemaVersion");
        if(!root.has("animations")||!root.get("animations").isJsonObject())throw new IllegalArgumentException("animations deve ser objeto");
        for(var entry:root.getAsJsonObject("animations").entrySet()){
            if(!entry.getValue().isJsonObject())throw new IllegalArgumentException("Animação deve ser objeto");var animation=entry.getValue().getAsJsonObject();JsonValidation.fields(animation,Set.of("frameTicks","frames"));JsonValidation.integer(animation,"frameTicks");JsonValidation.strings(animation,"frames");
        }
        return GSON.fromJson(root,StyleConfig.class);
    }
}
