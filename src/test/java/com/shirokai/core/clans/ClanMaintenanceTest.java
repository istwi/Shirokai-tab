package com.shirokai.core.clans;

import com.shirokai.core.storage.SqliteStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.*;

class ClanMaintenanceTest {
    @TempDir Path directory;
    private final Clock clock=Clock.fixed(Instant.ofEpochMilli(10000),ZoneOffset.UTC);
    @Test void previewIncludesMembershipsOfReferencedPlayers()throws Exception{
        try(var storage=new SqliteStorage(directory.resolve("referenced.db"))){
            var repo=new ClanRepository(storage,clock);var config=ClanConfig.defaults();
            UUID leader=UUID.randomUUID(),target=UUID.randomUUID();var clan=repo.create(leader,"Source",config).get();
            repo.invite(leader,target,config).get();
            storage.transact(c->{try(var s=c.createStatement()){s.executeUpdate("UPDATE clan_invites SET expires_at=0");}return null;}).get();
            var plan=repo.maintenance().preview(clan.id().toString()).get();
            repo.create(target,"Elsewhere",config).get();
            assertThrows(ExecutionException.class,()->repo.maintenance().repair(leader,plan).get());
            assertTrue(repo.maintenance().history(clan.id().toString(),0).get().stream().noneMatch(e->e.action().equals("admin-repair")));
        }
    }
    @Test void repairRestoresOnlyRecordedLeaderAndAuditsAtomically()throws Exception{
        var file=directory.resolve("leader.db");try(var storage=new SqliteStorage(file)){
            var repo=new ClanRepository(storage,clock);UUID leader=UUID.randomUUID();var clan=repo.create(leader,"Repair",ClanConfig.defaults()).get();
            try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var s=c.createStatement()){s.execute("PRAGMA foreign_keys=OFF");s.executeUpdate("DELETE FROM clan_members");}
            var maintenance=repo.maintenance();var plan=maintenance.preview(clan.id().toString()).get();assertFalse(plan.report().healthy());assertFalse(plan.report().safeActions().isEmpty());
            var result=maintenance.repair(UUID.randomUUID(),plan).get();assertTrue(result.remaining().healthy());
            assertThrows(ExecutionException.class,()->maintenance.memberHistory(UUID.randomUUID(),clan.id(),0).get());
            assertFalse(maintenance.memberHistory(leader,clan.id(),0).get().isEmpty());
            assertEquals(leader,repo.find(clan.id().toString()).get().leader());assertEquals(Set.of(leader),repo.find(clan.id().toString()).get().members().keySet());
            assertTrue(maintenance.history(clan.id().toString(),0).get().stream().anyMatch(e->e.action().equals("admin-repair")));
        }
    }
    @Test void stalePreviewDoesNotCleanNewStateAndHistorySurvivesDisband()throws Exception{
        try(var storage=new SqliteStorage(directory.resolve("stale.db"))){
            var repo=new ClanRepository(storage,clock);var config=ClanConfig.defaults();UUID leader=UUID.randomUUID(),target=UUID.randomUUID();var clan=repo.create(leader,"History",config).get();repo.invite(leader,target,config).get();
            storage.transact(c->{try(var s=c.createStatement()){s.executeUpdate("UPDATE clan_invites SET expires_at=0");}return null;}).get();
            var plan=repo.maintenance().preview(clan.id().toString()).get();assertFalse(plan.report().safeActions().isEmpty());
            repo.invite(leader,UUID.randomUUID(),config).get();assertThrows(ExecutionException.class,()->repo.maintenance().repair(leader,plan).get());
            var fresh=repo.maintenance().preview(clan.id().toString()).get();var result=repo.maintenance().repair(leader,fresh).get();assertTrue(result.changes()>0);assertTrue(result.remaining().healthy());
            repo.disband(leader,false,config).get();repo.disband(leader,true,config).get();assertFalse(repo.maintenance().history(clan.id().toString(),0).get().isEmpty());
        }
    }
    @Test void auditFailureRollsBackAllRepairsAndHistory()throws Exception{
        try(var storage=new SqliteStorage(directory.resolve("rollback.db"))){
            var repo=new ClanRepository(storage,clock);UUID leader=UUID.randomUUID();var clan=repo.create(leader,"Rollback",ClanConfig.defaults()).get();repo.invite(leader,UUID.randomUUID(),ClanConfig.defaults()).get();
            storage.transact(c->{try(var s=c.createStatement()){s.executeUpdate("UPDATE clan_invites SET expires_at=0");s.execute("CREATE TRIGGER reject_repair BEFORE INSERT ON admin_audit WHEN NEW.action LIKE 'clan repair %' BEGIN SELECT RAISE(ABORT,'audit failure'); END");}return null;}).get();
            var plan=repo.maintenance().preview(clan.id().toString()).get();assertThrows(ExecutionException.class,()->repo.maintenance().repair(leader,plan).get());
            assertFalse(repo.maintenance().diagnose(clan.id().toString()).get().healthy());assertTrue(repo.maintenance().history(clan.id().toString(),0).get().stream().noneMatch(e->e.action().equals("admin-repair")));
        }
    }
    @Test void cannotMoveLeaderFromAnotherClanOrDeleteOrphanMembers()throws Exception{
        var file=directory.resolve("unsafe.db");try(var storage=new SqliteStorage(file)){
            var repo=new ClanRepository(storage,clock);UUID a=UUID.randomUUID(),b=UUID.randomUUID();var first=repo.create(a,"First",ClanConfig.defaults()).get();var second=repo.create(b,"Second",ClanConfig.defaults()).get();
            try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var s=c.createStatement()){s.execute("PRAGMA foreign_keys=OFF");s.executeUpdate("UPDATE clans SET leader_uuid='"+b+"' WHERE id='"+first.id()+"'");}
            var plan=repo.maintenance().preview(first.id().toString()).get();assertTrue(plan.report().issues().stream().anyMatch(i->i.contains("MANUAL")));assertTrue(plan.report().safeActions().isEmpty());
            assertThrows(ExecutionException.class,()->repo.maintenance().repair(a,plan).get());assertEquals(second.id(),repo.membership(b).get().orElseThrow().id());
            assertTrue(repo.maintenance().databaseHealth().get().stream().anyMatch(i->i.startsWith("FK:")));
        }
    }
}
