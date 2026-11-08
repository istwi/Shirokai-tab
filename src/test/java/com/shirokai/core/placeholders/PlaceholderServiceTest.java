package com.shirokai.core.placeholders;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PlaceholderServiceTest {
    private final PlaceholderService service = new PlaceholderService((key, error) -> fail(error));
    @Test void usesRealSnapshotValuesAndFallbackForUnavailableData() {
        UUID id = UUID.randomUUID(); var context = new PlaceholderService.Context(id, "Ash", 7);
        assertEquals("Ash/7/" + id + "/N/D", service.render("%shirokai:player%/%shirokai:online%/%shirokai:uuid%/%shirokai:dollars%", context, "N/D"));
        assertEquals("?", service.render("%shirokai:player%", new PlaceholderService.Context(null, null, 0), "?"));
    }
    @Test void replacementIsLiteralNonrecursiveAndDoesNotInterpretUserMarkup() {
        String malicious = "$1\\%shirokai:online%<click:run_command:'/op x'>";
        assertEquals(malicious, service.render("%shirokai:player%", new PlaceholderService.Context(null, malicious, 99), "?"));
        assertThrows(IllegalArgumentException.class, () -> service.render("x".repeat(8193), new PlaceholderService.Context(null, null, 0), "?"));
    }
    @org.junit.jupiter.api.Test void externalArgumentsAreLiteralAndRecursiveProvidersCannotLoop() {
        var service = new PlaceholderService((key, error) -> {});
        var context = new PlaceholderService.Context(java.util.UUID.randomUUID(), "Test", 1);
        service.external((key, c) -> key.equals("player:objective gyms") ? java.util.Optional.of("<click:run_command:/op>literal</click>") : service.resolve(key, c));
        org.junit.jupiter.api.Assertions.assertEquals("<click:run_command:/op>literal</click>", service.render("%player:objective gyms%", context, "N/D"));
        org.junit.jupiter.api.Assertions.assertEquals("N/D", service.render("%missing:value%", context, "N/D"));
        service.register("shirokai:recursive", c -> service.resolve("shirokai:recursive", c));
        org.junit.jupiter.api.Assertions.assertEquals("N/D", service.render("%shirokai:recursive%", context, "N/D"));
    }
}
