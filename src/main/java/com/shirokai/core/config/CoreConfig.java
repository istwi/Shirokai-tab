package com.shirokai.core.config;

import java.util.Map;
import java.util.Set;

public record CoreConfig(int schemaVersion, Map<String, Boolean> modules,
                         String databaseFile, String unavailablePlaceholder) {
    public static final Set<String> MODULES = Set.of("storage", "permissions", "placeholders", "administration", "titles", "profiles", "clans", "chat", "styles", "tab", "scoreboard", "integrations");
    public CoreConfig { modules = Map.copyOf(modules); }
    public static CoreConfig defaults() {
        return new CoreConfig(1, MODULES.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(id -> id, id -> true)), "shirokai.db", "N/D");
    }
    public boolean enabled(String id) { return modules.getOrDefault(id, false); }
    public boolean requiresRestartComparedTo(CoreConfig previous) {
        return !modules.equals(previous.modules) || !databaseFile.equals(previous.databaseFile);
    }
}
