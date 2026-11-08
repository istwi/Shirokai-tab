package com.shirokai.core.scoreboard;

import com.google.gson.*;
import com.shirokai.core.styles.JsonValidation;
import com.shirokai.core.tab.TabConfig.Condition;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public record ScoreboardConfig(int schemaVersion, int updateTicks, List<Layout> layouts) {
    public record Line(String id, Condition condition, String text) {
        public Line { validId(id); Objects.requireNonNull(condition); Objects.requireNonNull(text); }
    }
    public record Layout(String id, int priority, Condition condition, String title, List<Line> lines) {
        public Layout {
            validId(id); Objects.requireNonNull(condition); Objects.requireNonNull(title); lines = List.copyOf(lines);
            if (priority < -1000000 || priority > 1000000 || lines.size() > 15) throw new IllegalArgumentException("Scoreboard: máximo de 15 linhas e prioridade ±1000000");
            var ids = new HashSet<String>();
            for (var line : lines) if (!ids.add(line.id())) throw new IllegalArgumentException("Linha duplicada: " + line.id());
        }
    }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static void validId(String id) { if (id == null || !id.matches("[a-z][a-z0-9_-]{0,63}")) throw new IllegalArgumentException("ID de scoreboard inválido"); }
    public ScoreboardConfig {
        layouts = List.copyOf(layouts);
        if (schemaVersion != 1 || updateTicks < 5 || updateTicks > 1200 || layouts.isEmpty() || layouts.size() > 50) throw new IllegalArgumentException("Configuração de scoreboard inválida");
        var ids = new HashSet<String>();
        for (var layout : layouts) if (!ids.add(layout.id())) throw new IllegalArgumentException("Layout duplicado");
        if (layouts.stream().noneMatch(l -> l.id().equals("default") && l.condition().equals(Condition.any()))) throw new IllegalArgumentException("Layout default sem condições é obrigatório");
    }
    public static ScoreboardConfig defaults() {
        return new ScoreboardConfig(1, 20, List.of(new Layout("default", 0, Condition.any(), "%animation:gold_wave%", List.of(
            new Line("title", Condition.any(), "<gold>Título: %shirokai:selected_title%</gold>"),
            new Line("clan", Condition.any(), "<gray>Clã: %shirokai:clan_tag%</gray>"),
            new Line("online", Condition.any(), "<aqua>Online: %shirokai:online%</aqua>"),
            new Line("ping", Condition.any(), "<gray>Ping: %shirokai:ping%ms</gray>"),
            new Line("dollars", Condition.any(), "<gray>PokéDollars: %shirokai:dollars%</gray>")))));
    }
    public static ScoreboardConfig load(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) try { Files.writeString(file, GSON.toJson(defaults()) + "\n", StandardOpenOption.CREATE_NEW); } catch (FileAlreadyExistsException ignored) { }
        if (Files.size(file) > 262144) throw new IOException("scoreboard.json excede 256KiB");
        try { return parse(Files.readString(file)); } catch (Exception failure) { throw new IOException(file + ": " + failure.getMessage(), failure); }
    }
    private static void condition(JsonObject object) {
        if (!object.has("condition") || !object.get("condition").isJsonObject()) throw new IllegalArgumentException("condition deve ser objeto");
        var c = object.getAsJsonObject("condition"); JsonValidation.fields(c, Set.of("permission", "group", "worlds"));
        JsonValidation.string(c, "permission", true); JsonValidation.string(c, "group", true); JsonValidation.strings(c, "worlds");
    }
    public static ScoreboardConfig parse(String input) throws IOException {
        var root = JsonValidation.object(input); JsonValidation.fields(root, Set.of("schemaVersion", "updateTicks", "layouts"));
        JsonValidation.integer(root, "schemaVersion"); JsonValidation.integer(root, "updateTicks");
        if (!root.has("layouts") || !root.get("layouts").isJsonArray()) throw new IllegalArgumentException("layouts deve ser lista");
        for (var item : root.getAsJsonArray("layouts")) {
            if (!item.isJsonObject()) throw new IllegalArgumentException("Layout deve ser objeto");
            var l = item.getAsJsonObject(); JsonValidation.fields(l, Set.of("id", "priority", "condition", "title", "lines"));
            JsonValidation.string(l, "id", false); JsonValidation.string(l, "title", false); JsonValidation.integer(l, "priority"); condition(l);
            if (!l.has("lines") || !l.get("lines").isJsonArray()) throw new IllegalArgumentException("lines deve ser lista");
            for (var row : l.getAsJsonArray("lines")) {
                if (!row.isJsonObject()) throw new IllegalArgumentException("Linha deve ser objeto");
                var line = row.getAsJsonObject(); JsonValidation.fields(line, Set.of("id", "condition", "text"));
                JsonValidation.string(line, "id", false); JsonValidation.string(line, "text", false); condition(line);
            }
        }
        return GSON.fromJson(root, ScoreboardConfig.class);
    }
}
