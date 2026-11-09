package com.shirokai.core.clans;

import com.shirokai.core.storage.SqliteStorage;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** All mutations and history are committed together on the storage worker. */
public final class ClanRepository {
    private final SqliteStorage storage;
    private final Clock clock;
    private final UUID expectedClan;
    public ClanRepository(SqliteStorage storage,Clock clock) { this(storage,clock,null); }
    private ClanRepository(SqliteStorage storage,Clock clock,UUID expectedClan){this.storage=storage;this.clock=clock;this.expectedClan=expectedClan;}
    public ClanMaintenance maintenance(){return new ClanMaintenance(storage,clock);}
    public ClanRepository scoped(UUID id){return new ClanRepository(storage,clock,Objects.requireNonNull(id));}
    private <T> CompletableFuture<T> mutation(UUID actor,SqliteStorage.DatabaseWork<T> work){
        return storage.transact(c->{
            if(expectedClan!=null)require(clanOf(c,actor).equals(expectedClan),"Clã mudou desde a abertura do menu; atualize a tela");
            return work.run(c);
        });
    }
    private static PreparedStatement prepare(Connection c,String sql,Object...args) throws SQLException {
        var s=c.prepareStatement(sql);
        for(int i=0;i<args.length;i++)s.setObject(i+1,args[i] instanceof UUID?args[i].toString():args[i]);
        return s;
    }
    private static int update(Connection c,String sql,Object...args) throws SQLException {
        try(var s=prepare(c,sql,args)){return s.executeUpdate();}
    }
    private static String text(Connection c,String sql,Object...args) throws SQLException {
        try(var s=prepare(c,sql,args);var rows=s.executeQuery()){return rows.next()?rows.getString(1):null;}
    }
    private static long number(Connection c,String sql,Object...args) throws SQLException {
        try(var s=prepare(c,sql,args);var rows=s.executeQuery()){return rows.next()?rows.getLong(1):0;}
    }
    private static void require(boolean valid,String message) { if(!valid)throw new IllegalStateException(message); }
    private static UUID clanOf(Connection c,UUID player) throws SQLException {
        String id=text(c,"SELECT clan_id FROM clan_members WHERE player_uuid=?",player);
        require(id!=null,"Jogador não pertence a um clã");return UUID.fromString(id);
    }
    private static UUID lookup(Connection c,String name) throws SQLException {
        String id=text(c,"SELECT id FROM clans WHERE name_key=? OR id=?",ClanConfig.key(name),name);
        require(id!=null,"Clã não encontrado");return UUID.fromString(id);
    }
    private static ClanSnapshot snapshot(Connection c,UUID id) throws SQLException {
        try(var s=prepare(c,"SELECT name,tag,leader_uuid,next_name_edit,next_tag_edit FROM clans WHERE id=?",id);var row=s.executeQuery()) {
            require(row.next(),"Clã não encontrado");
            UUID leader=UUID.fromString(row.getString(3));var members=new HashMap<UUID,ClanSnapshot.Role>();
            try(var ms=prepare(c,"SELECT player_uuid,role FROM clan_members WHERE clan_id=?",id);var rows=ms.executeQuery()) {
                while(rows.next()){UUID player=UUID.fromString(rows.getString(1));members.put(player,player.equals(leader)?ClanSnapshot.Role.LEADER:ClanSnapshot.Role.valueOf(rows.getString(2)));}
            }
            return new ClanSnapshot(id,row.getString(1),row.getString(2),leader,row.getLong(4),row.getLong(5),members);
        }
    }
    private static ClanSnapshot own(Connection c,UUID actor) throws SQLException { return snapshot(c,clanOf(c,actor)); }
    private static void leader(ClanSnapshot clan,UUID actor) {require(clan.leader().equals(actor),"Somente o líder pode executar esta ação");}
    private static void manage(ClanSnapshot clan,UUID actor) {require(clan.members().get(actor)!=ClanSnapshot.Role.MEMBER,"Somente líder/oficial pode executar esta ação");}
    private static void clearClanSelection(Connection c,UUID player,UUID clan)throws SQLException{
        String title="clan_"+clan;
        update(c,"UPDATE title_profiles SET selected_visual=CASE WHEN selected_visual=? THEN NULL ELSE selected_visual END,revision=revision+1 WHERE player_uuid=? AND (selected_visual=? OR EXISTS(SELECT 1 FROM chat_identifications WHERE player_uuid=? AND title_id=?))",title,player,title,player,title);
        update(c,"DELETE FROM chat_identifications WHERE player_uuid=? AND title_id=?",player,title);
    }
    private void history(Connection c,UUID clan,UUID actor,String action,String details) throws SQLException {
        update(c,"INSERT INTO clan_history(clan_id,actor_uuid,action,details,created_at) VALUES(?,?,?,?,?)",clan,actor==null?new UUID(0,0):actor,action,details,clock.millis());
        update(c,"INSERT INTO admin_audit(actor_uuid,action,created_at) VALUES(?,?,?)",actor,"clan "+action+" "+clan+" "+details,java.time.Instant.ofEpochMilli(clock.millis()).toString());
    }
    public CompletableFuture<Optional<ClanSnapshot>> membership(UUID player){return storage.transact(c->{
        String id=text(c,"SELECT clan_id FROM clan_members WHERE player_uuid=?",player);
        return id==null?Optional.empty():Optional.of(snapshot(c,UUID.fromString(id)));
    });}
    public record TransferOffer(UUID clan,UUID from,long expiresAt){}
    public CompletableFuture<Optional<TransferOffer>> transferOffer(UUID player){return storage.transact(c->{
        try(var s=prepare(c,"SELECT clan_id,from_uuid,expires_at FROM clan_transfers WHERE to_uuid=? AND expires_at>?",player,clock.millis());var rows=s.executeQuery()){
            return rows.next()?Optional.of(new TransferOffer(UUID.fromString(rows.getString(1)),UUID.fromString(rows.getString(2)),rows.getLong(3))):Optional.empty();
        }
    });}
    public CompletableFuture<Map<String,Long>> invitations(UUID player) {
        return storage.transact(c -> {
            var result = new LinkedHashMap<String,Long>();
            try(var s=prepare(c,"SELECT clans.name,clan_invites.expires_at FROM clan_invites JOIN clans ON clans.id=clan_invites.clan_id WHERE target_uuid=? AND expires_at>? ORDER BY expires_at",player,clock.millis());var rows=s.executeQuery()) {
                while(rows.next())result.put(rows.getString(1),rows.getLong(2));
            }
            return Map.copyOf(result);
        });
    }
    public CompletableFuture<ClanSnapshot> own(UUID player) {return storage.transact(c->own(c,player));}
    public CompletableFuture<ClanSnapshot> find(String name) {return storage.transact(c->snapshot(c,lookup(c,name)));}
    public CompletableFuture<List<String>> list(int page) {
        if(page<0)throw new IllegalArgumentException("Página inválida");
        return storage.transact(c->{var result=new ArrayList<String>();
            try(var s=prepare(c,"SELECT id,name FROM clans ORDER BY name_key,id LIMIT 20 OFFSET ?",(long)page*20);var rows=s.executeQuery()){
                while(rows.next())result.add(rows.getString(1)+" "+rows.getString(2));
            }return List.copyOf(result);
        });
    }
    public CompletableFuture<ClanSnapshot> adminIdentity(UUID actor,ClanSnapshot expected,String value,boolean tag,ClanConfig config){
        String validated=tag?config.tag(value):config.name(value);
        return storage.transact(c->{
            var clan=snapshot(c,expected.id());
            require(Objects.equals(tag?clan.tag():clan.name(),tag?expected.tag():expected.name()),"Nome/sigla mudou; consulte novamente");
            String column=tag?"tag":"name";
            update(c,"UPDATE clans SET "+column+"=?,"+column+"_key=? WHERE id=?",validated,ClanConfig.key(validated),clan.id());
            history(c,clan.id(),actor,tag?"admin-tag":"admin-rename",validated);
            return snapshot(c,clan.id());
        });
    }
    public CompletableFuture<ClanSnapshot> create(UUID actor,String name,ClanConfig config) {
        return mutation(actor,c->{
            require(text(c,"SELECT clan_id FROM clan_members WHERE player_uuid=?",actor)==null,"Jogador já pertence a um clã");
            require(number(c,"SELECT next_at FROM clan_creation_cooldowns WHERE player_uuid=?",actor)<=clock.millis(),"Cooldown de criação ativo");
            require(text(c,"SELECT id FROM clans WHERE name_key=?",ClanConfig.key(name))==null,"Nome de clã já utilizado");
            UUID id=UUID.randomUUID();long now=clock.millis();
            update(c,"INSERT INTO clans(id,name,name_key,leader_uuid,created_at) VALUES(?,?,?,?,?)",id,name,ClanConfig.key(name),actor,now);
            update(c,"INSERT INTO clan_members(player_uuid,clan_id,role,joined_at) VALUES(?,?,'MEMBER',?)",actor,id,now);
            update(c,"INSERT INTO clan_creation_cooldowns(player_uuid,next_at) VALUES(?,?) ON CONFLICT(player_uuid) DO UPDATE SET next_at=excluded.next_at",actor,now+config.creationCooldownSeconds()*1000);
            history(c,id,actor,"create",name);return snapshot(c,id);
        });
    }
    public CompletableFuture<ClanSnapshot> identity(UUID actor,String value,boolean tag,ClanConfig config) {return identity(actor,value,tag,config,null);}
    public CompletableFuture<ClanSnapshot> identity(UUID actor,String value,boolean tag,ClanConfig config,ClanSnapshot expected) {
        return mutation(actor,c->{
            var clan=own(c,actor);ClanPolicy.require(clan,actor,tag?"tag":"rename",config);
            if(expected!=null)require(expected.id().equals(clan.id())&&Objects.equals(tag?expected.tag():expected.name(),tag?clan.tag():clan.name()),"Nome/sigla mudou desde a abertura do editor; reabra a tela");
            long now=clock.millis();
            require(now>=(tag?clan.nextTagEdit():clan.nextNameEdit()),"Cooldown de alteração ativo");
            require(!value.equals(tag?clan.tag():clan.name()),"Identidade não foi alterada");
            String column=tag?"tag":"name", cooldown=tag?"next_tag_edit":"next_name_edit";
            String existing=text(c,"SELECT id FROM clans WHERE "+column+"_key=? AND id<>?",ClanConfig.key(value),clan.id());
            require(existing==null,"Nome/sigla já utilizado");
            update(c,"UPDATE clans SET "+column+"=?,"+column+"_key=?,"+cooldown+"=? WHERE id=?",value,ClanConfig.key(value),now+(tag?config.tagCooldownSeconds():config.nameCooldownSeconds())*1000,clan.id());
            history(c,clan.id(),actor,tag?"tag":"rename",value);return snapshot(c,clan.id());
        });
    }
    public CompletableFuture<ClanSnapshot> invite(UUID actor,UUID target,ClanConfig config) {
        return mutation(actor,c->{
            var clan=own(c,actor);ClanPolicy.require(clan,actor,"invite",config);long now=clock.millis();
            require(text(c,"SELECT clan_id FROM clan_members WHERE player_uuid=?",target)==null,"Convidado já pertence a um clã");
            require(number(c,"SELECT next_at FROM clan_invite_cooldowns WHERE clan_id=? AND actor_uuid=? AND target_uuid=?",clan.id(),actor,target)<=now,"Cooldown de convite ativo");
            require(number(c,"SELECT COUNT(*) FROM clan_invites WHERE clan_id=? AND expires_at>?",clan.id(),now)<config.maximumInvitations(),"Limite de convites pendentes atingido");
            require(number(c,"SELECT COUNT(*) FROM clan_invites WHERE target_uuid=? AND expires_at>?",target,now)<config.maximumInvitations(),"Jogador recebeu convites demais");
            require(number(c,"SELECT expires_at FROM clan_invites WHERE clan_id=? AND target_uuid=?",clan.id(),target)<=now,"Convite já está pendente");
            update(c,"INSERT INTO clan_invites(clan_id,target_uuid,inviter_uuid,expires_at) VALUES(?,?,?,?) ON CONFLICT(clan_id,target_uuid) DO UPDATE SET inviter_uuid=excluded.inviter_uuid,expires_at=excluded.expires_at",clan.id(),target,actor,now+config.invitationSeconds()*1000);
            update(c,"INSERT INTO clan_invite_cooldowns(clan_id,actor_uuid,target_uuid,next_at) VALUES(?,?,?,?) ON CONFLICT(clan_id,actor_uuid,target_uuid) DO UPDATE SET next_at=excluded.next_at",clan.id(),actor,target,now+config.invitationCooldownSeconds()*1000);
            history(c,clan.id(),actor,"invite",target.toString());return clan;
        });
    }
    public CompletableFuture<ClanSnapshot> accept(UUID actor,UUID clanId,UUID expectedLeader,int limit) {
        return mutation(actor,c->{
            var clan=snapshot(c,clanId);require(clan.leader().equals(expectedLeader),"Liderança mudou; tente aceitar novamente");
            require(text(c,"SELECT clan_id FROM clan_members WHERE player_uuid=?",actor)==null,"Jogador já pertence a um clã");
            require(number(c,"SELECT expires_at FROM clan_invites WHERE clan_id=? AND target_uuid=?",clanId,actor)>clock.millis(),"Convite ausente ou expirado");
            require(clan.members().size()<limit,"Clã sem vagas; membros existentes serão mantidos");
            update(c,"INSERT INTO clan_members(player_uuid,clan_id,role,joined_at) VALUES(?,?,'MEMBER',?)",actor,clanId,clock.millis());
            update(c,"DELETE FROM clan_invites WHERE target_uuid=?",actor);
            history(c,clanId,actor,"join","");return snapshot(c,clanId);
        });
    }
    public CompletableFuture<ClanSnapshot> decline(UUID actor,String name) {
        return mutation(actor,c->{UUID id=lookup(c,name);require(update(c,"DELETE FROM clan_invites WHERE clan_id=? AND target_uuid=?",id,actor)==1,"Convite não encontrado");history(c,id,actor,"decline","");return snapshot(c,id);});
    }
    public CompletableFuture<Map<UUID,Long>> sentInvitations(UUID actor) {
        return mutation(actor,c->{
            var clan=own(c,actor);var result=new LinkedHashMap<UUID,Long>();
            try(var s=prepare(c,"SELECT target_uuid,expires_at FROM clan_invites WHERE clan_id=? AND expires_at>? ORDER BY expires_at,target_uuid",clan.id(),clock.millis());var rows=s.executeQuery()){
                while(rows.next())result.put(UUID.fromString(rows.getString(1)),rows.getLong(2));
            }
            return Map.copyOf(result);
        });
    }
    public CompletableFuture<ClanSnapshot> cancel(UUID actor,UUID target,ClanConfig config) {
        return mutation(actor,c->{var clan=own(c,actor);ClanPolicy.require(clan,actor,"invite",config);require(update(c,"DELETE FROM clan_invites WHERE clan_id=? AND target_uuid=?",clan.id(),target)==1,"Convite não encontrado");history(c,clan.id(),actor,"cancel-invite",target.toString());return clan;});
    }
    public CompletableFuture<ClanSnapshot> leave(UUID actor) {
        return mutation(actor,c->{var clan=own(c,actor);require(!clan.leader().equals(actor),"Líder deve transferir liderança ou confirmar exclusão");clearClanSelection(c,actor,clan.id());update(c,"DELETE FROM clan_members WHERE player_uuid=?",actor);update(c,"DELETE FROM clan_transfers WHERE clan_id=? AND to_uuid=?",clan.id(),actor);history(c,clan.id(),actor,"leave","");return snapshot(c,clan.id());});
    }
    public CompletableFuture<ClanSnapshot> memberAction(UUID actor,UUID target,String action,ClanConfig config) {
        return mutation(actor,c->{
            var clan=own(c,actor);require(clan.members().containsKey(target),"Jogador não pertence a este clã");require(!actor.equals(target),"Use o comando de saída para si mesmo");require(!clan.leader().equals(target),"Líder não pode ser removido/rebaixado");
            ClanPolicy.require(clan,actor,action,config);
            switch(action) {
                case "kick" -> {require(clan.leader().equals(actor)||clan.members().get(target).ordinal()>clan.members().get(actor).ordinal(),"Oficial não pode expulsar outro oficial");clearClanSelection(c,target,clan.id());update(c,"DELETE FROM clan_members WHERE player_uuid=?",target);update(c,"DELETE FROM clan_transfers WHERE clan_id=? AND to_uuid=?",clan.id(),target);}
                case "promote","demote" -> {require(clan.leader().equals(actor)||clan.members().get(target).ordinal()>clan.members().get(actor).ordinal(),"Não é permitido alterar cargo de alguém da mesma hierarquia ou superior");update(c,"UPDATE clan_members SET role=? WHERE player_uuid=?",action.equals("promote")?"OFFICER":"MEMBER",target);}
                default -> throw new IllegalArgumentException("Ação de membro inválida");
            }
            history(c,clan.id(),actor,action,target.toString());return snapshot(c,clan.id());
        });
    }
    public CompletableFuture<ClanSnapshot> requestTransfer(UUID actor,UUID target,ClanConfig config) {
        return mutation(actor,c->{var clan=own(c,actor);leader(clan,actor);require(!target.equals(actor)&&clan.members().containsKey(target),"Sucessor deve ser outro membro do clã");update(c,"INSERT INTO clan_transfers(clan_id,from_uuid,to_uuid,expires_at) VALUES(?,?,?,?) ON CONFLICT(clan_id) DO UPDATE SET from_uuid=excluded.from_uuid,to_uuid=excluded.to_uuid,expires_at=excluded.expires_at",clan.id(),actor,target,clock.millis()+config.confirmationSeconds()*1000);history(c,clan.id(),actor,"transfer-request",target.toString());return clan;});
    }
    public CompletableFuture<ClanSnapshot> answerTransfer(UUID actor,boolean accept) {
        return mutation(actor,c->{var clan=own(c,actor);require(number(c,"SELECT expires_at FROM clan_transfers WHERE clan_id=? AND to_uuid=? AND from_uuid=?",clan.id(),actor,clan.leader())>clock.millis(),"Solicitação de transferência ausente ou expirada");if(accept){update(c,"UPDATE clan_members SET role='MEMBER' WHERE player_uuid IN (?,?)",actor,clan.leader());update(c,"UPDATE clans SET leader_uuid=? WHERE id=?",actor,clan.id());update(c,"DELETE FROM clan_disband_confirmations WHERE clan_id=?",clan.id());}update(c,"DELETE FROM clan_transfers WHERE clan_id=?",clan.id());history(c,clan.id(),actor,accept?"transfer-accept":"transfer-decline",clan.leader().toString());return snapshot(c,clan.id());});
    }
    public CompletableFuture<ClanSnapshot> cancelDisband(UUID actor){return mutation(actor,c->{
        var clan=own(c,actor);leader(clan,actor);update(c,"DELETE FROM clan_disband_confirmations WHERE clan_id=?",clan.id());history(c,clan.id(),actor,"disband-cancel","");return clan;
    });}
    public CompletableFuture<ClanSnapshot> disband(UUID actor,boolean confirm,ClanConfig config) {
        return mutation(actor,c->{var clan=own(c,actor);leader(clan,actor);if(!confirm){update(c,"INSERT INTO clan_disband_confirmations(clan_id,actor_uuid,expires_at) VALUES(?,?,?) ON CONFLICT(clan_id) DO UPDATE SET actor_uuid=excluded.actor_uuid,expires_at=excluded.expires_at",clan.id(),actor,clock.millis()+config.confirmationSeconds()*1000);history(c,clan.id(),actor,"disband-request","");}else{require(number(c,"SELECT expires_at FROM clan_disband_confirmations WHERE clan_id=? AND actor_uuid=?",clan.id(),actor)>clock.millis(),"Confirmação ausente ou expirada");history(c,clan.id(),actor,"disband",clan.name());for(UUID member:clan.members().keySet())clearClanSelection(c,member,clan.id());update(c,"DELETE FROM clans WHERE id=?",clan.id());}return clan;});
    }
}
