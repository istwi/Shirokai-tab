package com.shirokai.core.administration;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.shirokai.core.CoreRuntime;
import com.shirokai.core.placeholders.PlaceholderService;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import static net.minecraft.server.command.CommandManager.*;

public final class AdminCommands {
    private final Supplier<CoreRuntime> runtime;
    private final String version;
    public AdminCommands(Supplier<CoreRuntime> runtime, String version) { this.runtime = runtime; this.version = version; }
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (dispatcher.getRoot().getChild("shirokaicore") != null)
            throw new IllegalStateException("Conflito: /shirokaicore já registrado; nenhum comando substituído");
        var root = literal("shirokaicore").requires(this::canSeeAny)
            .executes(context -> status(context.getSource()));
        root.then(literal("status").requires(s -> allowed(s, "status")).executes(c -> status(c.getSource())));
        root.then(literal("version").requires(s -> allowed(s, "version")).executes(c -> feedback(c.getSource(), "Shirokai Core " + version)));
        root.then(literal("modules").requires(s -> allowed(s, "modules")).executes(c -> feedback(c.getSource(), runtime.get().modules().states().toString())));
        root.then(literal("permissions").requires(s -> allowed(s, "permissions")).executes(c -> feedback(c.getSource(), "Provider: " + runtime.get().permissions().provider())));
        root.then(literal("reload").requires(s -> allowed(s, "reload")).executes(c -> {
            var source = c.getSource(); var core = runtime.get();
            return respond(source, core.reload().thenApply(v -> "Configuração recarregada; estados ativos preservados"), "reload");
        }));
        root.then(literal("debug").requires(s -> allowed(s, "debug")).executes(c -> {
            var core = runtime.get();
            try { return respond(c.getSource(), core.storage().integrityCheck().thenApply(v -> "SQLite integrity_check=" + v), null); }
            catch (RuntimeException failure) { return error(c.getSource(), failure); }
        }));
        root.then(literal("backup").requires(s -> allowed(s, "backup")).executes(c -> {
            try {
                var core = runtime.get();
                var path = core.dataDirectory().resolve("backups/backup-" + Instant.now().toEpochMilli() + "-" + java.util.UUID.randomUUID() + ".db");
                return respond(c.getSource(), core.storage().backup(path).thenApply(v -> "Backup criado: " + v.getFileName()), "backup");
            } catch (RuntimeException failure) { return error(c.getSource(), failure); }
        }));
        root.then(literal("placeholders").requires(s -> allowed(s, "placeholders"))
            .executes(c -> feedback(c.getSource(), "Nativos: " + runtime.get().placeholders().keys()))
            .then(argument("template", StringArgumentType.greedyString()).executes(c -> {
                var source = c.getSource(); var core = runtime.get();
                if (!core.running("placeholders")) return feedback(source, "Módulo placeholders desativado");
                var player = source.getPlayer();
                var context = new PlaceholderService.Context(player == null ? null : player.getUuid(),
                        player == null ? null : player.getGameProfile().getName(), source.getServer().getCurrentPlayerCount());
                return feedback(source, core.placeholders().render(StringArgumentType.getString(c, "template"), context, core.config().unavailablePlaceholder()));
            })));
        root.then(literal("tab").requires(s->allowed(s,"tab.inspect")).executes(c->feedback(c.getSource(),runtime.get().tab().status())));
        root.then(literal("integrations").requires(s->allowed(s,"integrations.inspect")).executes(c->feedback(c.getSource(),runtime.get().integrations().status())));
        root.then(literal("scoreboard").requires(s->allowed(s,"scoreboard.inspect")).executes(c->feedback(c.getSource(),runtime.get().scoreboard().status())));
        root.then(new ChatAdminCommands(runtime,this::allowed).node());
        root.then(new ClanAdminCommands(runtime,this::allowed).node());
        root.then(new TitleAdminCommands(runtime, this::allowed).node());
        dispatcher.register(root);
    }
    private boolean canSeeAny(ServerCommandSource source) {
        for (String node : new String[]{"status", "version", "modules", "permissions", "reload", "debug", "backup", "placeholders", "titles.inspect", "titles.grant", "titles.revoke", "chat.mute", "chat.unmute", "chat.inspect", "tab.inspect", "scoreboard.inspect", "integrations.inspect", "clans.inspect", "clans.identity"})
            if (allowed(source, node)) return true;
        return false;
    }
    private boolean allowed(ServerCommandSource source, String action) {
        var core = runtime.get();
        boolean console = source.getPlayer() == null && source.hasPermissionLevel(4);
        if (core == null || !core.running("administration"))
            return console && (action.equals("status") || action.equals("version"));
        if (action.equals("placeholders") && !core.running("placeholders")) return false;
        if (action.startsWith("titles.") && !core.running("titles")) return false;
        if(action.startsWith("clans.")&&!core.running("clans"))return false;
        if(action.startsWith("chat.")&&!core.running("chat"))return false;
        if(action.startsWith("tab.")&&!core.running("tab"))return false;
        if(action.startsWith("scoreboard.")&&!core.running("scoreboard"))return false;
        if(action.startsWith("integrations.")&&!core.running("integrations"))return false;
        if (console) return true;
        if (core.permissions().provider().startsWith("vanilla-op4")) return source.hasPermissionLevel(4);
        return core.permissions().allowed(source.getPlayer().getUuid(), "shirokaicore.command." + action);
    }
    private int status(ServerCommandSource source) {
        if (!allowed(source, "status")) return feedback(source, "Sem permissão para status");
        var core = runtime.get();
        if (core == null) return feedback(source, "Shirokai Core indisponível; consulte o log de inicialização");
        return feedback(source, "Shirokai Core " + version + " | SQLite=" + core.storageStatus() + " | " + core.modules().states());
    }
    private int respond(ServerCommandSource source, CompletableFuture<String> operation, String auditAction) {
        // Source identity is captured on the server thread; async JDBC never accesses game objects.
        var actor = source.getPlayer() == null ? null : source.getPlayer().getUuid();
        var server = source.getServer(); var core = runtime.get();
        feedback(source, "Operação iniciada");
        operation.whenComplete((message, failure) -> server.execute(() -> {
            if (failure == null && auditAction != null && core != null && core.running("storage")) {
                core.storage().audit(actor, auditAction).whenComplete((v, auditError) -> {
                    if (auditError != null) org.slf4j.LoggerFactory.getLogger("shirokai-core").error("[administration] Falha ao registrar auditoria", auditError);
                });
            }
            if (failure == null) feedback(source, message); else error(source, failure);
        }));
        return 1;
    }
    private static int feedback(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message), false); return 1;
    }
    private static int error(ServerCommandSource source, Throwable failure) {
        while (failure.getCause() != null) failure = failure.getCause();
        source.sendError(Text.literal("Shirokai Core: " + failure.getMessage())); return 0;
    }
}
