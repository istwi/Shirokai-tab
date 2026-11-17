package com.shirokai.holograms.text;

import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.*;
import java.util.*;

/** Shared strict JSON checks; Gson otherwise coerces strings/booleans silently. */
public final class JsonValidation{
    private JsonValidation(){}
    public static JsonObject object(String input)throws IOException{
        try(var reader=new JsonReader(new StringReader(input))){reader.setLenient(false);var result=new Gson().getAdapter(JsonElement.class).read(reader);if(result==null||!result.isJsonObject()||reader.peek()!=JsonToken.END_DOCUMENT)throw new IllegalArgumentException("Esperado objeto JSON único");return result.getAsJsonObject();}
    }
    public static void fields(JsonObject object,Set<String> allowed){for(String key:object.keySet())if(!allowed.contains(key))throw new IllegalArgumentException("Campo desconhecido: "+key);}
    public static void integer(JsonObject object,String key){if(!object.has(key)||!object.get(key).toString().matches("-?[0-9]{1,9}"))throw new IllegalArgumentException("Inteiro esperado: "+key);}
    public static void string(JsonObject object,String key,boolean nullable){var v=object.get(key);if(nullable&&(v==null||v.isJsonNull()))return;if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Texto esperado: "+key);}
    public static void bool(JsonObject object,String key){var v=object.get(key);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Booleano esperado: "+key);}
    public static void strings(JsonObject object,String key){var v=object.get(key);if(v==null||!v.isJsonArray())throw new IllegalArgumentException("Lista esperada: "+key);for(var s:v.getAsJsonArray())if(!s.isJsonPrimitive()||!s.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Texto esperado em "+key);}
}
