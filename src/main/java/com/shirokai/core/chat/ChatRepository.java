package com.shirokai.core.chat;

import com.shirokai.core.storage.SqliteStorage;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class ChatRepository {
    private final SqliteStorage storage;private final Clock clock;
    public ChatRepository(SqliteStorage storage,Clock clock){this.storage=storage;this.clock=clock;}
    private static PreparedStatement prepare(Connection c,String sql,Object...args)throws SQLException{
        var s=c.prepareStatement(sql);for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]instanceof UUID?args[i].toString():args[i]);return s;
    }
    private static void update(Connection c,String sql,Object...args)throws SQLException{try(var s=prepare(c,sql,args)){s.executeUpdate();}}
    private static ChatProfile read(Connection c,UUID player,Channel fallback)throws SQLException{
        Channel channel=fallback;try(var s=prepare(c,"SELECT channel FROM chat_profiles WHERE player_uuid=?",player);var rows=s.executeQuery()){if(rows.next())channel=Channel.valueOf(rows.getString(1));}
        var ignored=new HashSet<UUID>();try(var s=prepare(c,"SELECT target_uuid FROM chat_ignores WHERE owner_uuid=?",player);var rows=s.executeQuery()){while(rows.next())ignored.add(UUID.fromString(rows.getString(1)));}
        ChatProfile.Mute mute=null;try(var s=prepare(c,"SELECT expires_at,reason FROM chat_mutes WHERE player_uuid=?",player);var rows=s.executeQuery()){if(rows.next())mute=new ChatProfile.Mute(rows.getLong(1),rows.getString(2));}
        return new ChatProfile(player,channel,ignored,mute);
    }
    public CompletableFuture<ChatProfile> profile(UUID player,Channel fallback){return storage.transact(c->read(c,player,fallback));}
    public CompletableFuture<ChatProfile> channel(UUID player,Channel channel){
        if(channel==Channel.PRIVATE)throw new IllegalArgumentException("Canal privado exige destinatário");
        return storage.transact(c->{update(c,"INSERT INTO chat_profiles(player_uuid,channel) VALUES(?,?) ON CONFLICT(player_uuid) DO UPDATE SET channel=excluded.channel",player,channel.name());return read(c,player,channel);});
    }
    public CompletableFuture<ChatProfile> toggleIgnore(UUID player,UUID target,Channel fallback){
        if(player.equals(target))throw new IllegalArgumentException("Não é possível ignorar a si mesmo");
        return storage.transact(c->{
            boolean exists;try(var s=prepare(c,"SELECT 1 FROM chat_ignores WHERE owner_uuid=? AND target_uuid=?",player,target);var rows=s.executeQuery()){exists=rows.next();}
            if(exists)update(c,"DELETE FROM chat_ignores WHERE owner_uuid=? AND target_uuid=?",player,target);
            else{try(var s=prepare(c,"SELECT COUNT(*) FROM chat_ignores WHERE owner_uuid=?",player);var rows=s.executeQuery()){if(rows.next()&&rows.getInt(1)>=1000)throw new IllegalStateException("Máximo de1000 jogadores ignorados");}update(c,"INSERT INTO chat_ignores(owner_uuid,target_uuid) VALUES(?,?)",player,target);}
            return read(c,player,fallback);
        });
    }
    public CompletableFuture<ChatProfile> unignore(UUID player,UUID target,Channel fallback){
        return storage.transact(c->{update(c,"DELETE FROM chat_ignores WHERE owner_uuid=? AND target_uuid=?",player,target);return read(c,player,fallback);});
    }
    public CompletableFuture<ChatProfile> mute(UUID actor,UUID target,Long expiresAt,String reason,Channel fallback){
        if(expiresAt!=null&&(expiresAt<0||reason==null||reason.isBlank()||reason.length()>256||reason.codePoints().anyMatch(Character::isISOControl)))throw new IllegalArgumentException("Mute inválido");
        return storage.transact(c->{
            if(expiresAt==null)update(c,"DELETE FROM chat_mutes WHERE player_uuid=?",target);
            else update(c,"INSERT INTO chat_mutes(player_uuid,expires_at,reason,actor_uuid,created_at) VALUES(?,?,?,?,?) ON CONFLICT(player_uuid) DO UPDATE SET expires_at=excluded.expires_at,reason=excluded.reason,actor_uuid=excluded.actor_uuid,created_at=excluded.created_at",target,expiresAt,reason,actor,clock.millis());
            update(c,"INSERT INTO admin_audit(actor_uuid,action,created_at) VALUES(?,?,?)",actor,(expiresAt==null?"unmute ":"mute ")+target+(expiresAt==null?"":" expires="+expiresAt+" reason="+reason),java.time.Instant.ofEpochMilli(clock.millis()).toString());
            return read(c,target,fallback);
        });
    }
}
