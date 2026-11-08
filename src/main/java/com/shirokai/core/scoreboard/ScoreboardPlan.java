package com.shirokai.core.scoreboard;

import com.shirokai.core.styles.StyleEngine;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.tab.TabPlan;
import java.util.*;
import java.util.function.Predicate;

public final class ScoreboardPlan {
    public record Line(ScoreboardConfig.Line definition, StyleEngine.Compiled text) { }
    public record Layout(ScoreboardConfig.Layout definition, StyleEngine.Compiled title, List<Line> lines) { }
    private final ScoreboardConfig config;
    private final StyleEngine styles;
    private final List<Layout> layouts;
    public ScoreboardPlan(ScoreboardConfig config, StyleEngine styles) {
        this.config = config; this.styles = styles;
        layouts = config.layouts().stream().sorted(Comparator.comparingInt(ScoreboardConfig.Layout::priority).reversed().thenComparing(ScoreboardConfig.Layout::id))
            .map(l -> new Layout(l, styles.compile(l.title()), l.lines().stream().map(r -> new Line(r, styles.compile(r.text()))).toList())).toList();
        for (var l : layouts) {
            if (styles.literalSize(l.title()) > 128 || l.lines().stream().anyMatch(r -> styles.literalSize(r.text()) > 256)) throw new IllegalArgumentException("Scoreboard: título excede 128 ou linha excede 256 caracteres");
        }
    }
    public Layout layout(PermissionService.Metadata metadata, String world, Predicate<String> permissions) {
        return layouts.stream().filter(l -> TabPlan.matches(l.definition().condition(), metadata, world, permissions)).findFirst().orElseThrow();
    }
    public ScoreboardConfig config() { return config; }
    public StyleEngine styles() { return styles; }
}
