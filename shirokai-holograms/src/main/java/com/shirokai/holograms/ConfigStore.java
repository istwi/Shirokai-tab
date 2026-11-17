package com.shirokai.holograms;

import com.google.gson.*;
import com.shirokai.holograms.text.JsonValidation;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;

/** IO-only. Reject coercion/unknown fields and atomically replace only holograms.json on edits. */
public final class ConfigStore {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path directory;
    public ConfigStore(Path directory) { this.directory = directory; }
    public Path directory() { return directory; }
    public record Snapshot(Definitions.Settings settings, Definitions.Animations animations, Definitions.Holograms holograms) { }
    public Snapshot load() throws IOException {
        Files.createDirectories(directory);
        return new Snapshot(load("config.json", Definitions.Settings.class, Definitions.Settings.defaults()),
            load("animations.json", Definitions.Animations.class, Definitions.Animations.defaults()),
            load("holograms.json", Definitions.Holograms.class, Definitions.Holograms.defaults()));
    }
    private <T> T load(String name, Class<T> type, T defaults) throws IOException {
        var file = directory.resolve(name);
        if (!Files.exists(file)) try { Files.writeString(file, JSON.toJson(defaults)+"\n", StandardOpenOption.CREATE_NEW); }
        catch (FileAlreadyExistsException ignored) { }
        try {
            if (Files.size(file) > 2_097_152) throw new IOException("arquivo excede 2MiB");
            return parse(Files.readString(file), type);
        } catch (Exception error) { throw new IOException(name+": "+error.getMessage(), error); }
    }
    public static <T> T parse(String input, Class<T> type) throws IOException {
        var object = JsonValidation.object(input);
        validate(object,type,"root");
        return JSON.fromJson(object,type);
    }
    private static void validate(JsonElement value, Type type, String path) {
        if (value == null || value.isJsonNull()) throw new IllegalArgumentException("Campo ausente/nulo: "+path);
        if (type instanceof ParameterizedType generic) {
            if (generic.getRawType() == List.class) {
                if (!value.isJsonArray()) throw new IllegalArgumentException("Lista esperada: "+path);
                for (var child:value.getAsJsonArray()) validate(child,generic.getActualTypeArguments()[0],path+"[]");
            } else {
                if (!value.isJsonObject()) throw new IllegalArgumentException("Objeto esperado: "+path);
                value.getAsJsonObject().entrySet().forEach(e->validate(e.getValue(),generic.getActualTypeArguments()[1],path+"."+e.getKey()));
            }
            return;
        }
        var cls=(Class<?>)type;
        if (cls.isRecord()) {
            if (!value.isJsonObject()) throw new IllegalArgumentException("Objeto esperado: "+path);
            var object=value.getAsJsonObject(); var keys=new HashSet<String>();
            for(var field:cls.getRecordComponents()) {
                keys.add(field.getName());
                if(cls==Definitions.Line.class && !field.getName().equals("text") && (!object.has(field.getName())||object.get(field.getName()).isJsonNull()))continue;
                validate(object.get(field.getName()),field.getGenericType(),path+"."+field.getName());
            }
            JsonValidation.fields(object,keys); return;
        }
        if (!value.isJsonPrimitive()) throw new IllegalArgumentException("Valor inválido: "+path);
        var p=value.getAsJsonPrimitive();
        boolean valid = cls == String.class || cls.isEnum() ? p.isString() : cls==boolean.class ? p.isBoolean() : p.isNumber();
        if (!valid || cls==int.class && !value.toString().matches("-?[0-9]{1,9}"))throw new IllegalArgumentException("Tipo inválido: "+path);
    }
    public void save(Definitions.Holograms holograms) throws IOException {
        var file=directory.resolve("holograms.json");
        var temporary=Files.createTempFile(directory,".holograms-",".tmp");
        try {
            Files.writeString(temporary,JSON.toJson(holograms)+"\n");
            Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
}
