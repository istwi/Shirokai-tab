package com.shirokai.core.chat;

import com.google.gson.*;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.storage.SqliteStorage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatServiceTest {
    @TempDir Path directory;
    private final UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID(),d=UUID.randomUUID(),clan=UUID.randomUUID();
    private ExecutorService main;private SqliteStorage storage;private ChatService service;
    private final Map<UUID,Set<String>> nodes=new HashMap<>();private final List<ChatService.Participant> people=new ArrayList<>();
    private final List<UUID> delivered=new ArrayList<>();private final List<ChatService.Envelope> messages=new ArrayList<>();
    private final ChatService.Sink sink=(id,message)->{delivered.add(id);messages.add(message);};
    private <T>T on(Callable<T> task)throws Exception{return main.submit(task).get(10,TimeUnit.SECONDS);}
    @BeforeEach void setup()throws Exception{
        main=Executors.newSingleThreadExecutor(r->new Thread(r,"chat-test-main"));storage=new SqliteStorage(directory.resolve("data.db"));
        people.addAll(List.of(new ChatService.Participant(a,"Alice","overworld",0,0,0,clan),new ChatService.Participant(b,"Bob","overworld",10,0,0,clan),
            new ChatService.Participant(c,"Carol","overworld",200,0,0,null),new ChatService.Participant(d,"Dave","nether",0,0,0,null)));
        var permission=new PermissionService(){public boolean allowed(UUID id,String node){assertEquals("chat-test-main",Thread.currentThread().getName());return nodes.getOrDefault(id,Set.of()).contains(node);}public String provider(){return "test";}};
        var json=new Gson().toJsonTree(ChatConfig.defaults()).getAsJsonObject();json.addProperty("cooldownMillis",0);json.addProperty("duplicateWindowSeconds",0);
        service=new ChatService(new ChatRepository(storage,Clock.systemUTC()),permission,main,()->List.copyOf(people),id->List.of("[VIP]","[CLAN]","[FORGED THIRD]"),Clock.systemUTC(),ChatConfig.parse(json.toString()),(id,reason)->{});
        for(UUID id:List.of(a,b,c,d))on(()->service.join(id)).get();
    }
    @AfterEach void close()throws Exception{storage.close();main.shutdown();assertTrue(main.awaitTermination(10,TimeUnit.SECONDS));}
    @Test void localUsesSameWorldAndDistanceWhileGlobalReachesAll()throws Exception{
        assertEquals(2,on(()->service.send(a,Channel.LOCAL,"Local",sink)));assertEquals(Set.of(a,b),new HashSet<>(delivered));delivered.clear();
        assertEquals(4,on(()->service.send(a,Channel.GLOBAL,"Global",sink)));assertEquals(Set.of(a,b,c,d),new HashSet<>(delivered));
    }
    @Test void clanNeverLeaksToNonMembersOrFallsBackToGlobal()throws Exception{
        on(()->service.send(a,Channel.CLAN,"Secret",sink));assertEquals(Set.of(a,b),new HashSet<>(delivered));
        assertThrows(ExecutionException.class,()->on(()->service.send(c,Channel.CLAN,"Denied",sink)));assertEquals(2,delivered.size());
    }
    @Test void staffRequiresPermissionOnBothSenderAndRecipient()throws Exception{
        assertThrows(ExecutionException.class,()->on(()->service.send(a,Channel.STAFF,"Denied",sink)));
        on(()->{nodes.put(a,Set.of("shirokaicore.chat.staff"));nodes.put(b,Set.of("shirokaicore.chat.staff"));return null;});
        on(()->service.send(a,Channel.STAFF,"Staff",sink));assertEquals(Set.of(a,b),new HashSet<>(delivered));delivered.clear();
        on(()->{nodes.remove(b);return null;});on(()->service.send(a,Channel.STAFF,"Next",sink));assertEquals(List.of(a),delivered);
    }
    @Test void selectedStaffChannelCannotLeakAfterPermissionLoss()throws Exception{
        on(()->{nodes.put(a,Set.of("shirokaicore.chat.staff"));return null;});on(()->service.select(a,Channel.STAFF)).get();
        on(()->{nodes.remove(a);return null;});assertThrows(ExecutionException.class,()->on(()->service.send(a,service.selected(a),"Private staff text",sink)));assertTrue(delivered.isEmpty());
    }
    @Test void ignoreAppliesToChannelsAndPrivateReplyAndCanBeRemoved()throws Exception{
        on(()->service.ignore(b,a)).get();on(()->service.send(a,Channel.GLOBAL,"Public",sink));assertFalse(delivered.contains(b));delivered.clear();
        assertThrows(ExecutionException.class,()->on(()->{service.privateMessage(a,b,"Hidden",sink);return null;}));assertTrue(delivered.isEmpty());
        on(()->service.ignore(b,a)).get();on(()->{service.privateMessage(a,b,"Hello",sink);return null;});assertEquals(List.of(b,a),delivered);assertEquals(a,on(()->service.replyTarget(b)));
        on(()->{service.leave(a);return null;});assertThrows(ExecutionException.class,()->on(()->service.replyTarget(b)));
    }
    @Test void muteBlocksAllCoreSendPathsAndUnmuteRestores()throws Exception{
        on(()->service.mute(null,a,0L,"Reason")).get();
        for(Channel channel:List.of(Channel.GLOBAL,Channel.LOCAL,Channel.CLAN))assertThrows(ExecutionException.class,()->on(()->service.send(a,channel,"Muted",sink)));
        assertThrows(ExecutionException.class,()->on(()->{service.privateMessage(a,b,"Muted",sink);return null;}));assertTrue(delivered.isEmpty());
        on(()->service.mute(null,a,null,"")).get();on(()->service.send(a,Channel.GLOBAL,"Restored",sink));assertEquals(4,delivered.size());
    }
    @Test void bypassRequiresExplicitNodeAndNeverBypassesLengthValidation()throws Exception{
        on(()->service.mute(null,a,0L,"Reason")).get();on(()->{nodes.put(a,Set.of("shirokaicore.chat.bypass.mute"));return null;});
        on(()->service.send(a,Channel.GLOBAL,"Allowed",sink));assertEquals(4,delivered.size());
        assertThrows(ExecutionException.class,()->on(()->service.send(a,Channel.GLOBAL,"x".repeat(257),sink)));
    }
    @Test void labelsUseOnlyTwoChosenIdentificationsAndBodyIsLiteral()throws Exception{
        String body="<click:run_command:'/op Alice'>hello</click>";on(()->service.send(a,Channel.GLOBAL,body,sink));
        var message=messages.getFirst();assertEquals(body,message.body());assertTrue(message.label().contains("[VIP] [CLAN] Alice"));assertFalse(message.label().contains("THIRD"));
    }
    @Test void unknownAndLoadingSessionsCannotSendOrReceive()throws Exception{
        on(()->{service.leave(b);return null;});on(()->service.send(a,Channel.GLOBAL,"Visible",sink));assertFalse(delivered.contains(b));
        assertThrows(ExecutionException.class,()->on(()->service.send(b,Channel.GLOBAL,"No profile",sink)));
        assertThrows(ExecutionException.class,()->on(()->{service.privateMessage(a,b,"Unavailable",sink);return null;}));
    }
    @Test void unrelatedModerationNeverSwitchesChannelAfterDefaultReload()throws Exception{
        var json=new Gson().toJsonTree(service.config()).getAsJsonObject();json.addProperty("defaultChannel","CLAN");var changed=ChatConfig.parse(json.toString());
        on(()->{service.replaceConfig(changed);return null;});
        on(()->service.ignore(a,b)).get();assertEquals(Channel.GLOBAL,on(()->service.selected(a)));
        on(()->service.mute(null,a,0L,"Reason")).get();assertEquals(Channel.GLOBAL,on(()->service.selected(a)));
        on(()->service.mute(null,a,null,"")).get();assertEquals(Channel.GLOBAL,on(()->service.selected(a)));
    }
    @Test void essentialsMuteBlocksEveryChannelAndPrivateBeforeAnyDelivery() throws Exception {
        on(()->{nodes.put(a,Set.of("shirokaicore.chat.staff"));
            service.bridge(id->{throw new IllegalStateException("Essentials mute");},ids->true,ids->true,id->{},(target,message)->{},(id,target)->false,(id,target)->{});return null;});
        for(var channel:List.of(Channel.GLOBAL,Channel.LOCAL,Channel.CLAN,Channel.STAFF))
            assertThrows(ExecutionException.class,()->on(()->service.send(a,channel,"blocked",sink)));
        assertThrows(ExecutionException.class,()->on(()->{service.privateMessage(a,b,"blocked",sink);return null;}));
        assertTrue(delivered.isEmpty());
    }
    @Test void essentialsPrivateToggleRejectsWithoutReplyOrSpyAndPublicIgnoreRestrictsRecipients() throws Exception {
        var observed=new ArrayList<UUID>();
        on(()->{service.bridge(id->{},ids->!ids.get(0).equals(b),ids->false,id->{},(target,message)->observed.add(target),(id,target)->false,(id,target)->{});return null;});
        assertThrows(ExecutionException.class,()->on(()->{service.privateMessage(a,b,"hidden",sink);return null;}));
        assertTrue(delivered.isEmpty());assertTrue(observed.isEmpty());
        assertThrows(ExecutionException.class,()->on(()->service.replyTarget(a)));
        on(()->service.send(a,Channel.GLOBAL,"visible",sink));assertFalse(delivered.contains(b));assertEquals(3,delivered.size());
    }
    @Test void sharedPrivateRouterPreservesBodyAndReplyIdentityAndUnignoreClearsLegacyState() throws Exception {
        boolean[] ignored={true};var observed=new ArrayList<UUID>();
        on(()->{service.bridge(id->{},ids->true,ids->true,id->{},(target,message)->observed.add(target),(id,target)->ignored[0],(id,target)->ignored[0]=false);return null;});
        var profile=on(()->service.ignore(b,a)).get();assertFalse(ignored[0]);assertFalse(profile.ignored().contains(a));
        on(()->{service.privateMessage(a,b,"<click:run_command:/op>literal",sink);return null;});
        assertEquals(List.of(b),observed);assertEquals(a,on(()->service.replyTarget(b)));
        assertTrue(messages.stream().allMatch(message->message.body().equals("<click:run_command:/op>literal")));
    }

}
