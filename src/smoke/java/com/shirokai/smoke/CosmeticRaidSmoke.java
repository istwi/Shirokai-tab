package com.shirokai.smoke;

import com.shirokai.core.cosmetics.CosmeticService;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

/** Optional branch invoked only with real Cobblemon and Raids installed. */
final class CosmeticRaidSmoke {
    @SuppressWarnings("unchecked")
    static void verify(ServerPlayerEntity player,CosmeticService service,List<Packet<?>> packets,BiConsumer<Boolean,String> check){
        try{
            var boss=com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse("mew level=5").createEntity(player.getServerWorld());boss.setPosition(player.getPos());
            var type=Class.forName("com.shirokai.raids.RaidVictoryTransition");var chunks=Class.forName("com.shirokai.raids.RaidChunkLease");
            var ctor=type.getDeclaredConstructor(com.cobblemon.mod.common.entity.pokemon.PokemonEntity.class,chunks,net.minecraft.entity.boss.ServerBossBar.class);ctor.setAccessible(true);
            var bar=new net.minecraft.entity.boss.ServerBossBar(net.minecraft.text.Text.literal("Raid smoke"),net.minecraft.entity.boss.BossBar.Color.WHITE,net.minecraft.entity.boss.BossBar.Style.PROGRESS);
            var transition=ctor.newInstance(boss,null,bar);var begin=type.getDeclaredMethod("begin",String.class,Collection.class);begin.setAccessible(true);
            var cooldown=CosmeticService.class.getDeclaredField("lastCelebration");cooldown.setAccessible(true);((Map<?,?>)cooldown.get(service)).clear();service.tick(player.getServer());packets.clear();
            begin.invoke(transition,"Smoke",List.of(player));
            long singles=packets.stream().filter(ParticleS2CPacket.class::isInstance).map(ParticleS2CPacket.class::cast).filter(p->p.getCount()==1).count();
            check.accept(singles==36,"real transformed Raids victory callback emits exactly one Core celebration");
            ((Map<?,?>)cooldown.get(service)).clear();bar.clearPlayers();boss.discard();
        }catch(ReflectiveOperationException e){throw new java.util.concurrent.CompletionException(e);}
    }
}
