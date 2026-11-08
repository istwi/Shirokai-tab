package com.shirokai.core.tab;

import com.google.gson.*;
import com.shirokai.core.styles.JsonValidation;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Separate opt-in configuration; tab.json schema/constructors remain compatible. */
public record TabExtrasConfig(int schemaVersion, Listed listed, Nametags nametags, BelowName belowName, Bossbar bossbar) {
    public record Listed(boolean enabled,String hiddenPermission,String seeHiddenPermission,boolean sameWorldOnly) {
        public Listed { node(hiddenPermission);node(seeHiddenPermission); }
    }
    public record Nametags(boolean enabled,String prefix,String suffix,String visibility) {
        public Nametags { Objects.requireNonNull(prefix);Objects.requireNonNull(suffix);
            if(!Set.of("ALWAYS","NEVER","HIDE_FOR_OTHER_TEAMS","HIDE_FOR_OWN_TEAM").contains(visibility))throw new IllegalArgumentException("Visibilidade de nametag inválida"); }
    }
    public record BelowName(boolean enabled,String label,String value) {
        public BelowName { Objects.requireNonNull(label);Objects.requireNonNull(value); }
    }
    public record Bossbar(boolean enabled,String title,String progress,String color,String style,TabConfig.Condition condition) {
        public Bossbar {
            Objects.requireNonNull(title);Objects.requireNonNull(progress);Objects.requireNonNull(condition);
            if(!Set.of("PINK","BLUE","RED","GREEN","YELLOW","PURPLE","WHITE").contains(color) ||
                !Set.of("PROGRESS","NOTCHED_6","NOTCHED_10","NOTCHED_12","NOTCHED_20").contains(style))
                throw new IllegalArgumentException("Cor/estilo da bossbar inválido");
        }
    }
    private static void node(String value){if(value==null||!value.matches("[a-zA-Z0-9_.-]{1,128}"))throw new IllegalArgumentException("Permissão de listagem inválida");}
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public TabExtrasConfig { if((schemaVersion!=1 && schemaVersion!=2)||listed==null||nametags==null||belowName==null||bossbar==null)throw new IllegalArgumentException("Extras de TAB inválidos"); }
    public static TabExtrasConfig defaults(){return new TabExtrasConfig(2,
        new Listed(false,"shirokaicore.tab.hidden","shirokaicore.tab.seehidden",false),
        new Nametags(true,"%shirokai:title_prefix%","","ALWAYS"),
        new BelowName(false,"<gray>ms</gray>","%shirokai:ping%"),
        new Bossbar(false,"<gold>✦ SHIROKAI ✦</gold>","1","YELLOW","PROGRESS",TabConfig.Condition.any()));}
    public boolean requiresRestartComparedTo(TabExtrasConfig previous){
        return listed.enabled()!=previous.listed.enabled()||nametags.enabled()!=previous.nametags.enabled()||
            belowName.enabled()!=previous.belowName.enabled()||bossbar.enabled()!=previous.bossbar.enabled();
    }
    public static TabExtrasConfig load(Path file)throws IOException{
        Files.createDirectories(file.getParent());
        if(!Files.exists(file))try{Files.writeString(file,GSON.toJson(defaults())+"\n",StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored){}
        if(Files.size(file)>65536)throw new IOException("tab-extras.json excede64KiB");
        try{return parse(Files.readString(file));}catch(Exception error){throw new IOException(file+": "+error.getMessage(),error);}
    }
    public static TabExtrasConfig parse(String text)throws IOException{
        var root=JsonValidation.object(text);JsonValidation.fields(root,Set.of("schemaVersion","listed","nametags","belowName","bossbar"));JsonValidation.integer(root,"schemaVersion");
        var l=object(root,"listed",Set.of("enabled","hiddenPermission","seeHiddenPermission","sameWorldOnly"));
        JsonValidation.bool(l,"enabled");JsonValidation.bool(l,"sameWorldOnly");JsonValidation.string(l,"hiddenPermission",false);JsonValidation.string(l,"seeHiddenPermission",false);
        var n=object(root,"nametags",Set.of("enabled","prefix","suffix","visibility"));JsonValidation.bool(n,"enabled");
        for(String key:List.of("prefix","suffix","visibility"))JsonValidation.string(n,key,false);
        var b=object(root,"belowName",Set.of("enabled","label","value"));JsonValidation.bool(b,"enabled");
        for(String key:List.of("label","value"))JsonValidation.string(b,key,false);
        var bar=object(root,"bossbar",Set.of("enabled","title","progress","color","style","condition"));JsonValidation.bool(bar,"enabled");
        for(String key:List.of("title","progress","color","style"))JsonValidation.string(bar,key,false);
        var c=object(bar,"condition",Set.of("permission","group","worlds"));JsonValidation.string(c,"permission",true);JsonValidation.string(c,"group",true);JsonValidation.strings(c,"worlds");
        int version=root.get("schemaVersion").getAsInt();
        // Upgrade only the exact old default; schema 2 allows an explicit opt-out.
        if(version==1 && !n.get("enabled").getAsBoolean() && n.get("prefix").getAsString().equals("%shirokai:title_prefix%") && n.get("suffix").getAsString().isEmpty() && n.get("visibility").getAsString().equals("ALWAYS")) n.addProperty("enabled",true);
        if(version==1)root.addProperty("schemaVersion",2);
        return GSON.fromJson(root,TabExtrasConfig.class);
    }
    private static JsonObject object(JsonObject parent,String name,Set<String> fields){
        if(!parent.has(name)||!parent.get(name).isJsonObject())throw new IllegalArgumentException(name+" deve ser objeto");
        var result=parent.getAsJsonObject(name);JsonValidation.fields(result,fields);return result;
    }
}
