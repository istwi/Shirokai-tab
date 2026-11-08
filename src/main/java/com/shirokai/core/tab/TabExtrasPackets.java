package com.shirokai.core.tab;

import com.shirokai.core.scoreboard.SidebarPackets;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import java.util.*;

/** Connection-local observations only: no game APIs or IO in this synchronized hook. */
public final class TabExtrasPackets {
    public record Listed(boolean value,boolean removed,long revision){}
    public record State(Map<UUID,Listed> listed,Set<UUID> bossIds,boolean overflow){}
    private static final class Ledger {
        final Map<UUID,Listed> listed=new HashMap<>();final Set<UUID> bossIds=new HashSet<>();boolean overflow;long revision;
    }
    private static final Map<ServerCommonNetworkHandler,Ledger> LEDGERS=new WeakHashMap<>();
    private static final ThreadLocal<Packet<?>> OWN=new ThreadLocal<>();
    private TabExtrasPackets(){}
    public static synchronized void observe(ServerCommonNetworkHandler handler,Packet<?> packet){
        if(packet==OWN.get())return;
        if(packet instanceof BundleS2CPacket bundle){for(var child:bundle.getPackets())observe(handler,child);return;}
        if(!(packet instanceof PlayerListS2CPacket||packet instanceof PlayerRemoveS2CPacket||packet instanceof BossBarS2CPacket))return;
        var ledger=LEDGERS.computeIfAbsent(handler,k->new Ledger());
        if(ledger.overflow)return;
        if(packet instanceof PlayerListS2CPacket list){
            if(list.getActions().contains(PlayerListS2CPacket.Action.UPDATE_LISTED)||list.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER))
                for(var entry:list.getEntries())ledger.listed.put(entry.profileId(),new Listed(list.getActions().contains(PlayerListS2CPacket.Action.UPDATE_LISTED)&&entry.listed(),false,++ledger.revision));
        }else if(packet instanceof PlayerRemoveS2CPacket remove){
            for(UUID id:remove.profileIds())ledger.listed.put(id,new Listed(false,true,++ledger.revision));
        }else if(packet instanceof BossBarS2CPacket boss)boss.accept(new BossBarS2CPacket.Consumer(){
            private void seen(UUID id){ledger.bossIds.add(id);}
            public void add(UUID id,net.minecraft.text.Text text,float percent,net.minecraft.entity.boss.BossBar.Color color,net.minecraft.entity.boss.BossBar.Style style,boolean sky,boolean music,boolean fog){seen(id);}
            public void remove(UUID id){seen(id);}public void updateProgress(UUID id,float progress){seen(id);}
            public void updateName(UUID id,net.minecraft.text.Text text){seen(id);}
            public void updateStyle(UUID id,net.minecraft.entity.boss.BossBar.Color color,net.minecraft.entity.boss.BossBar.Style style){seen(id);}
            public void updateProperties(UUID id,boolean sky,boolean music,boolean fog){seen(id);}
        });
        if(ledger.listed.size()>8192||ledger.bossIds.size()>4096){ledger.overflow=true;ledger.listed.clear();ledger.bossIds.clear();}
    }
    public static synchronized State state(ServerCommonNetworkHandler handler){
        var ledger=LEDGERS.get(handler);return ledger==null?new State(Map.of(),Set.of(),false):new State(Map.copyOf(ledger.listed),Set.copyOf(ledger.bossIds),ledger.overflow);
    }
    public static synchronized void forget(ServerCommonNetworkHandler handler){LEDGERS.remove(handler);}
    public static void send(ServerCommonNetworkHandler handler,Packet<?> packet){
        var previous=OWN.get();OWN.set(packet);
        try{SidebarPackets.send(handler,packet);}finally{if(previous==null)OWN.remove();else OWN.set(previous);}
    }
    public static PlayerListS2CPacket listed(List<PlayerListS2CPacket.Entry> entries){
        var packet=new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.Action.UPDATE_LISTED),List.of());
        ((com.shirokai.core.mixin.PlayerListPacketAccessor)packet).shirokai$setEntries(List.copyOf(entries));return packet;
    }
}
