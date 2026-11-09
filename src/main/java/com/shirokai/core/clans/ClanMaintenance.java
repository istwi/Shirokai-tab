package com.shirokai.core.clans;

import com.shirokai.core.storage.SqliteStorage;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Explicit, optimistic repairs. Never changes the recorded leader or deletes clans/members. */
public final class ClanMaintenance {
    public record Event(long id,String actor,String action,String details,long at){}
    public record Report(UUID clan,List<String> issues,List<String> safeActions){public boolean healthy(){return issues.isEmpty();}}
    public record Plan(UUID clan,String token,long at,Report report){}
    public record Result(Report remaining,Set<UUID> affected,int changes){}
    private final SqliteStorage storage;private final Clock clock;
    public ClanMaintenance(SqliteStorage storage,Clock clock){this.storage=storage;this.clock=clock;}
    private static PreparedStatement statement(Connection c,String sql,Object...args)throws SQLException{
        var s=c.prepareStatement(sql);for(int i=0;i<args.length;i++)s.setObject(i+1,args[i] instanceof UUID?args[i].toString():args[i]);return s;
    }
    private static String text(Connection c,String sql,Object...args)throws SQLException{
        try(var s=statement(c,sql,args);var r=s.executeQuery()){return r.next()?r.getString(1):null;}
    }
    private static int count(Connection c,String sql,Object...args)throws SQLException{
        try(var s=statement(c,sql,args);var r=s.executeQuery()){return r.next()?r.getInt(1):0;}
    }
    private static int update(Connection c,String sql,Object...args)throws SQLException{try(var s=statement(c,sql,args)){return s.executeUpdate();}}
    private static UUID resolve(Connection c,String id)throws SQLException{
        try{return UUID.fromString(id);}catch(IllegalArgumentException ignored){}
        String found=text(c,"SELECT id FROM clans WHERE name_key=?",ClanConfig.key(id));
        if(found==null)throw new IllegalArgumentException("Clã não encontrado; para histórico de clã excluído, use UUID");return UUID.fromString(found);
    }
    public CompletableFuture<List<Event>> history(String id,int page){
        if(page<0||page>1000000)throw new IllegalArgumentException("Página inválida");
        return storage.transact(c->{UUID clan=resolve(c,id);var result=new ArrayList<Event>();
            try(var s=statement(c,"SELECT id,actor_uuid,action,details,created_at FROM clan_history WHERE clan_id=? ORDER BY id DESC LIMIT 10 OFFSET ?",clan,(long)page*10);var rows=s.executeQuery()){
                while(rows.next())result.add(new Event(rows.getLong(1),rows.getString(2),rows.getString(3),rows.getString(4),rows.getLong(5)));
            }return List.copyOf(result);
        });
    }
    public CompletableFuture<List<Event>> memberHistory(UUID actor,UUID clan,int page){
        if(page<0||page>1000000)throw new IllegalArgumentException("Página inválida");
        return storage.transact(c->{
            if(count(c,"SELECT count(*) FROM clan_members WHERE player_uuid=? AND clan_id=?",actor,clan)!=1)throw new IllegalStateException("Clã mudou; reabra o histórico");
            var result=new ArrayList<Event>();
            try(var s=statement(c,"SELECT id,actor_uuid,action,details,created_at FROM clan_history WHERE clan_id=? ORDER BY id DESC LIMIT 10 OFFSET ?",clan,(long)page*10);var rows=s.executeQuery()){while(rows.next())result.add(new Event(rows.getLong(1),rows.getString(2),rows.getString(3),rows.getString(4),rows.getLong(5)));}
            return List.copyOf(result);
        });
    }
    private static final String BAD_INVITES="(expires_at<=? OR NOT EXISTS(SELECT 1 FROM clans WHERE id=clan_invites.clan_id) OR NOT EXISTS(SELECT 1 FROM clan_members WHERE clan_id=clan_invites.clan_id AND player_uuid=inviter_uuid) OR EXISTS(SELECT 1 FROM clan_members WHERE player_uuid=target_uuid))";
    private static final String BAD_TRANSFERS="(expires_at<=? OR NOT EXISTS(SELECT 1 FROM clans WHERE id=clan_transfers.clan_id AND leader_uuid=from_uuid) OR NOT EXISTS(SELECT 1 FROM clan_members WHERE clan_id=clan_transfers.clan_id AND player_uuid=to_uuid) OR from_uuid=to_uuid)";
    private static final String BAD_CONFIRMATIONS="(expires_at<=? OR NOT EXISTS(SELECT 1 FROM clans WHERE id=clan_disband_confirmations.clan_id AND leader_uuid=actor_uuid))";
    private static Report report(Connection c,UUID clan,long at)throws SQLException{
        var issues=new ArrayList<String>();var actions=new ArrayList<String>();
        String leader=text(c,"SELECT leader_uuid FROM clans WHERE id=?",clan);
        if(leader==null)issues.add("Clã não existe; histórico preservado. Nenhum membro será excluído.");
        else{
            try{
                UUID.fromString(leader);String membership=text(c,"SELECT clan_id FROM clan_members WHERE player_uuid=?",leader);
                if(!clan.toString().equals(membership)){
                    issues.add("Líder registrado sem associação válida ao clã");
                    if(membership==null)actions.add("Restaurar associação do líder registrado, sem mudar liderança");
                    else issues.add("MANUAL: líder já pertence a outro clã; nenhuma transferência automática");
                }
            }catch(IllegalArgumentException invalid){issues.add("MANUAL: UUID do líder inválido");}
        }
        for(var row:List.of(new String[]{"clan_invites",BAD_INVITES,"convites"},new String[]{"clan_transfers",BAD_TRANSFERS,"transferências pendentes"},new String[]{"clan_disband_confirmations",BAD_CONFIRMATIONS,"confirmações pendentes"})){
            int n=count(c,"SELECT count(*) FROM "+row[0]+" WHERE clan_id=? AND "+row[1],clan,at);
            if(n>0){issues.add(n+" "+row[2]+" expirados/inválidos");actions.add("Limpar apenas "+row[2]+" expirados/inválidos");}
        }
        String title="clan_"+clan;
        int selections=count(c,"SELECT count(*) FROM chat_identifications t WHERE title_id=? AND NOT EXISTS(SELECT 1 FROM clan_members m JOIN clans c ON c.id=m.clan_id WHERE m.player_uuid=t.player_uuid AND m.clan_id=?)",title,clan);
        int visual=count(c,"SELECT count(*) FROM title_profiles t WHERE selected_visual=? AND NOT EXISTS(SELECT 1 FROM clan_members m JOIN clans c ON c.id=m.clan_id WHERE m.player_uuid=t.player_uuid AND m.clan_id=?)",title,clan);
        if(selections+visual>0){issues.add("Identificações de clã sem associação válida: "+(selections+visual));actions.add("Limpar somente identificações inválidas deste clã");}
        int orphans=count(c,"SELECT count(*) FROM clan_members WHERE clan_id=? AND NOT EXISTS(SELECT 1 FROM clans WHERE id=clan_id)",clan);
        if(orphans>0)issues.add("MANUAL: "+orphans+" membros órfãos; não serão apagados");
        return new Report(clan,List.copyOf(issues),List.copyOf(actions));
    }
    private static String fingerprint(Connection c,UUID clan)throws Exception{
        var data=new StringBuilder();
        for(String table:List.of("clans","clan_members","clan_invites","clan_transfers","clan_disband_confirmations")){
            String where=table.equals("clans")?"id":"clan_id";
            try(var s=statement(c,"SELECT * FROM "+table+" WHERE "+where+"=? ORDER BY 1,2",clan);var r=s.executeQuery()){
                data.append(table);int columns=r.getMetaData().getColumnCount();while(r.next())for(int i=1;i<=columns;i++){String value=r.getString(i);data.append(value==null?"-1:":value.length()+":"+value);}
            }
        }
        try(var s=statement(c,"SELECT * FROM clan_members WHERE player_uuid IN(SELECT leader_uuid FROM clans WHERE id=? UNION SELECT target_uuid FROM clan_invites WHERE clan_id=? UNION SELECT inviter_uuid FROM clan_invites WHERE clan_id=? UNION SELECT from_uuid FROM clan_transfers WHERE clan_id=? UNION SELECT to_uuid FROM clan_transfers WHERE clan_id=? UNION SELECT actor_uuid FROM clan_disband_confirmations WHERE clan_id=?) ORDER BY player_uuid",clan,clan,clan,clan,clan,clan);var r=s.executeQuery()){
            data.append("referenced-members");while(r.next())for(int i=1;i<=r.getMetaData().getColumnCount();i++){String value=r.getString(i);data.append(value==null?"-1:":value.length()+":"+value);}
        }
        for(String sql:List.of("SELECT * FROM title_profiles WHERE selected_visual=? ORDER BY player_uuid","SELECT * FROM chat_identifications WHERE title_id=? ORDER BY player_uuid,slot")){
            try(var s=statement(c,sql,"clan_"+clan);var r=s.executeQuery()){while(r.next())for(int i=1;i<=r.getMetaData().getColumnCount();i++){String value=r.getString(i);data.append(value==null?"-1:":value.length()+":"+value);}}
        }
        return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    public CompletableFuture<Report> diagnose(String id){return storage.transact(c->report(c,resolve(c,id),clock.millis()));}
    public CompletableFuture<List<String>> databaseHealth(){return storage.transact(c->{var result=new ArrayList<String>();
        try(var s=c.createStatement();var rows=s.executeQuery("PRAGMA quick_check")){while(rows.next())result.add("SQLite: "+rows.getString(1));}
        try(var s=c.createStatement();var rows=s.executeQuery("PRAGMA foreign_key_check")){while(rows.next()&&result.size()<101)result.add("FK: "+rows.getString(1)+" row="+rows.getString(2)+" parent="+rows.getString(3));}
        return List.copyOf(result);
    });}
    public CompletableFuture<Plan> preview(String id){return storage.transact(c->{UUID clan=resolve(c,id);long at=clock.millis();return new Plan(clan,fingerprint(c,clan),at,report(c,clan,at));});}
    public CompletableFuture<Result> repair(UUID actor,Plan expected){return storage.transact(c->{
        if(clock.millis()<expected.at()||clock.millis()-expected.at()>120000)throw new IllegalStateException("Prévia expirou; solicite outra");
        if(!fingerprint(c,expected.clan()).equals(expected.token()))throw new ConcurrentModificationException("Dados mudaram após a prévia; nenhum reparo aplicado");
        UUID clan=expected.clan();var before=report(c,clan,expected.at());if(before.safeActions().isEmpty())throw new IllegalStateException("Nenhum reparo automático seguro disponível");
        var affected=new HashSet<UUID>();
        try(var s=statement(c,"SELECT player_uuid FROM clan_members WHERE clan_id=? UNION SELECT player_uuid FROM chat_identifications WHERE title_id=? UNION SELECT player_uuid FROM title_profiles WHERE selected_visual=?",clan,"clan_"+clan,"clan_"+clan);var rows=s.executeQuery()){while(rows.next())try{affected.add(UUID.fromString(rows.getString(1)));}catch(IllegalArgumentException ignored){}}
        int changes=0;String leader=text(c,"SELECT leader_uuid FROM clans WHERE id=?",clan);
        if(leader!=null)try{UUID uuid=UUID.fromString(leader);affected.add(uuid);if(text(c,"SELECT clan_id FROM clan_members WHERE player_uuid=?",uuid)==null)changes+=update(c,"INSERT INTO clan_members(player_uuid,clan_id,role,joined_at) VALUES(?,?,'MEMBER',?)",uuid,clan,expected.at());}catch(IllegalArgumentException ignored){}
        for(var row:List.of(new String[]{"clan_invites",BAD_INVITES},new String[]{"clan_transfers",BAD_TRANSFERS},new String[]{"clan_disband_confirmations",BAD_CONFIRMATIONS}))changes+=update(c,"DELETE FROM "+row[0]+" WHERE clan_id=? AND "+row[1],clan,expected.at());
        String title="clan_"+clan;
        changes+=update(c,"UPDATE title_profiles SET selected_visual=NULL,revision=revision+1 WHERE selected_visual=? AND NOT EXISTS(SELECT 1 FROM clan_members m JOIN clans c ON c.id=m.clan_id WHERE m.player_uuid=title_profiles.player_uuid AND m.clan_id=?)",title,clan);
        update(c,"UPDATE title_profiles SET revision=revision+1 WHERE player_uuid IN(SELECT t.player_uuid FROM chat_identifications t WHERE t.title_id=? AND NOT EXISTS(SELECT 1 FROM clan_members m JOIN clans c ON c.id=m.clan_id WHERE m.player_uuid=t.player_uuid AND m.clan_id=?))",title,clan);
        changes+=update(c,"DELETE FROM chat_identifications WHERE title_id=? AND NOT EXISTS(SELECT 1 FROM clan_members m JOIN clans c ON c.id=m.clan_id WHERE m.player_uuid=chat_identifications.player_uuid AND m.clan_id=?)",title,clan);
        String detail="changes="+changes+" actions="+before.safeActions();
        update(c,"INSERT INTO clan_history(clan_id,actor_uuid,action,details,created_at) VALUES(?,?,'admin-repair',?,?)",clan,actor==null?new UUID(0,0):actor,detail,clock.millis());
        update(c,"INSERT INTO admin_audit(actor_uuid,action,created_at) VALUES(?,?,?)",actor,"clan repair "+clan+" "+detail,java.time.Instant.ofEpochMilli(clock.millis()).toString());
        return new Result(report(c,clan,clock.millis()),Set.copyOf(affected),changes);
    });}
}
