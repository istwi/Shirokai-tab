package com.shirokai.core.profiles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.shirokai.core.CoreRuntime;
import com.shirokai.core.titles.TitleState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import com.shirokai.core.messages.Messages;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import static net.minecraft.server.command.CommandManager.*;

public final class ProfileCommands {
    private final Supplier<CoreRuntime> runtime;
    public ProfileCommands(Supplier<CoreRuntime> runtime) { this.runtime = runtime; }
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (dispatcher.getRoot().getChild("perfil") != null) {
            org.slf4j.LoggerFactory.getLogger("shirokai-core").warn("[profiles] /perfil já existe; comando do Core não registrado"); return;
        }
        dispatcher.register(literal("perfil").executes(c -> {
            var source = c.getSource();
            if (!available(source)) return 0;
            ProfileMenu.open(source.getPlayer(), runtime.get().titles()); return 1;
        }).then(literal("titulo").then(argument("id", StringArgumentType.word()).suggests((c,b) -> suggest(c.getSource(),b)).executes(c -> {
            if (!available(c.getSource())) return 0;
            return respond(c.getSource(), runtime.get().titles().equip(c.getSource().getPlayer().getUuid(), StringArgumentType.getString(c, "id"), false));
        }))).then(literal("chat").then(argument("id", StringArgumentType.word()).suggests((c,b) -> suggest(c.getSource(),b)).executes(c -> {
            if (!available(c.getSource())) return 0;
            return respond(c.getSource(), runtime.get().titles().equip(c.getSource().getPlayer().getUuid(), StringArgumentType.getString(c, "id"), true));
        }))).then(literal("limpar").executes(c -> {
            if (!available(c.getSource())) return 0;
            return respond(c.getSource(), runtime.get().titles().clear(c.getSource().getPlayer().getUuid()));
        })));
    }
    private CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggest(ServerCommandSource source, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        var core = runtime.get();
        if (core != null && core.running("titles")) for (String id : (source.getPlayer()==null?core.titles().catalog():core.titles().catalog(source.getPlayer().getUuid())).definitions().keySet())
            if (id.startsWith(builder.getRemainingLowerCase())) builder.suggest(id);
        return builder.buildFuture();
    }
    private boolean available(ServerCommandSource source) {
        var core = runtime.get();
        if (source.getPlayer() == null) { source.sendError(Messages.error("/perfil exige um jogador")); return false; }
        if (core == null || !core.running("profiles") || !core.running("titles")) {
            source.sendError(Messages.error("Perfil indisponível; consulte a administração")); return false;
        }
        return true;
    }
    private int respond(ServerCommandSource source, CompletableFuture<TitleState> operation) {
        operation.whenCompleteAsync((state, error) -> {
            if (error != null) {
                Throwable cause = error; while (cause.getCause() != null) cause = cause.getCause();
                source.sendError(Messages.error("Perfil: " + cause.getMessage()));
            } else source.sendFeedback(() -> {
                var titles=runtime.get().titles();var id=source.getPlayer().getUuid();
                var card=Messages.info("Perfil atualizado.");
                var visual=titles.visualPresentation(id);
                card.append(Text.literal("\n")).append(Messages.field("Título no TAB e scoreboard",visual.getString().isEmpty()?Text.literal("Nenhum"):visual));
                var tags=Text.empty();for(var tag:titles.chatPresentation(id)){if(!tags.getString().isEmpty())tags.append(Text.literal(" "));tags.append(tag);}
                card.append(Text.literal("\n")).append(Messages.field("Tags no chat ("+state.chatIds().size()+"/2)",tags.getString().isEmpty()?Text.literal("Nenhuma"):tags));
                return card;
            }, false);
        }, source.getServer()); return 1;
    }
}
