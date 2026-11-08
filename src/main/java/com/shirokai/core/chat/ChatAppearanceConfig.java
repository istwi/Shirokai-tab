package com.shirokai.core.chat;

import com.google.gson.*;
import com.shirokai.core.styles.JsonValidation;
import net.minecraft.util.Formatting;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public record ChatAppearanceConfig(int schemaVersion,boolean enabled,boolean legacyColors,String colorPermission,
                                   boolean connectionMessages,Map<Channel,String> bodyColors){
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public ChatAppearanceConfig{
        bodyColors=Map.copyOf(bodyColors);
        if(schemaVersion!=1||colorPermission==null||!colorPermission.matches("[a-zA-Z0-9_.-]{0,128}"))throw new IllegalArgumentException("Aparência de chat inválida");
        if(bodyColors.size()!=Channel.values().length)throw new IllegalArgumentException("Cor obrigatória para todos os canais");
        for(var channel:Channel.values()){
            var color=Formatting.byName(bodyColors.get(channel));
            if(color==null||!color.isColor())throw new IllegalArgumentException("Cor inválida para "+channel);
        }
    }
    public Formatting color(Channel channel){return Formatting.byName(bodyColors.get(channel));}
    public static ChatAppearanceConfig defaults(){return new ChatAppearanceConfig(1,true,true,"",true,
        Map.of(Channel.GLOBAL,"gray",Channel.LOCAL,"yellow",Channel.CLAN,"white",Channel.STAFF,"white",Channel.PRIVATE,"white"));}
    public static ChatAppearanceConfig load(Path file)throws IOException{
        Files.createDirectories(file.getParent());if(!Files.exists(file))try{Files.writeString(file,GSON.toJson(defaults())+"\n",StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored){}
        if(Files.size(file)>32768)throw new IOException("chat-appearance.json excede32KiB");
        try{return parse(Files.readString(file));}catch(Exception error){throw new IOException(file+": "+error.getMessage(),error);}
    }
    public static ChatAppearanceConfig parse(String text)throws IOException{
        var root=JsonValidation.object(text);JsonValidation.fields(root,Set.of("schemaVersion","enabled","legacyColors","colorPermission","connectionMessages","bodyColors"));
        JsonValidation.integer(root,"schemaVersion");
        for(String key:List.of("enabled","legacyColors","connectionMessages"))JsonValidation.bool(root,key);
        JsonValidation.string(root,"colorPermission",false);
        if(!root.has("bodyColors")||!root.get("bodyColors").isJsonObject())throw new IllegalArgumentException("bodyColors deve ser objeto");
        for(var entry:root.getAsJsonObject("bodyColors").entrySet()){Channel.valueOf(entry.getKey());JsonValidation.string(root.getAsJsonObject("bodyColors"),entry.getKey(),false);}
        return GSON.fromJson(root,ChatAppearanceConfig.class);
    }
}
