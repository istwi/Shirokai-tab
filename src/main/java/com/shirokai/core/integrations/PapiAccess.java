package com.shirokai.core.integrations;

import com.shirokai.core.placeholders.PlaceholderService;
import eu.pb4.placeholders.api.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import java.util.*;
import java.util.function.Supplier;

/** Text Placeholder API 2.4.2+1.21, isolated optional linkage. Values cross the bridge as plain strings. */
public final class PapiAccess implements PlaceholderBridge {
    private final MinecraftServer server;
    private final Supplier<String> fallback;
    private final Logger logger;
    private final Map<Identifier, PlaceholderHandler> owned = new HashMap<>();
    public PapiAccess(MinecraftServer server, Supplier<String> fallback, Logger logger) { this.server = server; this.fallback = fallback; this.logger = logger; }
    @Override public Optional<String> resolve(String key, PlaceholderService.Context context) {
        if (!server.isOnThread() || key.startsWith("shirokai:")) return Optional.empty();
        int split = key.indexOf(' '); var id = Identifier.tryParse(split < 0 ? key : key.substring(0, split));
        if (id == null || !Placeholders.getPlaceholders().containsKey(id)) return Optional.empty();
        var player = context.playerId() == null ? null : server.getPlayerManager().getPlayer(context.playerId());
        if (context.playerId() != null && player == null) return Optional.empty();
        var result = Placeholders.parsePlaceholder(id, split < 0 ? null : key.substring(split + 1), player == null ? PlaceholderContext.of(server) : PlaceholderContext.of(player));
        return result.isValid() ? Optional.of(result.text().getString()) : Optional.empty();
    }
    @Override public void export(PlaceholderService service) {
        for (String key : service.keys()) {
            if (!key.startsWith("shirokai:")) continue;
            var id = Identifier.of(key);
            if (Placeholders.getPlaceholders().containsKey(id)) { logger.warn("[integrations] Placeholder {} externo preservado", id); continue; }
            PlaceholderHandler handler = (context, argument) -> {
                if (context == null || context.server() != server || !server.isOnThread() || argument != null && !argument.isBlank()) return PlaceholderResult.invalid();
                var player = context.player();
                var nativeContext = new PlaceholderService.Context(player == null ? null : player.getUuid(), player == null ? null : player.getGameProfile().getName(), server.getCurrentPlayerCount());
                return PlaceholderResult.value(Text.literal(service.resolve(key, nativeContext).orElseGet(fallback)));
            };
            owned.put(id, handler); Placeholders.register(id, handler);
        }
    }
    @Override public void close() {
        for (var entry : owned.entrySet()) if (Placeholders.getPlaceholders().get(entry.getKey()) == entry.getValue()) Placeholders.remove(entry.getKey());
        owned.clear();
    }
}
