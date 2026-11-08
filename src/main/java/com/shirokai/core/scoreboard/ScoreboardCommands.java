package com.shirokai.core.scoreboard;

import com.mojang.brigadier.CommandDispatcher;
import com.shirokai.core.CoreRuntime;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import java.util.function.Supplier;
import static net.minecraft.server.command.CommandManager.literal;

public final class ScoreboardCommands {
    private final Supplier<CoreRuntime> runtime;
    public ScoreboardCommands(Supplier<CoreRuntime> runtime) { this.runtime = runtime; }
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        for (String name : new String[]{"shirokai:sidebar", "sidebar"}) {
            if (dispatcher.getRoot().getChild(name) != null) {
                org.slf4j.LoggerFactory.getLogger("shirokai-core").warn("[scoreboard] /{} já registrado; comando externo preservado", name); continue;
            }
            dispatcher.register(literal(name).executes(c -> toggle(c.getSource())).then(literal("toggle").executes(c -> toggle(c.getSource())))
                .then(literal("refresh").executes(c -> {
                    var core = available(c.getSource()); if (core == null) return 0;
                    core.scoreboard().refresh(c.getSource().getPlayer().getUuid()); c.getSource().sendFeedback(() -> Text.literal("Scoreboard atualizada"), false); return 1;
                })));
        }
    }
    private CoreRuntime available(ServerCommandSource source) {
        if (source.getPlayer() == null) { source.sendError(Text.literal("Scoreboard exige jogador")); return null; }
        var core = runtime.get();
        if (core == null || !core.running("scoreboard")) { source.sendError(Text.literal("Scoreboard indisponível")); return null; }
        return core;
    }
    private int toggle(ServerCommandSource source) {
        var core = available(source); if (core == null) return 0;
        core.scoreboard().toggle(source.getPlayer().getUuid()).whenCompleteAsync((enabled, error) -> {
            if (error != null) source.sendError(Text.literal("Não foi possível salvar preferência de scoreboard"));
            else source.sendFeedback(() -> Text.literal("Scoreboard: " + (enabled ? "ativada" : "desativada")), false);
        }, source.getServer()); return 1;
    }
}
