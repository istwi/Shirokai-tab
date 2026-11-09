package com.shirokai.core.customization;

import com.google.gson.*;
import com.shirokai.core.presentation.Surface;
import com.shirokai.core.styles.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

/** Administrator-owned, fully compiled catalog. Player input is always an ID. */
public final class PresetCatalog {
    public static final Set<Surface> SURFACES = Set.of(Surface.TITLE, Surface.CLAN_NAME, Surface.CLAN_TAG);
    public record Definition(String permission, Set<Surface> surfaces, String template,List<String> frames,int frameTicks) {
        public Definition(String permission,Set<Surface> surfaces,String template){this(permission,surfaces,template,List.of(),20);}
        public Definition {
            surfaces = Set.copyOf(surfaces);frames=frames==null?List.of():List.copyOf(frames);
            if(frameTicks<5||frameTicks>1200||frames.size()>100)throw new IllegalArgumentException("Frames de preset inválidos");
            if (permission == null || !permission.matches("[a-zA-Z0-9_.-]{0,128}") ||
                surfaces.isEmpty() || !SURFACES.containsAll(surfaces)) throw new IllegalArgumentException("Preset inválido");
        }
    }
    private record Document(int schemaVersion, String fallback, Map<String,Definition> presets) {}
    public record Preset(Definition definition, StyleEngine.Compiled template,List<StyleEngine.Compiled> frames) {}
    public enum Status { ADMINISTRATIVE, ACTIVE, UNAVAILABLE, REMOVED }
    public record Resolution(Status status, String saved, StyleEngine.Compiled template) {}
    private final Map<String,Preset> presets;
    private final String fallback;
    private final StyleEngine styles;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public PresetCatalog(String fallback, Map<String,Definition> definitions, StyleEngine styles) {
        if (definitions.isEmpty() || definitions.size() > 100) throw new IllegalArgumentException("Catálogo exige 1..100 presets");
        var compiled = new TreeMap<String,Preset>();
        definitions.forEach((id,definition) -> {
            if (!id.matches("[a-z][a-z0-9_-]{0,63}") || id.equals("padrao")) throw new IllegalArgumentException("ID de preset inválido");
            var template = styles.compile(definition.template());
            long occurrences = count(template.nodes());
            if (!template.variables().equals(Set.of("visual:value")) || occurrences != 1 ||
                !template.animations().isEmpty() || styles.literalSize(template) > 128)
                throw new IllegalArgumentException("Preset exige um único %visual:value%, sem animação");
            var frames=new ArrayList<StyleEngine.Compiled>();
            for(String source:definition.frames()){var frame=styles.compile(source);if(!frame.variables().equals(Set.of("visual:value"))||count(frame.nodes())!=1||!frame.animations().isEmpty()||styles.literalSize(frame)>128)throw new IllegalArgumentException("Frame exige um único %visual:value%, sem animação aninhada");frames.add(frame);}
            compiled.put(id,new Preset(definition,template,List.copyOf(frames)));
        });
        var basic = compiled.get(fallback);
        if (basic == null || !basic.frames().isEmpty() || !basic.definition().permission().isEmpty() ||
            !basic.definition().surfaces().containsAll(SURFACES) ||
            basic.definition().template().contains("<#") || basic.definition().template().contains("<gradient:"))
            throw new IllegalArgumentException("Fallback deve ser básico, gratuito e cobrir todas as superfícies");
        this.presets = Collections.unmodifiableMap(compiled); this.fallback = fallback; this.styles = styles;
    }
    private static long count(List<StyleEngine.Node> nodes) {
        long result=0;
        for (var node:nodes) {
            if (node instanceof StyleEngine.Variable v && v.name().equals("visual:value")) result++;
            if (node instanceof StyleEngine.Group g) result+=count(g.children());
        }
        return result;
    }
    public boolean hasAnimations(){return presets.values().stream().anyMatch(p->!p.frames().isEmpty());}
    public Map<String,Preset> presets() { return presets; }
    public StyleEngine styles() { return styles; }
    public boolean allowed(String id, Surface surface, Predicate<String> permission) {
        var preset=presets.get(id);
        if (preset==null || !preset.definition().surfaces().contains(surface)) return false;
        var def=preset.definition();
        if (!def.permission().isEmpty() && !permission.test(def.permission())) return false;
        if(!def.frames().isEmpty()&&!permission.test(surface==Surface.TITLE?"shirokaicore.style.animation":"shirokaicore.clan.animation"))return false;
        String sources=def.template()+String.join("",def.frames());
        if (surface!=Surface.TITLE) {
            if (sources.contains("<gradient:") && !permission.test("shirokaicore.clan.color.gradient")) return false;
            if (sources.contains("<#") && !permission.test("shirokaicore.clan.color.hex")) return false;
        }
        return true;
    }
    public net.minecraft.text.Text render(Resolution resolution,String literal,long tick,boolean animate){
        if(resolution.template()==null)return net.minecraft.text.Text.literal(literal);
        var preset=presets.get(resolution.status()==Status.ACTIVE?resolution.saved():fallback);
        var template=animate&&!preset.frames().isEmpty()?preset.frames().get((int)Math.floorMod(tick/preset.definition().frameTicks(),preset.frames().size())):preset.template();
        return styles.render(template,key->literal,0,false);
    }
    public Resolution resolve(String id, Surface surface, Predicate<String> permission) {
        if (id==null) return new Resolution(Status.ADMINISTRATIVE,null,null);
        Status status=!presets.containsKey(id) || !presets.get(id).definition().surfaces().contains(surface)
            ? Status.REMOVED : allowed(id,surface,permission) ? Status.ACTIVE : Status.UNAVAILABLE;
        return new Resolution(status,id,presets.get(status==Status.ACTIVE?id:fallback).template());
    }
    public static PresetCatalog load(Path file, StyleEngine styles) throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            var all=SURFACES;
            var doc=new Document(1,"basic",Map.of(
                "basic",new Definition("",all,"<aqua>%visual:value%</aqua>"),
                "gold",new Definition("shirokaicore.style.gold",all,"<gold><bold>%visual:value%</bold></gold>"),
                "sunset",new Definition("shirokaicore.style.sunset",all,"<gradient:#ff7733:#ffccff>%visual:value%</gradient>")));
            try { Files.writeString(file,GSON.toJson(doc)+"\n",StandardOpenOption.CREATE_NEW); }
            catch (FileAlreadyExistsException ignored) {}
        }
        if (Files.size(file)>262144) throw new IOException("presets.json excede256KiB");
        try { return parse(Files.readString(file),styles); }
        catch (Exception error) { throw new IOException(file+": "+error.getMessage(),error); }
    }
    public static PresetCatalog parse(String text,StyleEngine styles) throws IOException {
        var root=JsonValidation.object(text);
        JsonValidation.fields(root,Set.of("schemaVersion","fallback","presets"));
        JsonValidation.integer(root,"schemaVersion"); JsonValidation.string(root,"fallback",false);
        if (!root.has("presets") || !root.get("presets").isJsonObject()) throw new IllegalArgumentException("presets deve ser objeto");
        for (var entry:root.getAsJsonObject("presets").entrySet()) {
            if (!entry.getValue().isJsonObject()) throw new IllegalArgumentException("Preset deve ser objeto");
            var p=entry.getValue().getAsJsonObject(); JsonValidation.fields(p,Set.of("permission","surfaces","template","frames","frameTicks"));
            if(p.has("frames"))JsonValidation.strings(p,"frames");if(p.has("frameTicks"))JsonValidation.integer(p,"frameTicks");else p.addProperty("frameTicks",20);
            JsonValidation.string(p,"permission",false);JsonValidation.string(p,"template",false);JsonValidation.strings(p,"surfaces");
            for(var s:p.getAsJsonArray("surfaces")) Surface.valueOf(s.getAsString());
        }
        var document=GSON.fromJson(root,Document.class);
        if(document.schemaVersion()!=1)throw new IllegalArgumentException("Schema de presets incompatível");
        return new PresetCatalog(document.fallback(),document.presets(),styles);
    }
}
