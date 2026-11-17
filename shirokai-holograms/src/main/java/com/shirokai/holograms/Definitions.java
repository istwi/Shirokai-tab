package com.shirokai.holograms;

import java.util.*;
import net.minecraft.util.Identifier;

/** Immutable validated schema; all sizes and timing values are bounded before publication. */
public final class Definitions {
    private Definitions() { }
    public enum Alignment { LEFT, CENTER, RIGHT }
    public enum Animation { STATIC, MARQUEE_LEFT, MARQUEE_RIGHT, SLIDE_LEFT, SLIDE_RIGHT, TYPEWRITER, FADE_IN, FADE_OUT, BOUNCE, PING_PONG }
    public enum Billboard { FIXED, VERTICAL, HORIZONTAL, CENTER }
    public record Settings(int schemaVersion, boolean enabled, int updateTicks, int placeholderTicks,
                           int viewDistance, int maxHolograms, int maxLines, String unavailablePlaceholder) {
        public Settings {
            require(schemaVersion == 1 && updateTicks >= 1 && updateTicks <= 20 && placeholderTicks >= 5 && placeholderTicks <= 1200
                && viewDistance >= 4 && viewDistance <= 128 && maxHolograms >= 1 && maxHolograms <= 256
                && maxLines >= 1 && maxLines <= 40, "config.json: limites/intervalos inválidos");
            require(unavailablePlaceholder != null && unavailablePlaceholder.length() <= 128 && clean(unavailablePlaceholder), "Fallback inválido");
        }
        public static Settings defaults() { return new Settings(1, true, 2, 20, 48, 128, 20, "N/D"); }
    }
    public record Motion(Animation type, double speed, int durationTicks, int intervalTicks, int repetitions, int pauseTicks, double width) {
        public Motion {
            require(type != null && Double.isFinite(speed) && speed >= .1 && speed <= 10 && durationTicks >= 5 && durationTicks <= 72000
                && intervalTicks >= 1 && intervalTicks <= 100 && repetitions >= 0 && repetitions <= 10000
                && pauseTicks >= 0 && pauseTicks <= 72000 && Double.isFinite(width) && width >= 1 && width <= 2048, "Animação inválida");
        }
        public static Motion defaults(Animation type) { return new Motion(type, 1, 80, 2, 0, 20, 160); }
    }
    public record Animations(int schemaVersion, Map<String, Motion> presets) {
        public Animations {
            require(schemaVersion == 1 && presets != null && !presets.isEmpty() && presets.size() <= 100, "animations.json inválido");
            presets.forEach((id, value) -> require(id != null && id.matches("[A-Za-z0-9_-]{1,48}") && value != null, "Preset inválido"));
            presets = Map.copyOf(presets);
            for (var type : Animation.values()) require(presets.containsKey(type.name()), "Preset obrigatório ausente: " + type);
        }
        public static Animations defaults() {
            var map = new LinkedHashMap<String, Motion>();
            for (var type : Animation.values()) map.put(type.name(), Motion.defaults(type));
            return new Animations(1, map);
        }
    }
    public record Line(String text, Alignment alignment, String animation) {
        public Line {
            require(text != null && text.length() <= 512 && clean(text), "Linha inválida: até 512 caracteres sem controles");
            animation = animation == null ? "STATIC" : animation;
            require(animation.matches("[A-Za-z0-9_-]{1,48}"), "Preset de linha inválido");
        }
    }
    public record Hologram(String id, String world, double x, double y, double z, Alignment alignment, List<Line> lines,
                           double lineSpacing, float scale, int backgroundColor, int backgroundOpacity,
                           boolean shadow, float yaw, float pitch, Billboard billboard) {
        public Hologram {
            require(validId(id), "ID inválido: use a-z, 0-9, _ e - (1..48)");
            require(world != null && Identifier.tryParse(world) != null, "Dimensão inválida");
            require(Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && Math.abs(x) <= 30_000_000
                && Math.abs(z) <= 30_000_000 && Math.abs(y) <= 2048, "Posição inválida");
            require(alignment != null && billboard != null, "alignment/billboard inválido");
            require(lines != null && !lines.isEmpty() && lines.size() <= 40 && lines.stream().allMatch(Objects::nonNull)
                && lines.stream().mapToInt(line -> line.text().length()).sum() <= 8192, "Holograma exige 1..40 linhas (até 8192 caracteres)");
            lines = List.copyOf(lines);
            require(Double.isFinite(lineSpacing) && lineSpacing >= 9 && lineSpacing <= 100 && Float.isFinite(scale) && scale >= .25 && scale <= 4,
                "lineSpacing deve ser 9..100 pixels e scale .25..4");
            require(backgroundColor >= 0 && backgroundColor <= 0xffffff && backgroundOpacity >= 0 && backgroundOpacity <= 255
                && Float.isFinite(yaw) && Math.abs(yaw) <= 360 && Float.isFinite(pitch) && Math.abs(pitch) <= 90, "Cor/opacidade/rotação inválida");
        }
        public Hologram withLines(List<Line> value) { return new Hologram(id,world,x,y,z,alignment,value,lineSpacing,scale,backgroundColor,backgroundOpacity,shadow,yaw,pitch,billboard); }
        public Hologram aligned(Alignment value) { return new Hologram(id,world,x,y,z,value,lines,lineSpacing,scale,backgroundColor,backgroundOpacity,shadow,yaw,pitch,billboard); }
        public Hologram scaled(float value) { return new Hologram(id,world,x,y,z,alignment,lines,lineSpacing,value,backgroundColor,backgroundOpacity,shadow,yaw,pitch,billboard); }
        public Hologram moved(String dimension,double px,double py,double pz) { return new Hologram(id,dimension,px,py,pz,alignment,lines,lineSpacing,scale,backgroundColor,backgroundOpacity,shadow,yaw,pitch,billboard); }
        public static Hologram create(String id,String world,double x,double y,double z) {
            return new Hologram(id,world,x,y,z,Alignment.CENTER,List.of(new Line("<gold><bold>Shirokai</bold></gold>",null,"STATIC")),12,1,0,0,true,0,0,Billboard.CENTER);
        }
    }
    public record Holograms(int schemaVersion, List<Hologram> holograms) {
        public Holograms {
            require(schemaVersion == 1 && holograms != null && holograms.size() <= 256, "holograms.json inválido");
            var ids = new HashSet<String>();
            for (var hologram : holograms) require(hologram != null && ids.add(hologram.id()), "ID duplicado/nulo em holograms.json");
            holograms = List.copyOf(holograms);
        }
        public static Holograms defaults() { return new Holograms(1,List.of()); }
    }
    public static boolean validId(String id) { return id != null && id.matches("[a-z0-9_-]{1,48}"); }
    private static boolean clean(String value) { return value.codePoints().noneMatch(Character::isISOControl); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
}
