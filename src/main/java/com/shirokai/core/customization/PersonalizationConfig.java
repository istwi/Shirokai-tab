package com.shirokai.core.customization;

import com.google.gson.*;
import com.shirokai.core.styles.JsonValidation;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public record PersonalizationConfig(int schemaVersion,Map<VisualTarget,Rule> surfaces){
    public record Rule(boolean enabled,boolean animated,String permission){public Rule{if(permission==null||!permission.matches("[a-zA-Z0-9_.-]{0,128}"))throw new IllegalArgumentException("Permissão de superfície inválida");}}
    public PersonalizationConfig{surfaces=Map.copyOf(surfaces);if(schemaVersion!=1||!surfaces.keySet().equals(Set.of(VisualTarget.values())))throw new IllegalArgumentException("Configure todas as superfícies de personalização");}
    public static PersonalizationConfig defaults(){var map=new EnumMap<VisualTarget,Rule>(VisualTarget.class);for(var target:VisualTarget.values())map.put(target,new Rule(true,true,""));return new PersonalizationConfig(1,map);}
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public static PersonalizationConfig load(Path file)throws IOException{
        Files.createDirectories(file.getParent());if(!Files.exists(file))try{Files.writeString(file,GSON.toJson(defaults())+"\n",StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored){}
        if(Files.size(file)>65536)throw new IOException("personalization.json excede64KiB");
        try{var root=JsonValidation.object(Files.readString(file));JsonValidation.fields(root,Set.of("schemaVersion","surfaces"));JsonValidation.integer(root,"schemaVersion");
            if(!root.has("surfaces")||!root.get("surfaces").isJsonObject())throw new IllegalArgumentException("surfaces deve ser objeto");
            for(var entry:root.getAsJsonObject("surfaces").entrySet()){VisualTarget.valueOf(entry.getKey());var rule=entry.getValue().getAsJsonObject();JsonValidation.fields(rule,Set.of("enabled","animated","permission"));JsonValidation.bool(rule,"enabled");JsonValidation.bool(rule,"animated");JsonValidation.string(rule,"permission",false);}
            return GSON.fromJson(root,PersonalizationConfig.class);
        }catch(Exception error){throw new IOException(file+": "+error.getMessage(),error);}
    }
}
