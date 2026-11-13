package com.shirokai.smoke;

import com.shirokai.core.motd.*;
import com.shirokai.core.styles.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.network.ServerQueryNetworkHandler;
import net.minecraft.text.Text;
import java.util.*;
import static net.minecraft.server.command.CommandManager.literal;

public final class MotdSmoke implements ModInitializer {
    public void onInitialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(
        literal("shirokai-motd-smoke").requires(s->s.hasPermissionLevel(4)).executes(c->{
            var original=new ServerMetadata(Text.literal("Vanilla"),Optional.empty(),Optional.empty(),Optional.empty(),true);
            var previous=MotdBridge.decorate(original);
            try{
                var packets=new ArrayList<Packet<?>>();
                var connection=new ClientConnection(NetworkSide.SERVERBOUND){@Override public void send(Packet<?> packet){packets.add(packet);}};
                var plan=new MotdPlan(MotdConfig.defaults(),new StyleEngine(StyleConfig.defaults()));
                MotdBridge.publish(plan.render(0,3,20,"1.21.1"));
                new ServerQueryNetworkHandler(original,connection).onRequest(QueryRequestC2SPacket.INSTANCE);
                var response=((QueryResponseS2CPacket)packets.getFirst()).metadata();
                check(response.description().getString().contains("3/20 online"),"Transformed query did not return live MOTD");
                check(response.secureChatEnforced()==original.secureChatEnforced()&&response.players().equals(original.players())&&response.version().equals(original.version())&&response.favicon().equals(original.favicon()),"MOTD modified other metadata");
                check(original.description().getString().equals("Vanilla"),"Shared vanilla metadata mutated");
                MotdBridge.publish(plan.render(60,4,20,"1.21.1"));packets.clear();
                new ServerQueryNetworkHandler(original,connection).onRequest(QueryRequestC2SPacket.INSTANCE);
                check(((QueryResponseS2CPacket)packets.getFirst()).metadata().description().getString().contains("próximo Pokémon"),"MOTD frame did not advance");
                MotdBridge.publish(null);packets.clear();
                new ServerQueryNetworkHandler(original,connection).onRequest(QueryRequestC2SPacket.INSTANCE);
                check(((QueryResponseS2CPacket)packets.getFirst()).metadata()==original,"Disabled MOTD replaced vanilla metadata");
                c.getSource().sendFeedback(()->Text.literal("MOTD_SMOKE_PASS"),false);
            }finally{MotdBridge.publish(previous==original?null:previous.description());}
            return 1;
        })));
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(literal("shirokai-motd-inspect").requires(source->source.hasPermissionLevel(4)).executes(context->{
            var original=new ServerMetadata(Text.literal("Vanilla"),Optional.empty(),Optional.empty(),Optional.empty(),true);
            context.getSource().sendFeedback(()->Text.literal("MOTD_INSPECT: "+MotdBridge.decorate(original).description().getString()),false);return 1;
        })));
    }
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
