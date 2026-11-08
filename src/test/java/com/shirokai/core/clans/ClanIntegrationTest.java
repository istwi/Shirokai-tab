package com.shirokai.core.clans;

import com.google.gson.*;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.storage.SqliteStorage;
import com.shirokai.core.titles.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ClanIntegrationTest {
    @TempDir Path directory;
    @Test void configurableRolesStillCannotAffectLeaderOrPeers()throws Exception{
        var json=new Gson().toJsonTree(ClanConfig.defaults()).getAsJsonObject();var actions=json.getAsJsonObject("roleActions");
        var officer=new JsonArray();for(String action:List.of("invite","promote","kick","demote","tag"))officer.add(action);actions.add("OFFICER",officer);
        var config=ClanConfig.parse(json.toString());UUID leader=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID();
        try(var storage=new SqliteStorage(directory.resolve("data.db"))){
            var repo=new ClanRepository(storage,Clock.systemUTC());var clan=repo.create(leader,"Policies",config).get();
            for(UUID member:List.of(a,b)){repo.invite(leader,member,config).get();repo.accept(member,clan.id(),leader,10).get();}
            repo.memberAction(leader,a,"promote",config).get();
            repo.identity(a,"TAG",true,config).get();repo.memberAction(a,b,"promote",config).get();
            assertThrows(ExecutionException.class,()->repo.memberAction(a,b,"demote",config).get());
            assertThrows(ExecutionException.class,()->repo.memberAction(a,leader,"kick",config).get());
            json.getAsJsonObject("roleActions").add("OFFICER",new JsonArray());var disabled=ClanConfig.parse(json.toString());
            assertThrows(ExecutionException.class,()->repo.cancel(a,b,disabled).get());
        }
    }
    @Test void staleClanScopeCannotMutateNewMembership()throws Exception{
        try(var storage=new SqliteStorage(directory.resolve("data.db"))){
            var repo=new ClanRepository(storage,Clock.systemUTC());var config=ClanConfig.defaults();UUID old=UUID.randomUUID(),next=UUID.randomUUID(),member=UUID.randomUUID();
            var first=repo.create(old,"FirstClan",config).get();var second=repo.create(next,"OtherClan",config).get();
            repo.invite(old,member,config).get();repo.accept(member,first.id(),old,10).get();var stale=repo.scoped(first.id());
            repo.leave(member).get();repo.invite(next,member,config).get();repo.accept(member,second.id(),next,10).get();
            assertThrows(ExecutionException.class,()->stale.leave(member).get());assertEquals(second.id(),repo.own(member).get().id());
            assertThrows(ExecutionException.class,()->stale.answerTransfer(member,true).get());
        }
    }
    @Test void nativeClanTagSharesTwoSlotsAndCannotBeForgedOrResurrectedAfterLeave()throws Exception{
        var main=Executors.newSingleThreadExecutor();
        try(var storage=new SqliteStorage(directory.resolve("data.db"))){
            UUID leader=UUID.randomUUID(),member=UUID.randomUUID(),outsider=UUID.randomUUID();
            var repo=new ClanRepository(storage,Clock.systemUTC());var config=ClanConfig.defaults();
            var permissions=new PermissionService(){public boolean allowed(UUID id,String node){return true;}public String provider(){return "test";}};
            var clans=new ClanService(repo,permissions,main,config);main.submit(()->clans.cacheOnline(id->true)).get();
            var titleService=new TitleService(storage,TitleCatalog.defaults(),(id,node)->true,main,id->true,Clock.systemUTC(),(message,error)->{});
            main.submit(()->titleService.bindClans(id->clans.cached(id).flatMap(ClanTitle::from),id->clans.membership(id).thenApply(v->null))).get();
            var clan=main.submit(()->clans.create(leader,"Members",0)).get().get();main.submit(()->clans.identity(leader,"MBR",true)).get().get();
            main.submit(()->clans.invite(leader,member)).get().get();main.submit(()->clans.accept(member,"Members")).get().get();
            String tag=ClanTitle.id(clan.id());
            assertThrows(ExecutionException.class,()->main.submit(()->titleService.equip(member,tag,false)).get().get());
            main.submit(()->titleService.equip(member,"traveler",true)).get().get();
            var selection=main.submit(()->titleService.equip(member,tag,true)).get().get();assertEquals(List.of("traveler",tag),selection.chatIds());
            assertTrue(main.submit(()->titleService.placeholder(member,"shirokai:selected_title")).get().isEmpty());
            assertEquals(Optional.of("[MBR]"),main.submit(()->titleService.placeholder(member,"shirokai:chat_tag_2")).get());
            assertThrows(ExecutionException.class,()->main.submit(()->titleService.equip(outsider,tag,true)).get().get());
            long revision=selection.revision();repo.leave(member).get();var after=storage.titleState(member).get();
            assertNull(after.selectedVisual());assertEquals(List.of("traveler"),after.chatIds());assertTrue(after.revision()>revision);
            var forged=new TitleState(member,after.revision(),Map.of(),tag,List.of(tag));assertThrows(ExecutionException.class,()->storage.saveTitles(forged).get());
            main.submit(()->titleService.view(member)).get().get();assertTrue(main.submit(()->titleService.placeholder(member,"shirokai:selected_title")).get().isEmpty());
        }finally{main.shutdown();assertTrue(main.awaitTermination(10,TimeUnit.SECONDS));}
    }
    @Test void disbandAndKickClearOnlyMatchingClanTagAtomically()throws Exception{
        try(var storage=new SqliteStorage(directory.resolve("data.db"))){
            var config=ClanConfig.defaults();var repo=new ClanRepository(storage,Clock.systemUTC());UUID leader=UUID.randomUUID(),member=UUID.randomUUID();
            var clan=repo.create(leader,"Removed",config).get();repo.identity(leader,"REM",true,config).get();repo.invite(leader,member,config).get();repo.accept(member,clan.id(),leader,10).get();String id=ClanTitle.id(clan.id());
            for(UUID player:List.of(leader,member))storage.saveTitles(new TitleState(player,0,Map.of(),id,List.of("traveler",id))).get();
            repo.memberAction(leader,member,"kick",config).get();assertNull(storage.titleState(member).get().selectedVisual());
            repo.disband(leader,false,config).get();repo.disband(leader,true,config).get();assertNull(storage.titleState(leader).get().selectedVisual());assertEquals(List.of("traveler"),storage.titleState(leader).get().chatIds());
        }
    }
}
