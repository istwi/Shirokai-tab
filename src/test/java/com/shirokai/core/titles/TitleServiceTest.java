package com.shirokai.core.titles;

import com.shirokai.core.storage.SqliteStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class TitleServiceTest {
    @TempDir Path directory;
    @Test void primaryRankUpdatesWithoutEquippingAndSurvivesClear() throws Exception {
        var server=Executors.newSingleThreadExecutor();
        try(var storage=new SqliteStorage(directory.resolve("data.db"))){
            UUID player=UUID.randomUUID();
            var group=new java.util.concurrent.atomic.AtomicReference<String>("vip");
            var vip=TitleRulesTest.definition("vip",TitleDefinition.Category.ROLE,1,"group.vip");
            var owner=TitleRulesTest.definition("owner",TitleDefinition.Category.ROLE,100,"group.owner");
            var event=TitleRulesTest.definition("event",TitleDefinition.Category.SPECIAL,0,null);
            var service=new TitleService(storage,TitleRulesTest.catalog(vip,owner,event),(id,node)->true,server,id->true,Clock.systemUTC(),(m,e)->fail(e));
            service.bindRanks(id->new com.shirokai.core.permissions.PermissionService.Metadata(group.get(),"","",0));
            assertEquals(Optional.of("[vip]"),server.submit(()->service.placeholder(player,"shirokai:selected_title")).get());
            server.submit(()->service.grant(null,player,"event",0)).get().get();
            server.submit(()->service.equip(player,"event",true)).get().get();
            assertEquals(List.of("vip","event"),server.submit(()->service.chatDefinitions(player).stream().map(TitleDefinition::id).toList()).get());
            group.set("owner");
            assertEquals(Optional.of("[owner]"),server.submit(()->service.placeholder(player,"shirokai:selected_title")).get());
            server.submit(()->service.clear(player)).get().get();
            assertEquals(List.of("owner"),server.submit(()->service.chatDefinitions(player).stream().map(TitleDefinition::id).toList()).get());
            assertThrows(ExecutionException.class,()->server.submit(()->service.equip(player,"vip",true)).get().get());
            group.set("builder");assertEquals("[BUILDER]",server.submit(()->service.visualPresentation(player).getString()).get());
            group.set(null);assertTrue(server.submit(()->service.chatDefinitions(player)).get().isEmpty());
        }finally{server.shutdown();assertTrue(server.awaitTermination(10,TimeUnit.SECONDS));}
    }
    @Test void offlineDiagnosticNeverErasesSelectionsBecauseLuckPermsUserIsUnloaded() throws Exception {
        var server=Executors.newSingleThreadExecutor();
        try(var storage=new SqliteStorage(directory.resolve("data.db"))) {
            UUID player=UUID.randomUUID();storage.saveTitles(new TitleState(player,0,Map.of(),"vip",List.of("vip"))).get();
            var role=TitleRulesTest.definition("vip",TitleDefinition.Category.ROLE,1,"group.vip");
            var service=new TitleService(storage,TitleRulesTest.catalog(role),(id,node)->false,server,id->false,Clock.systemUTC(),(m,e)->fail(e));
            server.submit(()->service.view(player)).get().get();
            assertEquals("vip",storage.titleState(player).get().selectedVisual());
            assertTrue(server.submit(()->service.cached(player)).get().isEmpty());
        } finally { server.shutdown();assertTrue(server.awaitTermination(10,TimeUnit.SECONDS)); }
    }
    @Test void expiredGrantCannotBeSelectedAndClearIsDurable() throws Exception {
        var server=Executors.newSingleThreadExecutor();
        try(var storage=new SqliteStorage(directory.resolve("data.db"))) {
            UUID player=UUID.randomUUID();var event=TitleRulesTest.definition("event",TitleDefinition.Category.SPECIAL,0,null);
            storage.grantTitle(player,"event",50,null).get();
            var service=new TitleService(storage,TitleRulesTest.catalog(event),(id,node)->false,server,id->true,Clock.fixed(Instant.ofEpochMilli(100),ZoneOffset.UTC),(m,e)->fail(e));
            assertThrows(ExecutionException.class,()->server.submit(()->service.equip(player,"event",false)).get().get());
            assertThrows(IllegalArgumentException.class,()->service.grant(null,player,"event",99));
            server.submit(()->service.grant(null,player,"event",0)).get().get();
            assertThrows(ExecutionException.class,()->server.submit(()->service.equip(player,"event",false)).get().get());
            server.submit(()->service.equip(player,"event",true)).get().get();
            var cleared=server.submit(()->service.clear(player)).get().get();
            assertNull(cleared.selectedVisual());assertTrue(cleared.chatIds().isEmpty());assertTrue(cleared.grants().containsKey("event"));
        } finally { server.shutdown();assertTrue(server.awaitTermination(10,TimeUnit.SECONDS)); }
    }
}
