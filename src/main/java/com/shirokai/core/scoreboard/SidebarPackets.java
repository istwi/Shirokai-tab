package com.shirokai.core.scoreboard;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import java.util.*;

/** Observes outbound state, including packets sent before JOIN. No game API or IO here.
 * Weak connection keys; all access synchronized because other mods may send off-thread. */
public final class SidebarPackets {
    public record State(Set<String> names, Map<ScoreboardDisplaySlot, String> slots, boolean overflow, long displayRevision, long resetRevision,long belowRevision) { }
    private static final class Ledger {
        final Set<String> names = new HashSet<>();
        final Map<ScoreboardDisplaySlot, String> slots = new EnumMap<>(ScoreboardDisplaySlot.class);
        int users;
        boolean overflow;
        long displayRevision, resetRevision,belowRevision;
    }
    private static final Map<ServerCommonNetworkHandler, Ledger> LEDGERS = new WeakHashMap<>();
    private static final ThreadLocal<Packet<?>> OWN = new ThreadLocal<>();
    private SidebarPackets() { }
    public static synchronized void observe(ServerCommonNetworkHandler handler, Packet<?> packet) {
        if (packet == OWN.get()) return;
        if (packet instanceof BundleS2CPacket bundle) { for (var child : bundle.getPackets()) observe(handler, child); return; }
        if (!(packet instanceof ScoreboardObjectiveUpdateS2CPacket || packet instanceof ScoreboardDisplayS2CPacket || packet instanceof ScoreboardScoreUpdateS2CPacket || packet instanceof ScoreboardScoreResetS2CPacket)) return;
        var ledger = LEDGERS.computeIfAbsent(handler, ignored -> new Ledger());
        if (packet instanceof ScoreboardObjectiveUpdateS2CPacket objective) {
            if (objective.getMode() == ScoreboardObjectiveUpdateS2CPacket.REMOVE_MODE) {
                ledger.names.add(objective.getName()); ledger.slots.values().removeIf(objective.getName()::equals);
            } else ledger.names.add(objective.getName());
        } else if (packet instanceof ScoreboardDisplayS2CPacket display) {
            if(display.getSlot()==ScoreboardDisplaySlot.BELOW_NAME)ledger.belowRevision++;
            if (display.getSlot() != ScoreboardDisplaySlot.LIST && display.getSlot() != ScoreboardDisplaySlot.BELOW_NAME) ledger.displayRevision++;
            if (display.getName() == null || display.getName().isEmpty()) ledger.slots.remove(display.getSlot());
            else { ledger.slots.put(display.getSlot(), display.getName()); ledger.names.add(display.getName()); }
        } else if (packet instanceof ScoreboardScoreUpdateS2CPacket score) ledger.names.add(score.objectiveName());
        else if (packet instanceof ScoreboardScoreResetS2CPacket reset) { ledger.resetRevision++; if (reset.objectiveName() != null) ledger.names.add(reset.objectiveName()); }
        if (ledger.names.size() > 4096) { ledger.overflow = true; ledger.names.clear(); }
    }
    public static synchronized State state(ServerCommonNetworkHandler handler) {
        var ledger = LEDGERS.get(handler);
        return ledger == null ? new State(Set.of(), Map.of(), false, 0, 0, 0) : new State(Set.copyOf(ledger.names), Map.copyOf(ledger.slots), ledger.overflow, ledger.displayRevision, ledger.resetRevision,ledger.belowRevision);
    }
    public static synchronized void retain(ServerCommonNetworkHandler handler){LEDGERS.computeIfAbsent(handler,k->new Ledger()).users++;}
    public static synchronized void release(ServerCommonNetworkHandler handler){var ledger=LEDGERS.get(handler);if(ledger!=null&&--ledger.users<=0)LEDGERS.remove(handler);}
    public static synchronized void forget(ServerCommonNetworkHandler handler) { LEDGERS.remove(handler); }
    public static void send(ServerCommonNetworkHandler handler, Packet<?> packet) {
        var previous = OWN.get(); OWN.set(packet);
        try { handler.sendPacket(packet); } finally { if (previous == null) OWN.remove(); else OWN.set(previous); }
    }
}
