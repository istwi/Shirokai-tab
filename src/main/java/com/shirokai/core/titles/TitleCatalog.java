package com.shirokai.core.titles;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public record TitleCatalog(int schemaVersion, Map<String, TitleDefinition> definitions) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public TitleCatalog { definitions = Map.copyOf(definitions); }
    public static TitleCatalog defaults() {
        var traveler = new TitleDefinition("traveler", "Viajante", "[VIAJANTE]", "Explore o mundo Shirokai.",
                TitleDefinition.Category.SPECIAL, 0, null, true, false, false, true);
        return new TitleCatalog(1, Map.of(traveler.id(), traveler));
    }
    public static TitleCatalog load(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) try { Files.writeString(file, GSON.toJson(defaults()) + "\n", StandardOpenOption.CREATE_NEW); }
        catch (FileAlreadyExistsException ignored) { }
        if (Files.size(file) > 262144) throw new IOException(file + ": catálogo excede 256 KiB");
        try { return parse(Files.readString(file)); }
        catch (Exception error) { throw new IOException(file + ": " + error.getMessage(), error); }
    }
    public static TitleCatalog parse(String json) throws IOException {
        JsonElement value;
        try (var reader = new JsonReader(new StringReader(json))) {
            reader.setLenient(false); value = GSON.getAdapter(JsonElement.class).read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Conteúdo após JSON");
        }
        if (value == null || !value.isJsonObject()) throw new IllegalArgumentException("Catálogo deve ser objeto");
        var root = value.getAsJsonObject();
        fields(root, Set.of("schemaVersion", "definitions"));
        if (!root.has("schemaVersion") || !root.get("schemaVersion").toString().equals("1")) throw new IllegalArgumentException("titles.schemaVersion deve ser 1");
        if (!root.has("definitions") || !root.get("definitions").isJsonObject()) throw new IllegalArgumentException("definitions deve ser objeto");
        var definitions = new HashMap<String, TitleDefinition>();
        for (var entry : root.getAsJsonObject("definitions").entrySet()) {
            if (definitions.size() >= 500) throw new IllegalArgumentException("Máximo de 500 títulos");
            if (!entry.getValue().isJsonObject()) throw new IllegalArgumentException("Título deve ser objeto: " + entry.getKey());
            var object = entry.getValue().getAsJsonObject();
            fields(object, Set.of("id", "name", "display", "description", "category", "priority", "permission", "unlockedByDefault", "tab", "scoreboard", "chat"));
            for (String key : Set.of("id", "name", "display", "description", "category")) string(object, key, false);
            String permission = string(object, "permission", true);
            for (String key : Set.of("unlockedByDefault", "tab", "scoreboard", "chat")) {
                if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isBoolean()) throw new IllegalArgumentException(key + " deve ser booleano");
            }
            if (!object.has("priority") || !object.get("priority").toString().matches("-?[0-9]{1,8}")) throw new IllegalArgumentException("priority deve ser inteiro");
            var definition = new TitleDefinition(string(object, "id", false), string(object, "name", false), string(object, "display", false),
                    string(object, "description", false), TitleDefinition.Category.valueOf(string(object, "category", false)),
                    object.get("priority").getAsInt(), permission, object.get("unlockedByDefault").getAsBoolean(),
                    object.get("tab").getAsBoolean(), object.get("scoreboard").getAsBoolean(), object.get("chat").getAsBoolean());
            if (definition.category()==TitleDefinition.Category.CLAN) throw new IllegalArgumentException("CLAN é derivado de pertencimento real, não de configuração");
            if (!entry.getKey().equals(definition.id())) throw new IllegalArgumentException("ID diverge da chave: " + entry.getKey());
            definitions.put(definition.id(), definition);
        }
        return new TitleCatalog(1, definitions);
    }
    private static String string(JsonObject object, String key, boolean nullable) {
        var value = object.get(key);
        if (nullable && (value == null || value.isJsonNull())) return null;
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException(key + " deve ser texto");
        return value.getAsString();
    }
    private static void fields(JsonObject object, Set<String> allowed) {
        for (String key : object.keySet()) if (!allowed.contains(key)) throw new IllegalArgumentException("Campo desconhecido: " + key);
    }
}
