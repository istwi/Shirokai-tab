package com.shirokai.holograms;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import static net.minecraft.server.command.CommandManager.*;

public final class Commands {
    private final Supplier<HologramService> service;
    public Commands(Supplier<HologramService> service){this.service=service;}
    private HologramService service(){var result=service.get();if(result==null)throw new IllegalStateException("Servidor iniciando");return result;}
    private RequiredArgumentBuilder<ServerCommandSource,String> id() {
        return argument("id",StringArgumentType.word()).suggests((context,builder)->{
            try{service().plan().entries().keySet().stream().filter(s->s.startsWith(builder.getRemaining())).forEach(builder::suggest);}catch(RuntimeException ignored){}
            return builder.buildFuture();
        });
    }
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var root=literal("hologram").requires(s->List.of("create","delete","edit","move","reload").stream().anyMatch(a->Permissions.allowed(s,a)))
            .executes(c->reply(c.getSource(),"/hologram create|delete|edit|move|teleport|list|reload|align|line|scale"));
        root.then(literal("create").requires(s->Permissions.allowed(s,"create")).then(argument("id",StringArgumentType.word()).executes(c->safe(c,()->{
            var source=c.getSource();var player=source.getPlayerOrThrow();String id=StringArgumentType.getString(c,"id");
            var h=Definitions.Hologram.create(id,player.getServerWorld().getRegistryKey().getValue().toString(),player.getX(),player.getY()+2,player.getZ());
            return operation(source,service().change(entries->{if(entries.stream().anyMatch(e->e.id().equals(id)))throw new IllegalArgumentException("ID já existe");entries.add(h);return entries;}),"Holograma criado: "+id);
        }))));
        root.then(literal("delete").requires(s->Permissions.allowed(s,"delete")).then(id().executes(c->safe(c,()->{
            var h=service().get(StringArgumentType.getString(c,"id"));
            return operation(c.getSource(),service().change(entries->{entries.removeIf(e->e.id().equals(h.id()));return entries;}),"Holograma apagado: "+h.id());
        }))));
        root.then(literal("edit").requires(s->Permissions.allowed(s,"edit")).then(id().executes(c->safe(c,()->{
            var h=service().get(StringArgumentType.getString(c,"id"));reply(c.getSource(),h.id()+" | "+h.world()+" | "+h.alignment()+" | escala "+h.scale());
            for(int i=0;i<h.lines().size();i++)reply(c.getSource(),(i+1)+": "+h.lines().get(i).text()+" ["+h.lines().get(i).animation()+"]");
            return reply(c.getSource(),"Editar: /hologram line set "+h.id()+" <linha> <texto>; line add/remove/align/animation, align, scale, move. Demais opções: config/shirokai-holograms/holograms.json + /hologram reload");
        }))));
        root.then(literal("move").requires(s->Permissions.allowed(s,"move")).then(id().executes(c->safe(c,()->{
            var p=c.getSource().getPlayerOrThrow();return replace(c,h->h.moved(p.getServerWorld().getRegistryKey().getValue().toString(),p.getX(),p.getY()+2,p.getZ()));
        }))));
        root.then(literal("teleport").requires(s->Permissions.allowed(s,"move")).then(id().executes(c->safe(c,()->{
            var p=c.getSource().getPlayerOrThrow();var h=service().get(StringArgumentType.getString(c,"id"));
            for(var world:p.getServer().getWorlds())if(world.getRegistryKey().getValue().toString().equals(h.world())) {
                p.teleport(world,h.x(),h.y()-2,h.z(),p.getYaw(),p.getPitch());return reply(c.getSource(),"Teleportado para "+h.id());
            }
            throw new IllegalArgumentException("Dimensão indisponível: "+h.world());
        }))));
        root.then(literal("list").requires(s->Permissions.allowed(s,"edit")).executes(c->safe(c,()->reply(c.getSource(),"Hologramas: "+service().plan().entries().keySet()))));
        root.then(literal("reload").requires(s->Permissions.allowed(s,"reload")).executes(c->safe(c,()->operation(c.getSource(),service().reload(),"Hologramas recarregados"))));
        root.then(literal("align").requires(s->Permissions.allowed(s,"edit")).then(id().then(argument("alignment",StringArgumentType.word()).suggests((c,b)->{for(String a:List.of("left","center","right"))b.suggest(a);return b.buildFuture();}).executes(c->safe(c,()->replace(c,h->h.aligned(alignment(c))))))));
        root.then(literal("scale").requires(s->Permissions.allowed(s,"edit")).then(id().then(argument("value",FloatArgumentType.floatArg(.25f,4)).executes(c->safe(c,()->replace(c,h->h.scaled(FloatArgumentType.getFloat(c,"value"))))))));
        var line=literal("line").requires(s->Permissions.allowed(s,"edit"));
        line.then(literal("add").then(id().then(argument("text",StringArgumentType.greedyString()).executes(c->safe(c,()->replace(c,h->{var lines=new ArrayList<>(h.lines());lines.add(new Definitions.Line(StringArgumentType.getString(c,"text"),null,"STATIC"));return h.withLines(lines);}))))));
        line.then(literal("remove").then(id().then(argument("line",IntegerArgumentType.integer(1,40)).executes(c->safe(c,()->replace(c,h->{var lines=new ArrayList<>(h.lines());lines.remove(index(c,lines.size()));return h.withLines(lines);}))))));
        line.then(literal("set").then(id().then(argument("line",IntegerArgumentType.integer(1,40)).then(argument("text",StringArgumentType.greedyString()).executes(c->safe(c,()->editLine(c,old->new Definitions.Line(StringArgumentType.getString(c,"text"),old.alignment(),old.animation()))))))));
        line.then(literal("align").then(id().then(argument("line",IntegerArgumentType.integer(1,40)).then(argument("alignment",StringArgumentType.word()).executes(c->safe(c,()->editLine(c,old->new Definitions.Line(old.text(),alignment(c),old.animation()))))))));
        line.then(literal("animation").then(id().then(argument("line",IntegerArgumentType.integer(1,40)).then(argument("animation",StringArgumentType.word()).suggests((c,b)->{try{service().plan().snapshot().animations().presets().keySet().forEach(b::suggest);}catch(RuntimeException ignored){}return b.buildFuture();}).executes(c->safe(c,()->editLine(c,old->new Definitions.Line(old.text(),old.alignment(),animation(c)))))))));
        root.then(line);dispatcher.register(root);
    }
    private String animation(CommandContext<ServerCommandSource> c){String name=StringArgumentType.getString(c,"animation");return service().plan().snapshot().animations().presets().containsKey(name)?name:name.toUpperCase(Locale.ROOT);}
    private static int index(CommandContext<ServerCommandSource> c,int size){int i=IntegerArgumentType.getInteger(c,"line")-1;if(i>=size)throw new IllegalArgumentException("Linha inexistente (numeração começa em 1)");return i;}
    private static Definitions.Alignment alignment(CommandContext<ServerCommandSource> c){try{return Definitions.Alignment.valueOf(StringArgumentType.getString(c,"alignment").toUpperCase(Locale.ROOT));}catch(IllegalArgumentException e){throw new IllegalArgumentException("Alinhamento: left, center ou right");}}
    private int editLine(CommandContext<ServerCommandSource> c,UnaryOperator<Definitions.Line> edit){return replace(c,h->{var lines=new ArrayList<>(h.lines());int i=index(c,lines.size());lines.set(i,edit.apply(lines.get(i)));return h.withLines(lines);});}
    private int replace(CommandContext<ServerCommandSource> c,UnaryOperator<Definitions.Hologram> edit){
        var old=service().get(StringArgumentType.getString(c,"id"));var next=edit.apply(old);
        return operation(c.getSource(),service().change(entries->{for(int i=0;i<entries.size();i++)if(entries.get(i).id().equals(old.id()))entries.set(i,next);return entries;}),"Holograma salvo: "+old.id());
    }
    private int operation(ServerCommandSource source,CompletableFuture<Void> work,String message){
        work.whenComplete((v,error)->source.getServer().execute(()->{if(error==null)reply(source,message);else fail(source,error);}));return 1;
    }
    @FunctionalInterface private interface Action { int run() throws Exception; }
    private int safe(CommandContext<ServerCommandSource> c,Action action){try{return action.run();}catch(Exception e){return fail(c.getSource(),e);}}
    private static int fail(ServerCommandSource source,Throwable error){while(error.getCause()!=null)error=error.getCause();source.sendError(Text.literal("Hologramas: "+error.getMessage()));return 0;}
    private static int reply(ServerCommandSource source,String message){source.sendFeedback(()->Text.literal(message),false);return 1;}
}
