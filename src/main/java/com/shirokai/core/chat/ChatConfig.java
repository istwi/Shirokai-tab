package com.shirokai.core.chat;

import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public record ChatConfig(int schemaVersion, Channel defaultChannel, double localRange,
        int maximumLength, long cooldownMillis, long duplicateWindowSeconds, Set<String> blockedWords,
        Map<Channel,String> channelPermissions,Map<Channel,String> nameFormats, boolean takeVanillaPrivateCommands) {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public ChatConfig {
        if(schemaVersion!=1 || defaultChannel==null || defaultChannel==Channel.PRIVATE || !Double.isFinite(localRange)||localRange<1||localRange>10000
                ||maximumLength<1||maximumLength>1024||cooldownMillis<0||cooldownMillis>60000||duplicateWindowSeconds<0||duplicateWindowSeconds>3600)
            throw new IllegalArgumentException("Configuração de chat fora dos limites");
        blockedWords=Set.copyOf(Objects.requireNonNull(blockedWords));channelPermissions=Map.copyOf(Objects.requireNonNull(channelPermissions));nameFormats=Map.copyOf(Objects.requireNonNull(nameFormats));
        if(blockedWords.size()>1000||blockedWords.stream().anyMatch(word->word.isBlank()||word.length()>64))throw new IllegalArgumentException("Filtro de palavras inválido");
        for(Channel channel:Channel.values()){
            String permission=channelPermissions.get(channel),format=nameFormats.get(channel);
            if(permission==null||!permission.matches("[a-zA-Z0-9_.-]{0,128}")||format==null||format.length()>512||format.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Permissão/formato inválido: "+channel);
            String remaining=format.replace("{channel}","").replace("{tags}","").replace("{name}","").replace("{target}","");
            if(remaining.contains("{")||remaining.contains("}"))throw new IllegalArgumentException("Variável de formato desconhecida");
        }
    }
    public static ChatConfig defaults(){
        return new ChatConfig(1,Channel.GLOBAL,100,256,1000,30,Set.of(),
            Map.of(Channel.GLOBAL,"",Channel.LOCAL,"",Channel.CLAN,"",Channel.STAFF,"shirokaicore.chat.staff",Channel.PRIVATE,""),
            Map.of(Channel.GLOBAL,"[G] {tags}{name}",Channel.LOCAL,"[L] {tags}{name}",Channel.CLAN,"[CLÃ] {tags}{name}",Channel.STAFF,"[STAFF] {tags}{name}",Channel.PRIVATE,"[PRIVADO] {tags}{name} → {target}"),true);
    }
    public static ChatConfig load(Path file)throws IOException{
        Files.createDirectories(file.getParent());if(!Files.exists(file))try{Files.writeString(file,GSON.toJson(defaults())+"\n",StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored){}
        if(Files.size(file)>131072)throw new IOException("chat.json excede128KiB");
        try{return parse(Files.readString(file));}catch(Exception error){throw new IOException(file+": "+error.getMessage(),error);}
    }
    public static ChatConfig parse(String input)throws IOException{
        JsonElement value;try(var reader=new JsonReader(new StringReader(input))){reader.setLenient(false);value=GSON.getAdapter(JsonElement.class).read(reader);if(reader.peek()!=JsonToken.END_DOCUMENT)throw new IllegalArgumentException("Conteúdo após JSON");}
        if(value==null||!value.isJsonObject())throw new IllegalArgumentException("Chat deve ser objeto");var root=value.getAsJsonObject();var defaults=GSON.toJsonTree(defaults()).getAsJsonObject();
        for(String key:root.keySet()){
            if(!defaults.has(key))throw new IllegalArgumentException("Campo desconhecido: "+key);var given=root.get(key);var expected=defaults.get(key);
            if(given.isJsonNull()||expected.isJsonPrimitive()&&(!given.isJsonPrimitive()||expected.getAsJsonPrimitive().isString()!=given.getAsJsonPrimitive().isString()||expected.getAsJsonPrimitive().isBoolean()!=given.getAsJsonPrimitive().isBoolean()))throw new IllegalArgumentException("Tipo inválido: "+key);
            defaults.add(key,given);
        }
        if(!root.has("schemaVersion"))throw new IllegalArgumentException("schemaVersion obrigatório");
        for(String key:List.of("schemaVersion","maximumLength","cooldownMillis","duplicateWindowSeconds"))if(!defaults.get(key).toString().matches("[0-9]{1,9}"))throw new IllegalArgumentException("Inteiro inválido: "+key);
        if(!defaults.get("localRange").isJsonPrimitive()||!defaults.getAsJsonPrimitive("localRange").isNumber())throw new IllegalArgumentException("localRange deve ser número");
        if(!defaults.get("blockedWords").isJsonArray())throw new IllegalArgumentException("blockedWords deve ser lista");
        for(var word:defaults.getAsJsonArray("blockedWords"))if(!word.isJsonPrimitive()||!word.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Palavra inválida");
        for(String key:List.of("nameFormats","channelPermissions")){
            if(!defaults.get(key).isJsonObject())throw new IllegalArgumentException(key+" deve ser objeto");
            for(var entry:defaults.getAsJsonObject(key).entrySet()){Channel.valueOf(entry.getKey());if(!entry.getValue().isJsonPrimitive()||!entry.getValue().getAsJsonPrimitive().isString())throw new IllegalArgumentException("Texto esperado em "+key);}
        }
        // Upgrade only the exact two old defaults in memory; preserve the file and all customized formats.
        var formats=defaults.getAsJsonObject("nameFormats");
        if(formats.has("GLOBAL")&&formats.get("GLOBAL").getAsString().equals("[GLOBAL] {tags}{name}"))formats.addProperty("GLOBAL","[G] {tags}{name}");
        if(formats.has("LOCAL")&&formats.get("LOCAL").getAsString().equals("[LOCAL] {tags}{name}"))formats.addProperty("LOCAL","[L] {tags}{name}");
        return GSON.fromJson(defaults,ChatConfig.class);
    }
}
