package com.shirokai.core.integrations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationConfigTest {
    @TempDir Path directory;
    @Test void defaultsPreserveFilesAndRejectCurrencyMixupsAndTypes() throws Exception {
        var file = directory.resolve("integrations.json"); assertEquals(IntegrationConfig.defaults(), IntegrationConfig.load(file));
        String source = Files.readString(file);
        for (String invalid : List.of("null", "[]", source.replace("\"impactor\": true", "\"impactor\": \"true\""), source.replace("impactor:tokens", "impactor:dollars"), source.replace("\"refreshTicks\": 100", "\"refreshTicks\": 0"), source.replace("\"pokedexProvider\": \"cobblemon\"", "\"pokedexProvider\": \"auto\""))) assertThrows(Exception.class, () -> IntegrationConfig.parse(invalid));
        Files.writeString(file, "invalid"); assertThrows(Exception.class, () -> IntegrationConfig.load(file)); assertEquals("invalid", Files.readString(file));
    }
    @Test void enablementRequiresRestartButDataMappingCanReload() throws Exception {
        var file = directory.resolve("integrations.json"); IntegrationConfig.load(file); var text = Files.readString(file);
        var defaults = IntegrationConfig.defaults();
        assertTrue(IntegrationConfig.parse(text.replace("\"impactor\": true", "\"impactor\": false")).requiresRestartComparedTo(defaults));
        assertFalse(IntegrationConfig.parse(text.replace("impactor:dollars", "impactor:other")).requiresRestartComparedTo(defaults));
        assertFalse(IntegrationConfig.parse(text.replace("\"pokedexProvider\": \"cobblemon\"", "\"pokedexProvider\": \"shirokaidex\"")).requiresRestartComparedTo(defaults));
    }
    @Test void progressUsesRealDenominatorAndRejectsUnknownTotals() {
        assertEquals("0.11", ProgressValues.percentage(1, 905));
        assertEquals("100", ProgressValues.percentage(905, 905));
        assertEquals("0", ProgressValues.percentage(0, 905));
        assertThrows(IllegalArgumentException.class, () -> ProgressValues.percentage(0, 0));
        assertThrows(IllegalArgumentException.class, () -> ProgressValues.percentage(906, 905));
    }
}
