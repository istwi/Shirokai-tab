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
    public LiteralArgumentBuilder<ServerCommandSource> node(){
        var root=literal("clans").requires(s->allowed.test(s,"clans.inspect")||allowed.test(s,"clans.identity"));
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
