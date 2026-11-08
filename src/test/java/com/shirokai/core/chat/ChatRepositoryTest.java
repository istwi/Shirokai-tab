package com.shirokai.core.chat;

import com.shirokai.core.storage.SqliteStorage;
import com.shirokai.core.clans.*;
import com.shirokai.core.titles.TitleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Clock;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatRepositoryTest {
    @TempDir Path directory;
    @Test void channelIgnoreAndMutePersistAcrossRestart()throws Exception{
        Path db=directory.resolve("data.db");UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        try(var storage=new SqliteStorage(db)){
            var repo=new ChatRepository(storage,Clock.systemUTC());repo.channel(a,Channel.LOCAL).get();repo.toggleIgnore(a,b,Channel.GLOBAL).get();repo.mute(null,a,0L,"Permanent",Channel.GLOBAL).get();
        }
        try(var storage=new SqliteStorage(db)){
            var repo=new ChatRepository(storage,Clock.systemUTC());var state=repo.profile(a,Channel.GLOBAL).get();assertEquals(Channel.LOCAL,state.channel());assertEquals(Set.of(b),state.ignored());assertTrue(state.mute().active(Long.MAX_VALUE));
            assertTrue(repo.toggleIgnore(a,b,Channel.GLOBAL).get().ignored().isEmpty());assertNull(repo.mute(null,a,null,"",Channel.GLOBAL).get().mute());
        }
    }
    @Test void moderationAuditFailureRollsBackMute()throws Exception{
        Path db=directory.resolve("data.db");UUID target=UUID.randomUUID();
        try(var storage=new SqliteStorage(db)){
            storage.ready().get();try(var c=DriverManager.getConnection("jdbc:sqlite:"+db);var s=c.createStatement()){s.execute("CREATE TRIGGER fail_chat_audit BEFORE INSERT ON admin_audit BEGIN SELECT RAISE(ABORT,'audit failure'); END");}
            var repo=new ChatRepository(storage,Clock.systemUTC());assertThrows(ExecutionException.class,()->repo.mute(null,target,0L,"Reason",Channel.GLOBAL).get());assertNull(repo.profile(target,Channel.GLOBAL).get().mute());
        }
    }
    @Test void upgradeThreeBacksUpAndPreservesClansTitlesAndPreferences()throws Exception{
        Path db=directory.resolve("schema3.db");UUID player=UUID.randomUUID();
        try(var storage=new SqliteStorage(db)){
            storage.setScoreboard(player,false).get();storage.saveTitles(new TitleState(player,0,Map.of(),"traveler",List.of("traveler"))).get();new ClanRepository(storage,Clock.systemUTC()).create(player,"Preserved",ClanConfig.defaults()).get();
        }
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+db);var s=c.createStatement()){
            s.execute("DROP TABLE chat_profiles");s.execute("DROP TABLE chat_ignores");s.execute("DROP TABLE chat_mutes");s.execute("DROP TABLE clan_style_choices");s.execute("DROP TABLE player_style_choices");s.execute("ALTER TABLE player_preferences DROP COLUMN animations_enabled");s.execute("PRAGMA user_version=3");
        }
        try(var storage=new SqliteStorage(db)){
            assertFalse(storage.preferences(player).get().scoreboardEnabled());assertEquals("traveler",storage.titleState(player).get().selectedVisual());assertEquals("Preserved",new ClanRepository(storage,Clock.systemUTC()).own(player).get().name());
            assertNull(new ChatRepository(storage,Clock.systemUTC()).profile(player,Channel.GLOBAL).get().mute());assertEquals("ok",storage.integrityCheck().get());
        }
        List<Path> backups;try(var paths=Files.list(directory)){backups=paths.filter(p->p.getFileName().toString().contains(".schema3-")).toList();}assertEquals(1,backups.size());
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+backups.getFirst());var s=c.createStatement();var rows=s.executeQuery("PRAGMA user_version")){assertTrue(rows.next());assertEquals(3,rows.getInt(1));}
    }
    @Test void expiryBoundariesAndInvalidMuteAndIgnoreAreRejected()throws Exception{
        assertTrue(new ChatProfile.Mute(100,"Temporary").active(99));assertFalse(new ChatProfile.Mute(100,"Temporary").active(100));
        try(var storage=new SqliteStorage(directory.resolve("data.db"))){var repo=new ChatRepository(storage,Clock.systemUTC());UUID id=UUID.randomUUID();
            assertThrows(IllegalArgumentException.class,()->repo.toggleIgnore(id,id,Channel.GLOBAL));assertThrows(IllegalArgumentException.class,()->repo.mute(null,id,-1L,"Bad",Channel.GLOBAL));
            assertThrows(IllegalArgumentException.class,()->repo.mute(null,id,0L,"line\nbreak",Channel.GLOBAL));
        }
    }
}
