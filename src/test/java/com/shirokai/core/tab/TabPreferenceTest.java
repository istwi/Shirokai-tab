package com.shirokai.core.tab;

import com.shirokai.core.storage.SqliteStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TabPreferenceTest{
    @TempDir Path directory;
    @Test void animationAndScoreboardPreferencesRemainIndependentAndSurviveRestart()throws Exception{
        Path file=directory.resolve("data.db");UUID player=UUID.randomUUID();
        try(var storage=new SqliteStorage(file)){
            var initial=storage.preferences(player).get();assertTrue(initial.animationsEnabled());assertTrue(initial.scoreboardEnabled());
            storage.setScoreboard(player,false).get();storage.setAnimations(player,false).get();assertFalse(storage.preferences(player).get().scoreboardEnabled());
            storage.setAnimations(player,true).get();assertFalse(storage.preferences(player).get().scoreboardEnabled());
        }
        try(var storage=new SqliteStorage(file)){var result=storage.preferences(player).get();assertTrue(result.animationsEnabled());assertFalse(result.scoreboardEnabled());}
    }
    @Test void upgradeFourCreatesBackupBeforeAddingAnimationColumn()throws Exception{
        Path file=directory.resolve("schema4.db");UUID player=UUID.randomUUID();
        try(var storage=new SqliteStorage(file)){storage.setScoreboard(player,false).get();}
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var s=c.createStatement()){s.execute("DROP TABLE clan_style_choices");s.execute("DROP TABLE player_style_choices");s.execute("ALTER TABLE player_preferences DROP COLUMN animations_enabled");s.execute("PRAGMA user_version=4");}
        try(var storage=new SqliteStorage(file)){var result=storage.preferences(player).get();assertFalse(result.scoreboardEnabled());assertTrue(result.animationsEnabled());assertEquals("ok",storage.integrityCheck().get());}
        List<Path> backups;try(var files=Files.list(directory)){backups=files.filter(p->p.getFileName().toString().contains(".schema4-")).toList();}assertEquals(1,backups.size());
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+backups.getFirst());var s=c.createStatement();var row=s.executeQuery("PRAGMA user_version")){assertTrue(row.next());assertEquals(4,row.getInt(1));}
    }
}
