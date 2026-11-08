package com.shirokai.core.scoreboard;

import com.google.gson.Gson;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.styles.*;
import com.shirokai.core.tab.TabConfig.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScoreboardPlanTest {
    @TempDir Path directory;
    @Test void strictConfigurationPreservesExistingFilesAndRejectsInvalidLimits() throws Exception {
        var file = directory.resolve("scoreboard.json"); assertEquals(ScoreboardConfig.defaults(), ScoreboardConfig.load(file));
        String valid = Files.readString(file);
        for (String bad : List.of("null", "[]", valid.replace("\"updateTicks\": 20", "\"updateTicks\": \"20\""), valid.replace("\"updateTicks\": 20", "\"updateTicks\": 4"), valid.replace("\"title\":", "\"unknown\":"))) assertThrows(Exception.class, () -> ScoreboardConfig.parse(bad));
        var lines = new ArrayList<ScoreboardConfig.Line>();
        for (int i = 0; i < 16; i++) lines.add(new ScoreboardConfig.Line("line_" + i, Condition.any(), ""));
        assertThrows(IllegalArgumentException.class, () -> new ScoreboardConfig.Layout("default", 0, Condition.any(), "", lines));
        assertThrows(IllegalArgumentException.class, () -> new ScoreboardConfig.Layout("default", 0, Condition.any(), "", List.of(lines.getFirst(), lines.getFirst())));
        Files.writeString(file, "broken"); assertThrows(Exception.class, () -> ScoreboardConfig.load(file)); assertEquals("broken", Files.readString(file));
    }
    @Test void layoutMatchesAllConditionsAndUsesStablePriority() {
        var base = ScoreboardConfig.defaults(); var layouts = new ArrayList<>(base.layouts());
        layouts.add(new ScoreboardConfig.Layout("vip", 100, new Condition("sidebar.vip", "VIP", Set.of("minecraft:overworld")), "VIP", List.of()));
        var plan = new ScoreboardPlan(new ScoreboardConfig(1, 20, layouts), new StyleEngine(StyleConfig.defaults()));
        var meta = new PermissionService.Metadata("vip", "", "", 0);
        assertEquals("vip", plan.layout(meta, "minecraft:overworld", node -> true).definition().id());
        assertEquals("default", plan.layout(meta, "minecraft:overworld", node -> false).definition().id());
        assertEquals("default", plan.layout(meta, "minecraft:the_end", node -> true).definition().id());
    }
    @Test void allTemplatesCompileBeforeReloadAndRequireAnUnconditionalDefault() {
        var styles = new StyleEngine(StyleConfig.defaults());
        for (String bad : List.of("%animation:missing%", "<click:run_command:/op>x</click>", "x".repeat(129))) {
            var config = new ScoreboardConfig(1, 20, List.of(new ScoreboardConfig.Layout("default", 0, Condition.any(), bad, List.of())));
            assertThrows(IllegalArgumentException.class, () -> new ScoreboardPlan(config, styles));
        }
        assertThrows(IllegalArgumentException.class, () -> new ScoreboardPlan(new ScoreboardConfig(1, 20, List.of(new ScoreboardConfig.Layout("default", 0, Condition.any(), "ok", List.of(new ScoreboardConfig.Line("long", Condition.any(), "x".repeat(257)))))), styles));
        assertThrows(IllegalArgumentException.class, () -> new ScoreboardConfig(1, 20, List.of(new ScoreboardConfig.Layout("vip", 0, new Condition("vip", null, Set.of()), "", List.of()))));
    }
}
