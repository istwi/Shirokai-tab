package com.shirokai.core.chat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.shirokai.core.CoreRuntime;
import net.minecraft.command.argument.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import com.shirokai.core.messages.Messages;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import static net.minecraft.server.command.CommandManager.*;

public final class ChatCommands {
    private final Supplier<CoreRuntime> runtime;
    public ChatCommands(Supplier<CoreRuntime> runtime){this.runtime=runtime;}
    public void register(CommandDispatcher<ServerCommandSource> dispatcher){
        if(dispatcher.getRoot().getChild("chat")!=null){org.slf4j.LoggerFactory.getLogger("shirokai-core").warn("[chat] /chat em conflito; comandos Core não registrados");return;}
        var root=literal("chat").executes(c->channel(c.getSource(),null,null));
        for(Channel channel:List.of(Channel.GLOBAL,Channel.LOCAL,Channel.CLAN,Channel.STAFF))root.then(literal(channel.name().toLowerCase(Locale.ROOT))
                .executes(c->channel(c.getSource(),channel,null)).then(argument("message",StringArgumentType.greedyString()).executes(c->channel(c.getSource(),channel,StringArgumentType.getString(c,"message")))));
        root.then(literal("msg").then(argument("player",StringArgumentType.word()).then(argument("message",StringArgumentType.greedyString()).executes(c->privateMessage(c.getSource(),StringArgumentType.getString(c,"player"),StringArgumentType.getString(c,"message"))))));
        root.then(literal("reply").then(argument("message",StringArgumentType.greedyString()).executes(c->reply(c.getSource(),StringArgumentType.getString(c,"message")))));
        root.then(literal("ignore").then(argument("player",StringArgumentType.word()).executes(c->ignore(c.getSource(),StringArgumentType.getString(c,"player")))));
        dispatcher.register(root);
        for(var entry:Map.of("local",Channel.LOCAL,"l",Channel.LOCAL,"global",Channel.GLOBAL,"g",Channel.GLOBAL,"staffchat",Channel.STAFF).entrySet()){
            if(dispatcher.getRoot().getChild(entry.getKey())==null)dispatcher.register(literal(entry.getKey()).executes(c->channel(c.getSource(),entry.getValue(),null))
                .then(argument("message",StringArgumentType.greedyString()).executes(c->channel(c.getSource(),entry.getValue(),StringArgumentType.getString(c,"message")))));
            else org.slf4j.LoggerFactory.getLogger("shirokai-core").warn("[chat] Alias /{} preservado por conflito; use /chat {}",entry.getKey(),entry.getValue());
        }
        if(dispatcher.getRoot().getChild("r")==null)dispatcher.register(literal("r").then(argument("message",StringArgumentType.greedyString()).executes(c->reply(c.getSource(),StringArgumentType.getString(c,"message")))));
        if(dispatcher.getRoot().getChild("ignore")==null)dispatcher.register(literal("ignore").then(argument("player",StringArgumentType.word()).executes(c->ignore(c.getSource(),StringArgumentType.getString(c,"player")))));
        var current=runtime.get();
        if(current!=null&&current.running("chat"))configureVanilla(dispatcher,current.chat().service().config().takeVanillaPrivateCommands());
    }
    public void configureVanilla(CommandDispatcher<ServerCommandSource> dispatcher,boolean takeOwnership){
        if(!takeOwnership)return;
        var msg=dispatcher.getRoot().getChild("msg");
        if(msg==null){dispatcher.register(literal("msg").then(argument("targets",EntityArgumentType.players()).then(argument("message",MessageArgumentType.message()).executes(this::vanillaPrivate))));}
        else{
            var targets=msg.getChild("targets");var message=targets==null?null:targets.getChild("message");
            if(message==null||message.getCommand()==null||!message.getCommand().getClass().getName().startsWith(net.minecraft.server.command.MessageCommand.class.getName())){
                org.slf4j.LoggerFactory.getLogger("shirokai-core").warn("[chat] /msg de terceiro preservado; /chat msg é controlado pelo Core");return;
            }
            dispatcher.register(literal("msg").then(argument("targets",EntityArgumentType.players()).then(argument("message",MessageArgumentType.message()).executes(this::vanillaPrivate))));
        }
        var owned=dispatcher.getRoot().getChild("msg");
        for(String alias:List.of("tell","w")){
            var current=dispatcher.getRoot().getChild(alias);
            if(current==null)dispatcher.register(literal(alias).redirect(owned));
            else if(current.getRedirect()!=owned)org.slf4j.LoggerFactory.getLogger("shirokai-core").warn("[chat] /{} de terceiro preservado; use /chat msg",alias);
        }
    }
    private int vanillaPrivate(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var targets=EntityArgumentType.getPlayers(context,"targets");
        if(targets.size()!=1){context.getSource().sendError(Messages.error("Mensagem privada exige um destinatário"));return 0;}
        return privateMessage(context.getSource(),targets.iterator().next().getUuid().toString(),MessageArgumentType.getMessage(context,"message").getString());
    }
    public int channel(ServerCommandSource source,Channel channel,String body){return execute(source,chat->{
        UUID sender=source.getPlayer().getUuid();if(channel==null){source.sendFeedback(()->Messages.info("Canal atual: "+Messages.channel(chat.service().selected(sender))),false);return;}
        if(body==null)respond(source,chat.service().select(sender,channel).thenApply(profile->"Canal selecionado: "+Messages.channel(profile.channel())));
        else chat.service().send(sender,channel,body,chat::system);
    });}
    private UUID target(ServerCommandSource source,String value){
        try{return UUID.fromString(value);}catch(IllegalArgumentException invalid){var player=source.getServer().getPlayerManager().getPlayer(value);if(player==null)throw new IllegalArgumentException("Jogador offline: informe UUID para ignore");return player.getUuid();}
    }
    public int privateMessage(ServerCommandSource source,String player,String body){return execute(source,chat->chat.service().privateMessage(source.getPlayer().getUuid(),target(source,player),body,chat::system));}
    private int reply(ServerCommandSource source,String body){return execute(source,chat->chat.service().privateMessage(source.getPlayer().getUuid(),chat.service().replyTarget(source.getPlayer().getUuid()),body,chat::system));}
    private int ignore(ServerCommandSource source,String player){return execute(source,chat->{UUID target=target(source,player);respond(source,chat.service().ignore(source.getPlayer().getUuid(),target).thenApply(profile->profile.ignored().contains(target)?"Jogador ignorado":"Ignore removido"));});}
    private int execute(ServerCommandSource source,Consumer<MinecraftChat> action){
        var core=runtime.get();if(source.getPlayer()==null){source.sendError(Messages.error("Chat exige jogador"));return 0;}
        if(core==null||!core.running("chat")){source.sendError(Messages.error("Chat indisponível"));return 0;}
        try{action.accept(core.chat());return 1;}catch(RuntimeException error){failure(source,error);return 0;}
    }
    private static void respond(ServerCommandSource source,CompletableFuture<String> result){result.whenCompleteAsync((message,error)->{if(error!=null)failure(source,error);else source.sendFeedback(()->Messages.info(message),false);},source.getServer());}
    private static void failure(ServerCommandSource source,Throwable error){while(error.getCause()!=null)error=error.getCause();source.sendError(Messages.error("Chat: "+error.getMessage()));}
}
