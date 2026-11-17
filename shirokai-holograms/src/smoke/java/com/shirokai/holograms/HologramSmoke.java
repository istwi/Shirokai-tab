package com.shirokai.holograms;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.*;
import net.minecraft.network.packet.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.*;
import net.minecraft.text.*;
import net.minecraft.util.math.Vec3d;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static net.minecraft.server.command.CommandManager.literal;

/** Real Fabric registries/commands and native display packets; viewers are synthetic, not authenticated clients. */
public final class HologramSmoke implements ModInitializer {
    private record Wait(BooleanSupplier condition,long deadline,CompletableFuture<Void> result){}
    private final List<Wait> waits=new ArrayList<>();private long tick;private boolean running;
    private final List<String> checks=new ArrayList<>();
    public void onInitialize(){
        ServerTickEvents.END_SERVER_TICK.register(server->{tick++;for(var wait:List.copyOf(waits))try{if(wait.condition().getAsBoolean()){wait.result().complete(null);waits.remove(wait);}else if(tick>wait.deadline()){wait.result().completeExceptionally(new IllegalStateException("Smoke timeout"));waits.remove(wait);}}catch(Throwable e){wait.result().completeExceptionally(e);waits.remove(wait);}});
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(literal("shirokai-holograms-smoke").requires(s->s.hasPermissionLevel(4)).executes(c->{
            if(running)return 0;running=true;
            run(c.getSource().getServer()).whenComplete((v,error)->{running=false;if(error!=null){error.printStackTrace();c.getSource().sendError(Text.literal("HOLOGRAM_SMOKE_FAIL: "+error));}else c.getSource().sendFeedback(()->Text.literal("HOLOGRAM_SMOKE_PASS checks="+checks.size()),false);});return 1;
        })));
    }
    private CompletableFuture<Void> until(BooleanSupplier condition){var f=new CompletableFuture<Void>();waits.add(new Wait(condition,tick+200,f));return f;}
    private CompletableFuture<Void> run(MinecraftServer server){
        checks.clear();final ConfigStore store;final HologramService service;
        try{store=new ConfigStore(Files.createTempDirectory(server.getRunDirectory().resolve("."),"hologram-fixture-"));service=new HologramService(server,store,org.slf4j.LoggerFactory.getLogger("hologram-smoke"));}
        catch(Exception e){return CompletableFuture.failedFuture(e);}
        var aPackets=new ArrayList<Packet<?>>();var bPackets=new ArrayList<Packet<?>>();
        var alice=player(server,"Alice",aPackets);var bob=player(server,"Bob",bPackets);alice.setPosition(0,64,0);bob.setPosition(0,64,0);
        var dispatcher=new CommandDispatcher<ServerCommandSource>();new Commands(()->service).register(dispatcher);
        var op=alice.getCommandSource().withLevel(4);
        return service.reload().thenCompose(v->{
            try{
                check(!Permissions.allowed(alice.getCommandSource().withLevel(0),"create"),"ordinary player denied");
                check(Permissions.allowed(server.getCommandSource(),"reload"),"console permission");
                try{dispatcher.execute("hologram create denied",alice.getCommandSource().withLevel(0));throw new AssertionError("permission bypass");}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){checks.add("command tree denies ordinary player");}
                dispatcher.execute("hologram create spawn",op);
            }catch(Exception e){return CompletableFuture.failedFuture(e);}
            return until(()->service.plan().entries().containsKey("spawn"));
        }).thenCompose(v->command(dispatcher,op,"hologram line set spawn 1 <gold><bold>%player:name%</bold></gold>",()->service.get("spawn").lines().getFirst().text().contains("player:name")))
        .thenCompose(v->command(dispatcher,op,"hologram line add spawn <aqua>Online: %server:online%</aqua>",()->service.get("spawn").lines().size()==2))
        .thenCompose(v->command(dispatcher,op,"hologram align spawn left",()->service.get("spawn").alignment()==Definitions.Alignment.LEFT))
        .thenCompose(v->command(dispatcher,op,"hologram line align spawn 2 right",()->service.get("spawn").lines().get(1).alignment()==Definitions.Alignment.RIGHT))
        .thenCompose(v->command(dispatcher,op,"hologram scale spawn 1.5",()->service.get("spawn").scale()==1.5f))
        .thenCompose(v->{
            service.update(alice);service.update(bob);
            var a=metadata(server,aPackets);var b=metadata(server,bPackets);
            check(a.size()==2&&b.size()==2,"one text display per line per viewer");
            check(aPackets.stream().filter(p->p instanceof EntitySpawnS2CPacket).map(p->((EntitySpawnS2CPacket)p).getEntityId()).allMatch(id->server.getOverworld().getEntityById(id)==null),"virtual displays never enter ServerWorld persistence");
            check(a.stream().anyMatch(n->n.getString("text").contains("Alice"))&&b.stream().anyMatch(n->n.getString("text").contains("Bob")),"personalized placeholders");
            check(a.stream().anyMatch(n->n.getString("text").contains("bold")),"native formatted components");
            check(a.getFirst().getInt("background")==0&&a.getFirst().getString("billboard").equals("center")&&a.getFirst().getBoolean("shadow"),"transparent shadow billboard properties");
            int count=aPackets.size();service.update(alice);check(count==aPackets.size(),"static unchanged text emits zero packets");
            alice.setPosition(10000,64,0);service.update(alice);check(aPackets.stream().filter(p->p instanceof EntitiesDestroyS2CPacket).count()==2,"distance culling removes only owned displays");
            alice.setPosition(0,64,0);service.update(alice);
            check(aPackets.stream().filter(p->p instanceof EntitySpawnS2CPacket).count()==4,"reentry recreates private displays");
            // Compare actual tracked transforms; no padded text is used for alignment.
            var left=a.getFirst().getCompound("transformation").getList("translation",5).getFloat(0);
            var right=a.get(1).getCompound("transformation").getList("translation",5).getFloat(0);
            check(left<=0&&right>=0,"per-line pixel alignment transforms");
            return service.reload();
        }).thenCompose(v->{
            var previous=service.plan();var file=store.directory().resolve("holograms.json");var backup=store.directory().resolve("holograms.backup");
            try{Files.move(file,backup);Files.createDirectory(file);}catch(Exception e){throw new CompletionException(e);}
            return service.change(entries->{entries.removeIf(h->h.id().equals("nether"));return entries;}).handle((ok,error)->{
                check(error!=null&&service.plan()==previous,"failed atomic save preserves active plan");
                try{Files.delete(file);Files.move(backup,file);}catch(Exception e){throw new CompletionException(e);}return null;
            });
        }).thenCompose(v->{
            check(service.get("spawn").lines().size()==2&&service.get("spawn").scale()==1.5f,"reload restores persisted commands");
            var previous=service.plan();
            try{Files.writeString(store.directory().resolve("holograms.json"),"{broken");}catch(Exception e){throw new CompletionException(e);}
            return service.reload().handle((ok,error)->{check(error!=null&&service.plan()==previous,"invalid reload preserves active plan");return null;});
        }).thenCompose(v->{
            try{store.save(service.plan().snapshot().holograms());}catch(Exception e){throw new CompletionException(e);}
            return command(dispatcher,op,"hologram line animation spawn 1 TYPEWRITER",()->service.get("spawn").lines().getFirst().animation().equals("TYPEWRITER"));
        }).thenCompose(v->{
            // Full metadata reset to defaults must be sent, including opacity and interpolation start=0.
            var display=new DisplayLine(alice,service.get("spawn"));aPackets.clear();
            display.update(Text.literal("fade"),0,0,64,2,false);display.update(Text.literal("fade"),0,0,255,2,false);
            var client=new TextDisplayEntity(EntityType.TEXT_DISPLAY,server.getOverworld());
            for(var packet:aPackets)if(packet instanceof EntityTrackerUpdateS2CPacket update)client.getDataTracker().writeUpdatedEntries(update.trackedValues());
            var nbt=client.writeNbt(new NbtCompound());check(Byte.toUnsignedInt(nbt.getByte("text_opacity"))==255,"opacity returns to native default");display.destroy();
            for(var type:Definitions.Animation.values()) {
                var motion=Definitions.Motion.defaults(type);var probe=new DisplayLine(alice,service.get("spawn"));aPackets.clear();
                for(long t:new long[]{0,20,40,80}){var frame=AnimationFrame.at(motion,t,36);probe.update(frame.text(Text.literal("native")),frame.x(),frame.y(),frame.opacity(),2,false);}
                var received=metadata(server,aPackets);check(!received.isEmpty(),"native metadata codec: "+type);probe.destroy();
            }
            return command(dispatcher,op,"hologram line remove spawn 2",()->service.get("spawn").lines().size()==1);
        }).thenCompose(v->command(dispatcher,op,"hologram move spawn",()->service.get("spawn").x()==alice.getX()))
        .thenCompose(v->{
            try{dispatcher.execute("hologram edit spawn",op);dispatcher.execute("hologram list",op);dispatcher.execute("hologram teleport spawn",op);checks.add("edit/list/teleport commands");}
            catch(Exception e){throw new CompletionException(e);}
            return command(dispatcher,op,"hologram delete spawn",()->!service.plan().entries().containsKey("spawn"));
        }).thenRun(()->{
            service.update(bob);service.forget(bob.getUuid());checks.add("disconnect cleanup");
            try{Files.writeString(server.getRunDirectory().resolve("hologram-smoke-result.json"),ConfigStore.JSON.toJson(checks));}catch(Exception e){throw new CompletionException(e);}
        }).whenComplete((v,error)->service.close());
    }
    private CompletableFuture<Void> command(CommandDispatcher<ServerCommandSource> dispatcher,ServerCommandSource source,String command,BooleanSupplier done){
        try{dispatcher.execute(command,source);checks.add(command);return until(done);}catch(Exception e){return CompletableFuture.failedFuture(e);}
    }
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);checks.add(message);}
    private static List<NbtCompound> metadata(MinecraftServer server,List<Packet<?>> packets){
        var result=new ArrayList<NbtCompound>();
        for(var packet:packets)if(packet instanceof EntityTrackerUpdateS2CPacket update){
            var buffer=new RegistryByteBuf(io.netty.buffer.Unpooled.buffer(),server.getRegistryManager());
            EntityTrackerUpdateS2CPacket.CODEC.encode(buffer,update);var decoded=EntityTrackerUpdateS2CPacket.CODEC.decode(buffer);buffer.release();
            var entity=new TextDisplayEntity(EntityType.TEXT_DISPLAY,server.getOverworld());entity.getDataTracker().writeUpdatedEntries(decoded.trackedValues());result.add(entity.writeNbt(new NbtCompound()));
        }return result;
    }
    private static ServerPlayerEntity player(MinecraftServer server,String name,List<Packet<?>> capture){
        var profile=new GameProfile(UUID.randomUUID(),name);var player=new ServerPlayerEntity(server,server.getOverworld(),profile,SyncedClientOptions.createDefault());
        var connection=new ClientConnection(NetworkSide.SERVERBOUND){
            public void send(Packet<?> p){capture.add(p);}public void send(Packet<?> p,PacketCallbacks c){capture.add(p);}public void send(Packet<?> p,PacketCallbacks c,boolean f){capture.add(p);}
            public boolean isOpen(){return true;}public SocketAddress getAddress(){return new InetSocketAddress("127.0.0.1",0);}public void flush(){}
        };
        player.networkHandler=new ServerPlayNetworkHandler(server,connection,player,ConnectedClientData.createDefault(profile,false));return player;
    }
}
