package com.shirokai.core.motd;

import com.google.gson.*;
import com.shirokai.core.styles.JsonValidation;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public record MotdConfig(int schemaVersion,boolean enabled,int frameTicks,List<List<String>> frames) {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public MotdConfig {
        if(schemaVersion!=1||frameTicks<5||frameTicks>1200||frames==null||frames.isEmpty()||frames.size()>100)throw new IllegalArgumentException("MOTD inválido");
        frames=frames.stream().map(lines->{
            if(lines==null||lines.isEmpty()||lines.size()>2||lines.stream().anyMatch(s->s==null||s.length()>512))throw new IllegalArgumentException("MOTD exige uma ou duas linhas por quadro");
            return List.copyOf(lines);
        }).toList();
    }
    public static MotdConfig defaults(){return new MotdConfig(1,true,60,List.of(
        List.of("<gradient:#ffcc33:#fff4b0><bold>✦ SHIROKAI ✦</bold></gradient>","<aqua>Explore • Capture • Battle</aqua> <gray>%server:online%/%server:max% online</gray>"),
        List.of("<gold><bold>✦ SHIROKAI ✦</bold></gold>","<green>Seu próximo Pokémon espera por você!</green>")));}
    public static MotdConfig load(Path file)throws IOException{
        Files.createDirectories(file.getParent());
        if(!Files.exists(file))try{Files.writeString(file,GSON.toJson(defaults())+"\n",StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored){}
        if(Files.size(file)>262144)throw new IOException("motd.json excede 256KiB");
        return parse(Files.readString(file));
    }
    public static MotdConfig parse(String input)throws IOException{
        var root=JsonValidation.object(input);JsonValidation.fields(root,Set.of("schemaVersion","enabled","frameTicks","frames"));
        JsonValidation.integer(root,"schemaVersion");JsonValidation.bool(root,"enabled");JsonValidation.integer(root,"frameTicks");
        if(!root.has("frames")||!root.get("frames").isJsonArray())throw new IllegalArgumentException("frames deve ser lista");
        for(var frame:root.getAsJsonArray("frames")){
            if(!frame.isJsonArray())throw new IllegalArgumentException("Quadro deve ser lista de linhas");
            for(var line:frame.getAsJsonArray())if(!line.isJsonPrimitive()||!line.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Linha deve ser texto");
        }
        return GSON.fromJson(root,MotdConfig.class);
    }
}
