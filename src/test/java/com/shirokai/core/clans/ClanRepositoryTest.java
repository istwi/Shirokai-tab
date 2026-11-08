package com.shirokai.core.clans;

import com.shirokai.core.storage.SqliteStorage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class ClanRepositoryTest {
    @TempDir Path directory;
    static final class Time extends Clock {
        final AtomicLong millis=new AtomicLong(1000000);
        public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return Instant.ofEpochMilli(millis());}public long millis(){return millis.get();}
        void advance(long seconds){millis.addAndGet(seconds*1000);}
    }
    private static <T> T get(CompletableFuture<T> future)throws Exception{return future.get(10,TimeUnit.SECONDS);}
    @Test void createIsFreeAndManualTagUniquenessAndCooldownsAreIndependent()throws Exception{
        var clock=new Time();UUID leader=UUID.randomUUID(),other=UUID.randomUUID();var config=ClanConfig.defaults();
        try(var storage=new SqliteStorage(directory.resolve("data.db"))){
            var repo=new ClanRepository(storage,clock);var clan=get(repo.create(leader,"Akatsuki",config));assertNull(clan.tag());
            assertThrows(ExecutionException.class,()->get(repo.create(other,"aKATSUKI",config)));
            var second=get(repo.create(other,"Another",config));assertNotNull(second);
            clan=get(repo.identity(leader,"AKT",true,config));assertEquals("AKT",clan.tag());
            clan=get(repo.identity(leader,"Akatsuki New",false,config));assertEquals("Akatsuki New",clan.name());
            assertEquals(1,clan.members().size());assertEquals(ClanSnapshot.Role.LEADER,clan.members().get(leader));
            assertThrows(ExecutionException.class,()->get(repo.identity(leader,"NEXT",true,config)));
            assertThrows(ExecutionException.class,()->get(repo.identity(other,"akt",true,config)));
            clock.advance(config.tagCooldownSeconds());get(repo.identity(leader,"NEXT",true,config));
            // A failed duplicate tag must not start the other leader's cooldown.
            assertEquals("OTH",get(repo.identity(other,"OTH",true,config)).tag());
        }
    }
    @Test void concurrentAcceptOnlyFillsLastSeatAndLowerLimitNeverExpels()throws Exception{
        var clock=new Time();UUID leader=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID();var config=ClanConfig.defaults();
        try(var storage=new SqliteStorage(directory.resolve("data.db"))){
            var repo=new ClanRepository(storage,clock);var clan=get(repo.create(leader,"TestClan",config));
            get(repo.invite(leader,a,config));get(repo.invite(leader,b,config));
            var first=repo.accept(a,clan.id(),leader,2);var second=repo.accept(b,clan.id(),leader,2);
            get(first);assertThrows(ExecutionException.class,()->get(second));
            assertThrows(ExecutionException.class,()->get(repo.accept(b,clan.id(),leader,1)));
            assertEquals(2,get(repo.own(leader)).members().size());
            get(repo.accept(b,clan.id(),leader,3));assertEquals(3,get(repo.own(leader)).members().size());
            assertThrows(ExecutionException.class,()->get(repo.accept(b,clan.id(),leader,20)));
            get(repo.leave(a));assertEquals(2,get(repo.own(leader)).members().size());
        }
    }
    @Test void transferRequiresEligibleRecipientConfirmationAndInvalidatesDisband()throws Exception{
        var clock=new Time();UUID leader=UUID.randomUUID(),member=UUID.randomUUID(),stranger=UUID.randomUUID();var config=ClanConfig.defaults();
        try(var storage=new SqliteStorage(directory.resolve("data.db"))){
            var repo=new ClanRepository(storage,clock);var clan=get(repo.create(leader,"TestClan",config));
            get(repo.invite(leader,member,config));get(repo.accept(member,clan.id(),leader,2));
            assertThrows(ExecutionException.class,()->get(repo.answerTransfer(member,true)));
            get(repo.disband(leader,false,config));get(repo.requestTransfer(leader,member,config));
            assertEquals(leader,get(repo.own(leader)).leader());
            assertThrows(ExecutionException.class,()->get(repo.answerTransfer(stranger,true)));
            clan=get(repo.answerTransfer(member,true));assertEquals(member,clan.leader());assertEquals(ClanSnapshot.Role.MEMBER,clan.members().get(leader));
            assertThrows(ExecutionException.class,()->get(repo.disband(leader,true,config)));
            assertThrows(ExecutionException.class,()->get(repo.disband(member,true,config)));
            assertThrows(ExecutionException.class,()->get(repo.leave(member)));
            get(repo.disband(member,false,config));get(repo.disband(member,true,config));
            assertThrows(ExecutionException.class,()->get(repo.own(member)));
            assertThrows(ExecutionException.class,()->get(repo.own(leader)));
        }
    }
    @Test void expiredRequestsCannotBeAcceptedAndCancelDoesNotResetInviteCooldown()throws Exception{
        var clock=new Time();UUID leader=UUID.randomUUID(),member=UUID.randomUUID();var config=ClanConfig.defaults();
        try(var storage=new SqliteStorage(directory.resolve("data.db"))){
            var repo=new ClanRepository(storage,clock);var clan=get(repo.create(leader,"TestClan",config));
            get(repo.invite(leader,member,config));get(repo.cancel(leader,member,config));
            assertThrows(ExecutionException.class,()->get(repo.invite(leader,member,config)));
            clock.advance(config.invitationCooldownSeconds());get(repo.invite(leader,member,config));clock.advance(config.invitationSeconds());
            final UUID id=clan.id();assertThrows(ExecutionException.class,()->get(repo.accept(member,id,leader,3)));
            get(repo.invite(leader,member,config));get(repo.accept(member,id,leader,3));get(repo.requestTransfer(leader,member,config));
            clock.advance(config.confirmationSeconds());assertThrows(ExecutionException.class,()->get(repo.answerTransfer(member,true)));
            get(repo.disband(leader,false,config));clock.advance(config.confirmationSeconds());assertThrows(ExecutionException.class,()->get(repo.disband(leader,true,config)));
        }
    }
    @Test void rolesPreventEscalationAndForeignClanActions()throws Exception{
        var clock=new Time();UUID leader=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID(),foreign=UUID.randomUUID();var config=ClanConfig.defaults();
        try(var storage=new SqliteStorage(directory.resolve("data.db"))){
            var repo=new ClanRepository(storage,clock);var clan=get(repo.create(leader,"TestClan",config));get(repo.create(foreign,"Foreign",config));
            for(UUID id:List.of(a,b)){get(repo.invite(leader,id,config));get(repo.accept(id,clan.id(),leader,5));}
            assertThrows(ExecutionException.class,()->get(repo.memberAction(a,b,"kick",config)));
            get(repo.memberAction(leader,a,"promote",config));get(repo.memberAction(leader,b,"promote",config));
            assertThrows(ExecutionException.class,()->get(repo.memberAction(a,b,"kick",config)));
            assertThrows(ExecutionException.class,()->get(repo.memberAction(a,leader,"kick",config)));
            assertThrows(ExecutionException.class,()->get(repo.memberAction(a,b,"demote",config)));
            assertThrows(ExecutionException.class,()->get(repo.memberAction(leader,foreign,"kick",config)));
            get(repo.memberAction(leader,b,"demote",config));get(repo.memberAction(a,b,"kick",config));
            assertEquals(2,get(repo.own(leader)).members().size());
        }
    }
    @Test void persistenceAuditAndMembershipAreTransactional()throws Exception{
        Path file=directory.resolve("data.db");UUID leader=UUID.randomUUID();var config=ClanConfig.defaults();var clock=new Time();
        try(var storage=new SqliteStorage(file)){get(new ClanRepository(storage,clock).create(leader,"Persisted",config));}
        try(var storage=new SqliteStorage(file)){
            var repo=new ClanRepository(storage,clock);assertEquals("Persisted",get(repo.own(leader)).name());get(storage.ready());
            try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var s=c.createStatement()){
                s.execute("CREATE TRIGGER reject_clan_audit BEFORE INSERT ON admin_audit BEGIN SELECT RAISE(ABORT,'audit failure'); END");
            }
            assertThrows(ExecutionException.class,()->get(repo.identity(leader,"Changed",false,config)));
            assertEquals("Persisted",get(repo.own(leader)).name());
            assertThrows(ExecutionException.class,()->get(repo.create(UUID.randomUUID(),"Rollback",config)));
            assertThrows(ExecutionException.class,()->get(repo.find("Rollback")));
            assertEquals("ok",get(storage.integrityCheck()));
        }
    }
    @Test void identityEditorUsesExpectedValueAndPreservesIndependentField()throws Exception{
        UUID leader=UUID.randomUUID();var config=ClanConfig.defaults();
        try(var storage=new SqliteStorage(directory.resolve("editor.db"))){
            var repo=new ClanRepository(storage,new Time());var initial=get(repo.create(leader,"Original",config));
            var renamed=get(repo.identity(leader,"Renamed",false,config,initial));
            assertThrows(ExecutionException.class,()->get(repo.identity(leader,"Stale",false,config,initial)));
            var tagged=get(repo.identity(leader,"TAG",true,config,initial));
            assertEquals("Renamed",tagged.name());assertEquals("TAG",tagged.tag());
            assertEquals(renamed.nextNameEdit(),tagged.nextNameEdit());
        }
    }
    @Test void sentInvitesAreScopedExpiringAndCancellationRevalidatesRole()throws Exception{
        UUID leader=UUID.randomUUID(),member=UUID.randomUUID(),target=UUID.randomUUID();var config=ClanConfig.defaults();var clock=new Time();
        try(var storage=new SqliteStorage(directory.resolve("sent.db"))){
            var repo=new ClanRepository(storage,clock);var clan=get(repo.create(leader,"Inviters",config));
            get(repo.invite(leader,member,config));get(repo.accept(member,clan.id(),leader,20));get(repo.invite(leader,target,config));
            assertTrue(get(repo.scoped(clan.id()).sentInvitations(member)).containsKey(target));
            assertThrows(ExecutionException.class,()->get(repo.cancel(member,target,config)));
            assertThrows(ExecutionException.class,()->get(repo.scoped(UUID.randomUUID()).sentInvitations(leader)));
            get(repo.cancel(leader,target,config));assertTrue(get(repo.sentInvitations(leader)).isEmpty());
            clock.advance(config.invitationCooldownSeconds()+1);get(repo.invite(leader,target,config));clock.advance(config.invitationSeconds()+1);
            assertTrue(get(repo.sentInvitations(leader)).isEmpty());
        }
    }
    @Test void adminCorrectionValidatesUniquenessCasAndAuditWithoutChangingCooldowns()throws Exception{
        UUID leader=UUID.randomUUID(),other=UUID.randomUUID();var config=ClanConfig.defaults();Path db=directory.resolve("admin.db");
        try(var storage=new SqliteStorage(db)){
            var repo=new ClanRepository(storage,new Time());var initial=get(repo.create(leader,"Before",config));get(repo.create(other,"Occupied",config));
            var renamed=get(repo.identity(leader,"After",false,config));
            assertThrows(ExecutionException.class,()->get(repo.adminIdentity(null,initial,"Lost",false,config)));
            assertThrows(ExecutionException.class,()->get(repo.adminIdentity(null,renamed,"Occupied",false,config)));
            var corrected=get(repo.adminIdentity(null,renamed,"Corrected",false,config));
            assertEquals("Corrected",corrected.name());assertEquals(renamed.nextNameEdit(),corrected.nextNameEdit());assertEquals(renamed.members(),corrected.members());
            assertEquals(2,get(repo.list(0)).size());
            storage.transact(c->{try(var statement=c.createStatement()){statement.execute("CREATE TRIGGER reject_admin BEFORE INSERT ON admin_audit BEGIN SELECT RAISE(ABORT,'failure'); END");}return null;}).get();
            assertThrows(ExecutionException.class,()->get(repo.adminIdentity(null,corrected,"Rollback",false,config)));
            assertEquals("Corrected",get(repo.own(leader)).name());
        }
    }
    @Test void upgradeFromSchemaTwoPreservesTitlesAndCreatesReadableBackup()throws Exception{
        Path file=directory.resolve("schema2.db");UUID player=UUID.randomUUID();Class.forName("org.sqlite.JDBC");
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var s=c.createStatement()){
            s.execute("CREATE TABLE player_preferences(player_uuid TEXT PRIMARY KEY,scoreboard_enabled INTEGER)");
            s.execute("CREATE TABLE admin_audit(id INTEGER PRIMARY KEY,actor_uuid TEXT,action TEXT,created_at TEXT)");
            s.execute("CREATE TABLE title_profiles(player_uuid TEXT PRIMARY KEY,revision INTEGER,selected_visual TEXT)");
            s.execute("CREATE TABLE title_grants(player_uuid TEXT,title_id TEXT,expires_at INTEGER,PRIMARY KEY(player_uuid,title_id))");
            s.execute("CREATE TABLE chat_identifications(player_uuid TEXT,slot INTEGER,title_id TEXT,PRIMARY KEY(player_uuid,slot))");
            try(var insert=c.prepareStatement("INSERT INTO title_profiles VALUES(?,7,'traveler')")){insert.setString(1,player.toString());insert.executeUpdate();}
            s.execute("PRAGMA user_version=2");
        }
        try(var storage=new SqliteStorage(file)){
            var state=get(storage.titleState(player));assertEquals("traveler",state.selectedVisual());assertEquals(7,state.revision());
            assertEquals("Upgraded",get(new ClanRepository(storage,new Time()).create(player,"Upgraded",ClanConfig.defaults())).name());
        }
        List<Path> backups;try(var files=Files.list(directory)){backups=files.filter(path->path.getFileName().toString().contains(".schema2-")).toList();}
        assertEquals(1,backups.size());try(var c=DriverManager.getConnection("jdbc:sqlite:"+backups.getFirst());var s=c.createStatement();var rows=s.executeQuery("PRAGMA user_version")){assertTrue(rows.next());assertEquals(2,rows.getInt(1));}
    }
}
