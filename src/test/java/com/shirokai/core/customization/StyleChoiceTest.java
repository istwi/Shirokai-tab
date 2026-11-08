package com.shirokai.core.customization;

import com.shirokai.core.clans.*;
import com.shirokai.core.storage.SqliteStorage;
import com.shirokai.core.presentation.Surface;
import com.shirokai.core.styles.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class StyleChoiceTest {
    @TempDir Path directory;
    private Path db(){return directory.resolve("data.db");}
    private static void sql(Path db,String... commands)throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+db);var s=c.createStatement()){for(String command:commands)s.execute(command);}
    }
    @Test void independentClanFieldsPersistAndStaleRevisionCannotOverwrite()throws Exception {
        UUID leader=UUID.randomUUID();UUID clan;
        try(var storage=new SqliteStorage(db())){
            clan=new ClanRepository(storage,Clock.systemUTC()).create(leader,"Styles",ClanConfig.defaults()).get().id();
            var repo=new StyleChoiceRepository(storage);var initial=repo.load(clan,true).get();
            var name=repo.save(leader,initial,Surface.CLAN_NAME,"gold").get();
            assertThrows(ExecutionException.class,()->repo.save(leader,initial,Surface.CLAN_TAG,"sunset").get());
            repo.save(leader,name,Surface.CLAN_TAG,"basic").get();
        }
        try(var storage=new SqliteStorage(db())){
            var choices=new StyleChoiceRepository(storage).load(clan,true).get();
            assertEquals(Map.of(Surface.CLAN_NAME,"gold",Surface.CLAN_TAG,"basic"),choices.selections());
            assertEquals(2,choices.revision());assertEquals("ok",storage.integrityCheck().get());
        }
    }
    @Test void personalChoiceCasAndAuditRollbackPreserveOtherData()throws Exception {
        UUID actor=UUID.randomUUID();
        try(var storage=new SqliteStorage(db())){
            var repo=new StyleChoiceRepository(storage);var initial=repo.load(actor,false).get();
            var one=repo.save(actor,initial,Surface.TITLE,"gold");var two=repo.save(actor,initial,Surface.TITLE,"basic");
            one.get();assertThrows(ExecutionException.class,two::get);
            var saved=repo.load(actor,false).get();
            sql(db(),"CREATE TRIGGER reject_style BEFORE INSERT ON admin_audit BEGIN SELECT RAISE(ABORT,'audit failure'); END");
            assertThrows(ExecutionException.class,()->repo.save(actor,saved,Surface.TITLE,"basic").get());
            assertEquals(saved,repo.load(actor,false).get());
            assertThrows(ExecutionException.class,()->repo.save(UUID.randomUUID(),saved,Surface.TITLE,null).get());
        }
    }
    @Test void exactClanMembershipLeadershipAndDeletionAreRevalidated()throws Exception {
        UUID leader=UUID.randomUUID(),member=UUID.randomUUID(),other=UUID.randomUUID();
        try(var storage=new SqliteStorage(db())){
            var clans=new ClanRepository(storage,Clock.systemUTC());var config=ClanConfig.defaults();
            var clan=clans.create(leader,"Original",config).get();clans.invite(leader,member,config).get();clans.accept(member,clan.id(),leader,20).get();
            var repo=new StyleChoiceRepository(storage);var old=repo.load(clan.id(),true).get();
            assertThrows(ExecutionException.class,()->repo.save(member,old,Surface.CLAN_NAME,"basic").get());
            clans.requestTransfer(leader,member,config).get();clans.answerTransfer(member,true).get();
            assertThrows(ExecutionException.class,()->repo.save(leader,old,Surface.CLAN_NAME,"basic").get());
            var fresh=repo.save(member,old,Surface.CLAN_NAME,"gold").get();
            clans.create(other,"Different",config).get();
            assertThrows(ExecutionException.class,()->repo.save(other,fresh,Surface.CLAN_TAG,"basic").get());
            clans.disband(member,false,config).get();clans.disband(member,true,config).get();
            assertThrows(ExecutionException.class,()->repo.save(member,fresh,Surface.CLAN_TAG,"basic").get());
            assertTrue(repo.load(clan.id(),true).get().selections().isEmpty());
        }
    }
    @Test void upgradeFiveBacksUpAllExistingDataBeforeTransactionalMigration()throws Exception {
        UUID player=UUID.randomUUID();
        try(var storage=new SqliteStorage(db())){
            storage.setScoreboard(player,false).get();storage.grantTitle(player,"traveler",0,null).get();
            new ClanRepository(storage,Clock.systemUTC()).create(player,"Preserved",ClanConfig.defaults()).get();
        }
        sql(db(),"DROP TABLE clan_style_choices","DROP TABLE player_style_choices","PRAGMA user_version=5");
        try(var storage=new SqliteStorage(db())){
            storage.ready().get();
            assertFalse(storage.preferences(player).get().scoreboardEnabled());
            assertTrue(storage.titleState(player).get().grants().containsKey("traveler"));
            assertEquals("Preserved",new ClanRepository(storage,Clock.systemUTC()).own(player).get().name());
            var repo=new StyleChoiceRepository(storage);repo.save(player,repo.load(player,false).get(),Surface.TITLE,"basic").get();
        }
        List<Path> backups;try(var paths=Files.list(directory)){backups=paths.filter(p->p.toString().contains(".schema5-")).toList();}
        assertEquals(1,backups.size());
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+backups.getFirst());var s=c.createStatement();var row=s.executeQuery("PRAGMA user_version")){
            assertTrue(row.next());assertEquals(5,row.getInt(1));
        }
        try(var storage=new SqliteStorage(db())){assertEquals("basic",new StyleChoiceRepository(storage).load(player,false).get().selections().get(Surface.TITLE));}
    }
    @Test void failedSixMigrationRollsBackFirstTableAndKeepsSchemaFive()throws Exception {
        try(var storage=new SqliteStorage(db())){storage.ready().get();}
        sql(db(),"DROP TABLE player_style_choices","PRAGMA user_version=5");
        try(var storage=new SqliteStorage(db())){assertThrows(ExecutionException.class,()->storage.ready().get());}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+db());var s=c.createStatement()){
            try(var row=s.executeQuery("PRAGMA user_version")){assertTrue(row.next());assertEquals(5,row.getInt(1));}
            try(var row=s.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name='player_style_choices'")){assertTrue(row.next());assertEquals(0,row.getInt(1));}
        }
    }
    @Test void catalogLossRemovalAndRecoveryNeverDestroySavedId()throws Exception {
        var styles=new StyleEngine(StyleConfig.defaults());var file=directory.resolve("presets.json");var catalog=PresetCatalog.load(file,styles);
        String original=Files.readString(file);PresetCatalog.load(file,styles);assertEquals(original,Files.readString(file));
        assertEquals(PresetCatalog.Status.UNAVAILABLE,catalog.resolve("gold",Surface.TITLE,n->false).status());
        assertEquals(PresetCatalog.Status.ACTIVE,catalog.resolve("gold",Surface.TITLE,n->true).status());
        assertEquals("gold",catalog.resolve("gold",Surface.TITLE,n->false).saved());
        var removed=new PresetCatalog("basic",Map.of("basic",catalog.presets().get("basic").definition()),styles);
        assertEquals(PresetCatalog.Status.REMOVED,removed.resolve("gold",Surface.TITLE,n->true).status());
        assertEquals(PresetCatalog.Status.ACTIVE,catalog.resolve("gold",Surface.TITLE,n->true).status());
        assertFalse(catalog.allowed("sunset",Surface.CLAN_NAME,n->n.equals("shirokaicore.style.sunset")));
        assertTrue(catalog.allowed("sunset",Surface.CLAN_NAME,n->true));
        assertEquals("<click:/op>😀",styles.render(catalog.resolve("gold",Surface.TITLE,n->true).template(),k->"<click:/op>😀",0,false).getString());
    }
    @Test void catalogRejectsUnsafeTemplatesUnknownSurfacesAndPremiumFallback()throws Exception {
        var styles=new StyleEngine(StyleConfig.defaults());String valid=Files.readString(createCatalog(styles));
        assertThrows(Exception.class,()->PresetCatalog.parse(valid.replace("\"schemaVersion\": 1","\"schemaVersion\": \"1\""),styles));
        assertThrows(Exception.class,()->PresetCatalog.parse(valid.replace("CLAN_TAG","MENTION"),styles));
        assertThrows(Exception.class,()->PresetCatalog.parse(valid.replace("<aqua>%visual:value%</aqua>","<aqua>%visual:value%%visual:value%</aqua>"),styles));
        assertThrows(Exception.class,()->PresetCatalog.parse(valid.replace("\"fallback\": \"basic\"","\"fallback\": \"gold\""),styles));
        assertThrows(Exception.class,()->PresetCatalog.parse(valid.replace("<aqua>%visual:value%</aqua>","<click:/op>%visual:value%</click>"),styles));
    }
    private Path createCatalog(StyleEngine styles)throws Exception{var file=directory.resolve("presets.json");PresetCatalog.load(file,styles);return file;}
    @Test void staleCatalogTicketRejectsQueuedWrite()throws Exception {
        UUID actor=UUID.randomUUID();
        try(var storage=new SqliteStorage(db())){
            var repo=new StyleChoiceRepository(storage);var initial=repo.load(actor,false).get();
            assertThrows(ExecutionException.class,()->repo.save(actor,initial,Surface.TITLE,"gold",()->false).get());
            assertEquals(initial,repo.load(actor,false).get());
        }
    }
}
