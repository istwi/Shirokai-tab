package com.shirokai.core.cosmetics;

import com.shirokai.core.storage.SqliteStorage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CosmeticRepositoryTest {
    @TempDir Path root;
    private SqliteStorage db;
    private CosmeticRepository repo;
    private Path legacy;
    private <T>T await(CompletableFuture<T> value)throws Exception{return value.get(10,TimeUnit.SECONDS);}
    @BeforeEach void start()throws Exception{legacy=root.resolve("legacy");Files.createDirectories(legacy);db=new SqliteStorage(root.resolve("core.db"));repo=new CosmeticRepository(db,legacy);await(repo.initialize(root.resolve("config/cosmetics.json"),root.resolve("absent.json")));}
    @AfterEach void stop()throws Exception{db.close();}
    @Test void importsOnlyCosmeticFieldsAndPreservesSourceBytes()throws Exception{
        var id=UUID.randomUUID();String text="{\"schemaVersion\":2,\"nickname\":\"SecretNick\",\"homes\":{\"home\":{}},\"cosmetics\":{\"nick\":\"nick_gold\",\"trail\":\"trail_fire\",\"pet\":\"pet_legacy\"},\"cosmeticsHidden\":true,\"cosmeticPreferences\":{\"speed\":3,\"size\":1,\"density\":3,\"direction\":-1}}";
        Files.writeString(legacy.resolve(id+".json"),text);var profile=await(repo.load(id));assertEquals("nick_gold",profile.state().cosmetics.get("nick"));assertEquals("pet_legacy",profile.state().cosmetics.get("pet"));assertTrue(profile.state().cosmeticsHidden);assertEquals(3,profile.state().cosmeticPreferences.speed);assertEquals(-1,profile.state().cosmeticPreferences.direction);assertEquals(text,Files.readString(legacy.resolve(id+".json")));
    }
    @Test void existingCoreChoiceIsNeverOverwrittenByLegacyOnReconnect()throws Exception{
        var id=UUID.randomUUID();Files.writeString(legacy.resolve(id+".json"),"{\"cosmetics\":{\"nick\":\"nick_gold\"}}");var first=await(repo.load(id));var state=first.state().copy();state.cosmetics.put("nick","nick_cyan");await(repo.save(id,first.revision(),state));Files.writeString(legacy.resolve(id+".json"),"{\"cosmetics\":{\"nick\":\"nick_rainbow\"}}");assertEquals("nick_cyan",await(repo.load(id)).state().cosmetics.get("nick"));
    }
    @Test void concurrentSavesUseRevisionAndDoNotLoseWinner()throws Exception{
        var id=UUID.randomUUID();var first=await(repo.load(id));var a=first.state().copy();a.cosmetics.put("aura","aura_stars");var b=first.state().copy();b.cosmetics.put("aura","aura_fire");var one=repo.save(id,0,a);var two=repo.save(id,0,b);assertEquals(1,await(one).revision());assertThrows(ExecutionException.class,()->await(two));assertEquals("aura_stars",await(repo.load(id)).state().cosmetics.get("aura"));
    }
    @Test void malformedLegacyIsRejectedWithoutWritingOverItAndCanBeRetried()throws Exception{
        var id=UUID.randomUUID();var file=legacy.resolve(id+".json");Files.writeString(file,"{broken");assertThrows(ExecutionException.class,()->await(repo.load(id)));assertEquals("{broken",Files.readString(file));Files.writeString(file,"{\"cosmetics\":{\"nick\":\"nick_gold\"}}");assertEquals("nick_gold",await(repo.load(id)).state().cosmetics.get("nick"));
    }
    @Test void invalidPreferenceFailsBeforeDatabaseWrite()throws Exception{
        var id=UUID.randomUUID();var profile=await(repo.load(id));var invalid=profile.state().copy();invalid.cosmeticPreferences.direction=0;assertThrows(ExecutionException.class,()->await(repo.save(id,0,invalid)));assertEquals(0,await(repo.load(id)).revision());
    }
    @Test void selectionsAndPreferencesSurviveRestartWithoutLegacyFile()throws Exception{
        var id=UUID.randomUUID();var first=await(repo.load(id));var state=first.state().copy();state.cosmetics.put("raid","raid_cherry");state.cosmeticsHidden=true;state.cosmeticPreferences.density=3;await(repo.save(id,0,state));db.close();db=new SqliteStorage(root.resolve("core.db"));repo=new CosmeticRepository(db,legacy);await(repo.initialize(root.resolve("config/cosmetics.json"),root.resolve("absent.json")));var restored=await(repo.load(id));assertEquals("raid_cherry",restored.state().cosmetics.get("raid"));assertTrue(restored.state().cosmeticsHidden);assertEquals(3,restored.state().cosmeticPreferences.density);assertEquals(1,restored.revision());
    }
    @Test void legacySettingsAreCopiedOnceWithoutTouchingSource()throws Exception{
        var old=root.resolve("old.json");String source="{\"viewDistance\":16,\"maxDensity\":1}";Files.writeString(old,source);var target=root.resolve("copied/cosmetics.json");var value=await(repo.initialize(target,old));assertEquals(16,value.viewDistance);assertEquals(1,value.maxDensity);assertEquals(source,Files.readString(old));Files.writeString(old,"{\"viewDistance\":64}");assertEquals(16,await(repo.initialize(target,old)).viewDistance);
    }
    @Test void invalidReloadPreservesConfigBytes()throws Exception{
        var file=root.resolve("config/cosmetics.json");Files.writeString(file,"{\"maxParticlesPerTick\":999999}");assertThrows(ExecutionException.class,()->await(repo.reload(file)));assertTrue(Files.readString(file).contains("999999"));
    }
    @Test void stateCopiesDoNotMutateCachedProfile(){var state=new CosmeticState();state.cosmetics.put("nick","nick_gold");var copy=state.copy();copy.cosmetics.put("nick","nick_cyan");copy.cosmeticPreferences.speed=3;assertEquals("nick_gold",state.cosmetics.get("nick"));assertEquals(2,state.cosmeticPreferences.speed);}
}
