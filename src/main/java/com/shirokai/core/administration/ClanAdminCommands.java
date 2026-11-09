package com.shirokai.core.administration;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.shirokai.core.CoreRuntime;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import static net.minecraft.server.command.CommandManager.*;

/** Explicit repair authority: identity correction does not move members or bypass leadership confirmations. */
public final class ClanAdminCommands {
    private final Supplier<CoreRuntime> runtime;
    private final BiPredicate<ServerCommandSource,String> allowed;
    public ClanAdminCommands(Supplier<CoreRuntime> runtime,BiPredicate<ServerCommandSource,String> allowed){this.runtime=runtime;this.allowed=allowed;}
    private final java.util.Map<String,com.shirokai.core.clans.ClanMaintenance.Plan> pending=new java.util.LinkedHashMap<>();
    private String key(ServerCommandSource source,String clan){return (source.getPlayer()==null?"console":source.getPlayer().getUuidAsString())+":"+clan;}
    public LiteralArgumentBuilder<ServerCommandSource> node(){
        var root=literal("clans").requires(s->allowed.test(s,"clans.inspect")||allowed.test(s,"clans.identity")||allowed.test(s,"clans.history")||allowed.test(s,"clans.diagnose")||allowed.test(s,"clans.repair"));
        root.then(literal("list").requires(s->allowed.test(s,"clans.inspect")).executes(c->execute(c.getSource(),()->runtime.get().clans().list(0)))
            .then(argument("page",IntegerArgumentType.integer(1,1000000)).executes(c->execute(c.getSource(),()->runtime.get().clans().list(IntegerArgumentType.getInteger(c,"page")-1)))));
        root.then(literal("inspect").requires(s->allowed.test(s,"clans.inspect")).then(argument("id",StringArgumentType.word())
            .executes(c->execute(c.getSource(),()->runtime.get().clans().find(StringArgumentType.getString(c,"id"))))));
        for(String action:new String[]{"rename","tag"})root.then(literal(action).requires(s->allowed.test(s,"clans.identity"))
            .then(argument("id",StringArgumentType.word()).then(argument("value",StringArgumentType.greedyString()).executes(c->execute(c.getSource(),()->
                runtime.get().clans().find(StringArgumentType.getString(c,"id")).thenComposeAsync(expected->{
                    if(!allowed.test(c.getSource(),"clans.identity"))throw new IllegalStateException("Permissão administrativa revogada");
                    return runtime.get().clans().adminIdentity(c.getSource().getPlayer()==null?null:c.getSource().getPlayer().getUuid(),expected,StringArgumentType.getString(c,"value"),action.equals("tag"));
                },c.getSource().getServer()))))));
        root.then(literal("history").requires(s->allowed.test(s,"clans.history")).then(argument("id",StringArgumentType.word())
            .executes(c->execute(c.getSource(),()->runtime.get().clans().maintenance().history(StringArgumentType.getString(c,"id"),0)))
            .then(argument("page",IntegerArgumentType.integer(1,1000000)).executes(c->execute(c.getSource(),()->runtime.get().clans().maintenance().history(StringArgumentType.getString(c,"id"),IntegerArgumentType.getInteger(c,"page")-1))))));
        root.then(literal("diagnose").requires(s->allowed.test(s,"clans.diagnose"))
            .executes(c->execute(c.getSource(),()->runtime.get().clans().maintenance().databaseHealth()))
            .then(argument("id",StringArgumentType.word()).executes(c->execute(c.getSource(),()->runtime.get().clans().maintenance().diagnose(StringArgumentType.getString(c,"id"))))));
        root.then(literal("repair").requires(s->allowed.test(s,"clans.repair")).then(argument("id",StringArgumentType.word())
            .executes(c->execute(c.getSource(),()->runtime.get().clans().maintenance().preview(StringArgumentType.getString(c,"id")).thenApplyAsync(plan->{
                pending.entrySet().removeIf(e->System.currentTimeMillis()-e.getValue().at()>120000);
                if(pending.size()>=128)pending.remove(pending.keySet().iterator().next());pending.put(key(c.getSource(),StringArgumentType.getString(c,"id")),plan);
                return "PRÉVIA (nenhuma alteração): "+plan.report()+" | confirmar em120s: /shirokaicore clans repair "+StringArgumentType.getString(c,"id")+" confirm "+plan.token();
            },c.getSource().getServer())))
            .then(literal("confirm").then(argument("token",StringArgumentType.word()).executes(c->execute(c.getSource(),()->{
                if(!allowed.test(c.getSource(),"clans.repair"))throw new IllegalStateException("Permissão administrativa revogada");
                var plan=pending.remove(key(c.getSource(),StringArgumentType.getString(c,"id")));
                if(plan==null||!plan.token().equals(StringArgumentType.getString(c,"token")))throw new IllegalStateException("Prévia/token inválidos; solicite outra prévia");
                return runtime.get().clans().adminRepair(c.getSource().getPlayer()==null?null:c.getSource().getPlayer().getUuid(),plan);
            }))))));
        return root;
    }
    private int execute(ServerCommandSource source,Supplier<? extends CompletableFuture<?>> operation){
        try{
            operation.get().whenCompleteAsync((value,error)->{
                if(error==null)source.sendFeedback(()->Text.literal("Clãs: "+value),false);
                else{while(error.getCause()!=null)error=error.getCause();source.sendError(Text.literal("Clãs: "+error.getMessage()));}
            },source.getServer());return 1;
        }catch(Exception error){source.sendError(Text.literal("Clãs: "+error.getMessage()));return 0;}
    }
}
