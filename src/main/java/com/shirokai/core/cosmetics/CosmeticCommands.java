package com.shirokai.core.cosmetics;

import com.mojang.brigadier.CommandDispatcher;
import com.shirokai.core.CoreRuntime;
import java.util.function.Supplier;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import static net.minecraft.server.command.CommandManager.*;

public final class CosmeticCommands {
    private final Supplier<CoreRuntime> runtime;
    public CosmeticCommands(Supplier<CoreRuntime> runtime){this.runtime=runtime;}
    private CosmeticService service(){var r=runtime.get();if(r==null||r.cosmetics()==null)throw new IllegalStateException("Cosméticos carregando ou indisponíveis");return r.cosmetics();}
    private boolean allowed(ServerCommandSource s,String key){var r=runtime.get();return s.getPlayer()==null?s.hasPermissionLevel(4):r!=null&&r.permissions().allowed(s.getPlayer().getUuid(),"shirokai.core.cosmetics."+key);}
    public void register(CommandDispatcher<ServerCommandSource> dispatcher){
        for(String name:new String[]{"cosmeticos","shirokaicore:cosmeticos"}){
            if(dispatcher.getRoot().getChild(name)!=null)continue;
            dispatcher.register(literal(name).requires(s->s.getPlayer()!=null&&allowed(s,"use")).executes(c->run(c.getSource(),()->service().open(c.getSource().getPlayerOrThrow(),"main")))
                .then(literal("desativar").executes(c->run(c.getSource(),()->service().disable(c.getSource().getPlayerOrThrow())))));
        }
        for(String name:new String[]{"cosmeticosadmin","shirokaicore:cosmeticosadmin"}){
            if(dispatcher.getRoot().getChild(name)!=null)continue;
            dispatcher.register(literal(name).requires(s->allowed(s,"admin"))
                .then(literal("status").executes(c->run(c.getSource(),()->c.getSource().sendFeedback(()->CosmeticService.message(service().status()),false))))
                .then(literal("reload").executes(c->run(c.getSource(),()->service().reload().whenCompleteAsync((v,e)->{if(e!=null)c.getSource().sendError(CosmeticService.message("Configuração inválida; anterior mantida."));else c.getSource().sendFeedback(()->CosmeticService.message("Cosméticos recarregados."),false);},c.getSource().getServer()))))
                .then(literal("celebrar").then(argument("jogador",EntityArgumentType.player())
                    .then(literal("vitoria").executes(c->run(c.getSource(),()->celebrate(EntityArgumentType.getPlayer(c,"jogador"),"victory"))))
                    .then(literal("raid").executes(c->run(c.getSource(),()->celebrate(EntityArgumentType.getPlayer(c,"jogador"),"raid")))))));
        }
    }
    private void celebrate(net.minecraft.server.network.ServerPlayerEntity player,String category){if(!service().celebrate(player,category))throw new IllegalStateException("Efeito indisponível: confira seleção, permissão, cooldown e batalha.");}
    @FunctionalInterface private interface Work{void run()throws Exception;}
    private int run(ServerCommandSource source,Work work){try{work.run();return 1;}catch(Exception e){source.sendError(CosmeticService.message(e.getMessage()==null?"Cosméticos indisponíveis":e.getMessage()));return 0;}}
}
