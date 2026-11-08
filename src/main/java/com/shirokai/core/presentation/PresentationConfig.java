package com.shirokai.core.presentation;

import com.google.gson.*;
import com.shirokai.core.chat.Channel;
import com.shirokai.core.styles.JsonValidation;
import com.shirokai.core.tab.TabConfig.Condition;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public record PresentationConfig(int schemaVersion, boolean chatEnabled, boolean menusEnabled, boolean mentionsEnabled,
        String mentionPermission, Map<Surface,String> templates, List<Rule> rules, Map<String,String> titleTemplates) {
    public PresentationConfig(int schemaVersion,boolean chatEnabled,boolean menusEnabled,boolean mentionsEnabled,
            String mentionPermission,Map<Surface,String> templates,List<Rule> rules){
        this(schemaVersion,chatEnabled,menusEnabled,mentionsEnabled,mentionPermission,templates,rules,Map.of());
    }
    public record Rule(String id,int priority,Condition condition,Map<Surface,String> templates,Map<Channel,String> chatFormats) {
        public Rule {
            templates=Map.copyOf(templates);chatFormats=Map.copyOf(chatFormats);
            if(id==null||!id.matches("[a-z][a-z0-9_-]{0,63}")||priority < -1000000||priority>1000000||condition==null)throw new IllegalArgumentException("Regra visual inválida");
        }
    }
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public PresentationConfig {
        templates=Map.copyOf(templates);rules=List.copyOf(rules);
        titleTemplates=titleTemplates==null?Map.of():Map.copyOf(titleTemplates);
        if(titleTemplates.size()>500||titleTemplates.keySet().stream().anyMatch(id->!id.matches("[a-z0-9][a-z0-9_-]{0,63}")||id.startsWith("clan_")))throw new IllegalArgumentException("IDs de titleTemplates inválidos");
        if(schemaVersion!=1||mentionPermission==null||!mentionPermission.matches("[a-zA-Z0-9_.-]{0,128}")||templates.size()!=Surface.values().length||rules.size()>50)throw new IllegalArgumentException("Configuração visual inválida");
        var ids=new HashSet<String>();for(var rule:rules)if(!ids.add(rule.id()))throw new IllegalArgumentException("Regra visual duplicada");
    }
    public static PresentationConfig defaults(){return new PresentationConfig(1,true,true,true,"",Map.of(
        Surface.MENU_NAME,"<aqua>%visual:value%</aqua>",Surface.MENU_LORE,"<gray>%visual:value%</gray>",
        Surface.MENU_TITLE,"<gold><bold>%visual:value%</bold></gold>",Surface.TITLE,"<gold>%visual:value%</gold>",
        Surface.CLAN_NAME,"<aqua>%visual:value%</aqua>",Surface.CLAN_TAG,"<aqua>%visual:value%</aqua>",Surface.MENTION,"<yellow>%visual:value%</yellow>"),List.of());}
    public static PresentationConfig load(Path file)throws IOException{
        Files.createDirectories(file.getParent());if(!Files.exists(file))try{Files.writeString(file,GSON.toJson(defaults())+"\n",StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored){}
        if(Files.size(file)>262144)throw new IOException("presentation.json excede256KiB");
        try{return parse(Files.readString(file));}catch(Exception error){throw new IOException(file+": "+error.getMessage(),error);}
    }
    private static void strings(JsonObject object,Set<String> keys){JsonValidation.fields(object,keys);for(String key:object.keySet())JsonValidation.string(object,key,false);}
    public static PresentationConfig parse(String input)throws IOException{
        var root=JsonValidation.object(input);JsonValidation.fields(root,Set.of("schemaVersion","chatEnabled","menusEnabled","mentionsEnabled","mentionPermission","templates","rules","titleTemplates"));
        JsonValidation.integer(root,"schemaVersion");for(String key:List.of("chatEnabled","menusEnabled","mentionsEnabled"))JsonValidation.bool(root,key);JsonValidation.string(root,"mentionPermission",false);
        var surfaces=new HashSet<String>();for(var surface:Surface.values())surfaces.add(surface.name());var channels=new HashSet<String>();for(var channel:Channel.values())channels.add(channel.name());
        if(!root.has("templates")||!root.get("templates").isJsonObject())throw new IllegalArgumentException("templates deve ser objeto");strings(root.getAsJsonObject("templates"),surfaces);
        if(!root.has("rules")||!root.get("rules").isJsonArray())throw new IllegalArgumentException("rules deve ser lista");
        for(var item:root.getAsJsonArray("rules")){
            if(!item.isJsonObject())throw new IllegalArgumentException("Regra deve ser objeto");var r=item.getAsJsonObject();JsonValidation.fields(r,Set.of("id","priority","condition","templates","chatFormats"));JsonValidation.string(r,"id",false);JsonValidation.integer(r,"priority");
            if(!r.has("condition")||!r.get("condition").isJsonObject())throw new IllegalArgumentException("condition deve ser objeto");var c=r.getAsJsonObject("condition");JsonValidation.fields(c,Set.of("permission","group","worlds"));JsonValidation.string(c,"permission",true);JsonValidation.string(c,"group",true);JsonValidation.strings(c,"worlds");
            for(String key:List.of("templates","chatFormats"))if(!r.has(key)||!r.get(key).isJsonObject())throw new IllegalArgumentException(key+" deve ser objeto");
            strings(r.getAsJsonObject("templates"),surfaces);strings(r.getAsJsonObject("chatFormats"),channels);
        }
        if(root.has("titleTemplates")){
            if(!root.get("titleTemplates").isJsonObject())throw new IllegalArgumentException("titleTemplates deve ser objeto");
            var entries=root.getAsJsonObject("titleTemplates");
            for(String id:entries.keySet())JsonValidation.string(entries,id,false);
        }
        return GSON.fromJson(root,PresentationConfig.class);
    }
}
