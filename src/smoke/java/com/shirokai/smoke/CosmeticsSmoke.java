package com.shirokai.smoke;

import com.mojang.authlib.GameProfile;
import com.shirokai.core.*;
import com.shirokai.core.cosmetics.*;
import com.shirokai.core.chat.*;
import com.shirokai.smoke.mixin.SmokePlayers;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.types.PermissionNode;
import net.minecraft.network.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Production Core/Essentials and LP, synthetic players, no authentication claim. */
public final class CosmeticsSmoke implements ModInitializer {
    private static final UUID ID=UUID.fromString("34c87c75-44e2-4c10-bf81-06528f5890ea");
    private record Wait(BooleanSupplier condition,long deadline,CompletableFuture<Void> future){}
    private final List<Wait> waits=new ArrayList<>();private final List<String> checks=new ArrayList<>();private long tick;private boolean ran;
    public void onInitialize(){ServerTickEvents.END_SERVER_TICK.register(server->{tick++;for(var wait:List.copyOf(waits))try{if(wait.condition.getAsBoolean()){waits.remove(wait);wait.future.complete(null);}else if(tick>wait.deadline){waits.remove(wait);wait.future.completeExceptionally(new AssertionError("Cosmetics timeout"));}}catch(Throwable e){waits.remove(wait);wait.future.completeExceptionally(e);}
        if(!ran&&server.getTicks()>60){ran=true;try{test(server).whenCompleteAsync((v,e)->finish(server,e),server);}catch(Throwable e){finish(server,e);}}
    });}
    private CompletableFuture<Void> until(BooleanSupplier condition){var future=new CompletableFuture<Void>();waits.add(new Wait(condition,tick+400,future));return future;}
    private void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks.add(label);}
    private boolean color(Text text,String token,Formatting color){var match=new boolean[1];text.visit((style,part)->{if(part.contains(token)&&style.getColor()!=null&&style.getColor().getRgb()==color.getColorValue())match[0]=true;return Optional.empty();},net.minecraft.text.Style.EMPTY);return match[0];}
    private ServerPlayerEntity player(MinecraftServer server,UUID id,String name,List<Packet<?>> packets){
        var p=new ServerPlayerEntity(server,server.getOverworld(),new GameProfile(id,name),SyncedClientOptions.createDefault());
        var connection=new ClientConnection(NetworkSide.SERVERBOUND){@Override public void send(Packet<?> packet){packets.add(packet);}@Override public void send(Packet<?> packet,PacketCallbacks callbacks){packets.add(packet);}@Override public void send(Packet<?> packet,PacketCallbacks callbacks,boolean flush){packets.add(packet);}@Override public void flush(){}@Override public boolean isOpen(){return true;}@Override public SocketAddress getAddress(){return new InetSocketAddress("127.0.0.1",0);}};
        p.networkHandler=new ServerPlayNetworkHandler(server,connection,p,ConnectedClientData.createDefault(p.getGameProfile(),false));p.setPosition(0,100,0);p.setOnGround(true);
        return p;
    }
    private CosmeticState state(CosmeticService service,ServerPlayerEntity player){try{return service.state(player);}catch(Exception e){throw new CompletionException(e);}}
    private CompletableFuture<Void> test(MinecraftServer server)throws Exception{
        var entry=FabricLoader.getInstance().getEntrypoints("main",ModInitializer.class).stream().filter(x->x instanceof ShirokaiCore).findFirst().orElseThrow();var field=ShirokaiCore.class.getDeclaredField("runtime");field.setAccessible(true);var core=(CoreRuntime)field.get(entry);var service=core.cosmetics();
        var packets=new ArrayList<Packet<?>>();var observerPackets=new ArrayList<Packet<?>>();var p=player(server,ID,"CosmeticAlice",packets);var observer=player(server,UUID.fromString("34c87c75-44e2-4c10-bf81-06528f5890eb"),"CosmeticBob",observerPackets);
        var access=(SmokePlayers)server.getPlayerManager();for(var person:List.of(p,observer)){access.shirokai$players().add(person);access.shirokai$playerMap().put(person.getUuid(),person);server.getOverworld().getPlayers().add(person);}
        var api=LuckPermsProvider.get();var loaded=api.getUserManager().loadUser(ID,"CosmeticAlice");
        return service.ready().thenCompose(v->loaded).thenComposeAsync(user->{
            check(service.status().equals("READY"),"Core cosmetic runtime becomes ready");
            user.data().clear();user.data().add(PermissionNode.builder("shirokai.essentials.cosmetics.*").build());user.getCachedData().invalidate();
            core.playerJoined(ID);core.playerJoined(observer.getUuid());
            return service.join(ID).thenComposeAsync(v->service.join(observer.getUuid()),server).thenComposeAsync(v->{
                check(state(service,p).cosmetics.get("nick").equals(Boolean.getBoolean("core.cosmeticsRestart")?"nick_cyan":"nick_gold"),"legacy or previously saved Core nick is loaded");
                check(!service.canUse(p)&&CosmeticService.color(p)==Formatting.WHITE,"legacy permission does not implicitly grant new Core nodes");
                user.data().add(PermissionNode.builder("shirokai.core.cosmetics.use").build());user.data().add(PermissionNode.builder("shirokai.core.cosmetics.customize").build());for(var option:CosmeticCatalog.ENTRIES)user.data().add(PermissionNode.builder("shirokai.core.cosmetics."+option.id()).build());user.getCachedData().invalidate();
                check(service.canUse(p)&&service.canCustomize(p),"new Core LuckPerms nodes activate menu and personalization");
                check(state(service,p).cosmeticPreferences.speed==3&&state(service,p).cosmeticPreferences.direction==-1,"legacy particle personalization is preserved");
                if(FabricLoader.getInstance().isModLoaded("shirokai_essentials"))try{
                    var bridge=Class.forName("dev.shirokai.essentials.api.EssentialsBridge");check((boolean)bridge.getMethod("ownsCosmetics").invoke(null),"Essentials yields effects to Core through runtime handoff");
                    var old=Class.forName("dev.shirokai.essentials.ShirokaiEssentials").getMethod("instance").invoke(null);var legacy=old.getClass().getField("cosmetics").get(old);var store=old.getClass().getMethod("data").invoke(old);store.getClass().getMethod("player",UUID.class).invoke(store,ID);store.getClass().getMethod("player",UUID.class).invoke(store,observer.getUuid());check(!(boolean)legacy.getClass().getMethod("canUse",ServerPlayerEntity.class).invoke(legacy,p),"legacy cosmetics remain suppressed even with legacy wildcard permission");
                }catch(ReflectiveOperationException e){throw new CompletionException(e);}
                var commands=server.getCommandManager().getDispatcher();try{check(commands.execute("cosmeticos",p.getCommandSource())==1&&p.currentScreenHandler instanceof CosmeticScreen,"Core owns the original cosmetics menu command");}catch(Exception e){throw new CompletionException(e);}
                check(p.getInventory().isEmpty(),"opening cosmetics menu grants no items");
                long before=service.revision(p);service.equip(p,CosmeticCatalog.find("nick_cyan"));
                return until(()->service.revision(p)>before&&state(service,p).cosmetics.get("nick").equals("nick_cyan"));
            },server).thenComposeAsync(v->{
                check(CosmeticService.color(p)==Formatting.AQUA,"saved Core selection changes nickname color");
                check(color(CosmeticService.name(p),"CosmeticAlice",Formatting.AQUA),"nickname component carries cosmetic color without changing account name");
                core.cosmeticsChanged(ID);return until(()->server.getScoreboard().getScoreHolderTeam(p.getGameProfile().getName())!=null);
            },server).thenComposeAsync(v->{
                var team=server.getScoreboard().getScoreHolderTeam(p.getGameProfile().getName());check(team.getColor()==Formatting.AQUA,"Core-owned nametag team uses cosmetic color");check(!team.getPrefix().getString().isEmpty(),"rank prefix remains present on the nametag");
                var tab=observerPackets.stream().filter(PlayerListS2CPacket.class::isInstance).map(PlayerListS2CPacket.class::cast).flatMap(x->x.getEntries().stream()).filter(x->x.profileId().equals(ID)&&x.displayName()!=null).map(PlayerListS2CPacket.Entry::displayName).toList();
                check(tab.stream().anyMatch(x->color(x,"CosmeticAlice",Formatting.AQUA)),"actual TAB display packets contain colored nickname");
                var envelope=new ChatService.Envelope(ID,"","hello",Channel.GLOBAL);check(color(core.chat().service().header(envelope),"CosmeticAlice",Formatting.AQUA),"fallback chat header respects nickname color");
                try{var presentationField=CoreRuntime.class.getDeclaredField("presentation");presentationField.setAccessible(true);var presentation=(com.shirokai.core.presentation.PresentationRuntime)presentationField.get(core);check(color(presentation.header(envelope),"CosmeticAlice",Formatting.AQUA),"rich chat header respects nickname color");}catch(ReflectiveOperationException e){throw new CompletionException(e);}
                observerPackets.clear();var incoming=net.minecraft.network.message.SignedMessage.ofUnsigned(ID,"Cosmetics smoke message");core.chat().signed(p,incoming);
                var sent=observerPackets.stream().filter(ChatMessageS2CPacket.class::isInstance).map(ChatMessageS2CPacket.class::cast).findFirst().orElseThrow();
                check(color(sent.serializedParameters().name(),"CosmeticAlice",Formatting.AQUA),"actual chat packet preserves cosmetic nickname color");
                check(sent.body().content().equals(incoming.getSignedContent())&&Objects.equals(sent.signature(),incoming.signature())&&sent.unsignedContent()==null,"name color leaves message body, signature field and unsigned content untouched");
                observerPackets.clear();for(int i=0;i<6;i++)service.tick(server);check(observerPackets.stream().anyMatch(x->x instanceof ParticleS2CPacket),"Core emits actual aura particles using imported selection");
                if(FabricLoader.getInstance().isModLoaded("shirokai_raids"))CosmeticRaidSmoke.verify(p,service,observerPackets,(condition,label)->check(condition,label));
                service.tick(server);check(service.celebrate(p,"victory"),"Core victory category produces a bounded celebration");check(!service.celebrate(p,"victory"),"celebration cooldown prevents repeated burst");
                user.data().add(PermissionNode.builder("shirokai.core.cosmetics.nick_cyan").value(false).build());user.getCachedData().invalidate();
                check(CosmeticService.color(p)==Formatting.WHITE&&state(service,p).cosmetics.get("nick").equals("nick_cyan"),"permission revocation suspends color without deleting choice");
                user.data().remove(PermissionNode.builder("shirokai.core.cosmetics.nick_cyan").value(false).build());user.data().add(PermissionNode.builder("shirokai.core.cosmetics.nick_cyan").build());user.getCachedData().invalidate();
                check(CosmeticService.color(p)==Formatting.AQUA,"grant restoration reactivates saved color without reselecting");
                long revision=service.revision(p);service.disable(p);return until(()->service.revision(p)>revision&&state(service,p).cosmeticsHidden);
            },server).thenComposeAsync(v->{
                check(CosmeticService.color(p)==Formatting.WHITE,"hide-all disables cosmetic name color");observerPackets.clear();for(int i=0;i<6;i++)service.tick(server);check(observerPackets.stream().noneMatch(x->x instanceof ParticleS2CPacket),"hide-all stops Core particles");long revision=service.revision(p);service.resume(p);return until(()->service.revision(p)>revision&&!state(service,p).cosmeticsHidden);
            },server).thenComposeAsync(v->{
                check(p.getUuid().equals(ID)&&p.getGameProfile().getName().equals("CosmeticAlice"),"cosmetic changes preserve UUID and real account name");check(p.getInventory().isEmpty(),"cosmetics leave inventory intact");
                long previous=service.revision(p);service.equip(p,CosmeticCatalog.find("nick_gold"));
                var replacement=player(server,ID,"CosmeticAlice",packets);var manager=(SmokePlayers)server.getPlayerManager();manager.shirokai$players().remove(p);manager.shirokai$players().add(replacement);manager.shirokai$playerMap().put(ID,replacement);server.getOverworld().getPlayers().remove(p);server.getOverworld().getPlayers().add(replacement);
                return until(()->service.revision(replacement)>previous&&state(service,replacement).cosmetics.get("nick").equals("nick_gold"))
                    .thenComposeAsync(done->{check(CosmeticService.color(replacement)==Formatting.GOLD,"in-flight save publishes to replacement player after synthetic respawn");long revision=service.revision(replacement);service.equip(replacement,CosmeticCatalog.find("nick_cyan"));return until(()->service.revision(replacement)>revision&&state(service,replacement).cosmetics.get("nick").equals("nick_cyan"));},server);
            },server);
        },server);
    }
    private void finish(MinecraftServer server,Throwable error){if(error!=null){while(error.getCause()!=null)error=error.getCause();error.printStackTrace();}var result=Map.of("passed",error==null,"checks",List.copyOf(checks),"error",error==null?"":error.toString(),"realClients",0);CompletableFuture.runAsync(()->{try{java.nio.file.Files.writeString(java.nio.file.Path.of("cosmetics-results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(Exception e){throw new CompletionException(e);}}).whenCompleteAsync((v,e)->server.stop(false),server);}
}
