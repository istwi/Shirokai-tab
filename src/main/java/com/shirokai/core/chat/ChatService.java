package com.shirokai.core.chat;

import com.shirokai.core.permissions.PermissionService;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;

/** All methods run on the server executor. Persistence never reads Minecraft objects. */
public final class ChatService {
    public record Participant(UUID id,String name,String world,double x,double y,double z,UUID clan){}
    public record Envelope(UUID author,String label,String body,Channel channel,String target){public Envelope(UUID author,String label,String body,Channel channel){this(author,label,body,channel,"");}}
    public interface Audience { Collection<Participant> online(); }
    @FunctionalInterface public interface Sink { void deliver(UUID target,Envelope envelope); }
    private static final class Session {
        ChatProfile profile;final SpamGate spam=new SpamGate();UUID reply;Long lastLog;
        CompletableFuture<ChatProfile> loading;
    }
    private final ChatRepository repository;private final PermissionService permissions;private final Executor server;
    private final Audience audience;private final Function<UUID,List<String>> tags;private final Clock clock;
    private final BiConsumer<UUID,String> moderation;private final Map<UUID,Session> sessions=new HashMap<>();
    private ChatConfig config;
    public ChatService(ChatRepository repository,PermissionService permissions,Executor server,Audience audience,Function<UUID,List<String>> tags,Clock clock,ChatConfig config,BiConsumer<UUID,String> moderation){
        this.repository=repository;this.permissions=permissions;this.server=server;this.audience=audience;this.tags=tags;this.clock=clock;this.config=config;this.moderation=moderation;
    }
    public ChatConfig config(){return config;}
    public void replaceConfig(ChatConfig value){config=value;}
    public CompletableFuture<ChatProfile> join(UUID player){
        var existing=sessions.get(player);if(existing!=null)return existing.loading.copy();
        var session=new Session();sessions.put(player,session);
        session.loading=repository.profile(player,config.defaultChannel()).thenApplyAsync(profile->{if(sessions.get(player)==session)session.profile=profile;return profile;},server);
        return session.loading.copy();
    }
    public void leave(UUID player){sessions.remove(player);for(var session:sessions.values())if(player.equals(session.reply))session.reply=null;}
    private Session session(UUID player){var value=sessions.get(player);if(value==null||value.profile==null)throw new IllegalStateException("Chat carregando ou indisponível; tente novamente");return value;}
    public boolean permitted(UUID player,Channel channel){String node=config.channelPermissions().get(channel);return node.isEmpty()||permissions.allowed(player,node);}
    private CompletableFuture<ChatProfile> apply(CompletableFuture<ChatProfile> operation){return operation.thenApplyAsync(profile->{var session=sessions.get(profile.player());if(session!=null&&session.profile!=null)session.profile=profile;return profile;},server);}
    public CompletableFuture<ChatProfile> select(UUID player,Channel channel){
        session(player);if(!permitted(player,channel))throw new IllegalStateException("Sem permissão para canal "+channel);
        if(channel==Channel.CLAN&&participant(player).clan()==null)throw new IllegalStateException("Você não pertence a um clã");return apply(repository.channel(player,channel));
    }
    public CompletableFuture<ChatProfile> ignore(UUID player,UUID target){var current=session(player);return apply(repository.toggleIgnore(player,target,current.profile.channel()));}
    public CompletableFuture<ChatProfile> mute(UUID actor,UUID target,Long expiry,String reason){
        if(expiry!=null&&expiry!=0&&expiry<=clock.millis())throw new IllegalArgumentException("Expiração deve estar no futuro");
        return apply(repository.mute(actor,target,expiry,reason,fallback(target)));
    }
    private Channel fallback(UUID player){var session=sessions.get(player);return session!=null&&session.profile!=null?session.profile.channel():config.defaultChannel();}
    public CompletableFuture<ChatProfile> inspect(UUID player){return repository.profile(player,fallback(player));}
    public Channel selected(UUID player){return session(player).profile.channel();}
    public UUID replyTarget(UUID player){UUID target=session(player).reply;if(target==null)throw new IllegalStateException("Nenhuma conversa recente disponível");return target;}
    private Participant participant(UUID player){return audience.online().stream().filter(p->p.id().equals(player)).findFirst().orElseThrow(()->new IllegalStateException("Jogador indisponível"));}
    private boolean accepts(Participant recipient,Participant sender,Channel channel){
        var session=sessions.get(recipient.id());if(session==null||session.profile==null||!permitted(recipient.id(),channel))return false;
        if(session.profile.ignored().contains(sender.id())&&!permissions.allowed(sender.id(),"shirokaicore.chat.bypass.ignore"))return false;
        return switch(channel){
            case GLOBAL,STAFF -> true;
            case CLAN -> sender.clan()!=null&&sender.clan().equals(recipient.clan());
            case LOCAL -> sender.world().equals(recipient.world())&&distance(sender,recipient)<=config.localRange()*config.localRange();
            case PRIVATE -> true;
        };
    }
    private static double distance(Participant a,Participant b){return Math.pow(a.x()-b.x(),2)+Math.pow(a.y()-b.y(),2)+Math.pow(a.z()-b.z(),2);}
    private void gate(UUID sender,String body){
        var session=session(sender);var mute=session.profile.mute();
        if(mute!=null&&mute.active(clock.millis())&&!permissions.allowed(sender,"shirokaicore.chat.bypass.mute"))throw new IllegalStateException("Você está silenciado: "+mute.reason());
        session.spam.admit(body,config,clock.millis(),permissions.allowed(sender,"shirokaicore.chat.bypass.cooldown"),permissions.allowed(sender,"shirokaicore.chat.bypass.filter"));
    }
    public int send(UUID sender,Channel channel,String body,Sink sink){
        try{
            if(channel==Channel.PRIVATE)throw new IllegalArgumentException("Canal privado exige destinatário");
            session(sender);if(!permitted(sender,channel))throw new IllegalStateException("Sem permissão para canal "+channel);
            var author=participant(sender);if(channel==Channel.CLAN&&author.clan()==null)throw new IllegalStateException("Você não pertence a um clã");
            gate(sender,body);var envelope=new Envelope(sender,label(author,channel,""),body,channel);int sent=0;
            for(var recipient:List.copyOf(audience.online()))if(accepts(recipient,author,channel)){sink.deliver(recipient.id(),envelope);sent++;}
            return sent;
        }catch(RuntimeException error){rejected(sender,error);throw error;}
    }
    public void privateMessage(UUID sender,UUID target,String body,Sink sink){
        try{
            if(sender.equals(target))throw new IllegalArgumentException("Escolha outro jogador");var source=session(sender);session(target);
            if(!permitted(sender,Channel.PRIVATE)||!permitted(target,Channel.PRIVATE))throw new IllegalStateException("Mensagem privada indisponível");
            var author=participant(sender);var recipient=participant(target);
            if(source.profile.ignored().contains(target)||!accepts(recipient,author,Channel.PRIVATE))throw new IllegalStateException("Mensagem privada indisponível");
            gate(sender,body);var envelope=new Envelope(sender,label(author,Channel.PRIVATE,recipient.name()),body,Channel.PRIVATE,recipient.name());
            sink.deliver(target,envelope);sink.deliver(sender,envelope);source.reply=target;session(target).reply=sender;
        }catch(RuntimeException error){rejected(sender,error);throw error;}
    }
    private void rejected(UUID sender,RuntimeException error){var session=sessions.get(sender);if(session!=null&&(session.lastLog==null||clock.millis()-session.lastLog>=5000)){session.lastLog=clock.millis();moderation.accept(sender,error.getMessage());}}
    private String label(Participant author,Channel channel,String target){
        var selected=tags.apply(author.id()).stream().limit(2).toList();String identifications=selected.isEmpty()?"":String.join(" ",selected)+" ";
        var values=Map.of("channel",channel.name(),"tags",identifications,"name",author.name(),"target",target);
        var matcher=Pattern.compile("\\{(channel|tags|name|target)}").matcher(config.nameFormats().get(channel));var result=new StringBuilder();
        while(matcher.find())matcher.appendReplacement(result,Matcher.quoteReplacement(values.get(matcher.group(1))));return matcher.appendTail(result).toString();
    }
}
