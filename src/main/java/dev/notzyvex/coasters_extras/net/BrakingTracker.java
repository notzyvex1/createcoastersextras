package dev.notzyvex.coasters_extras.net;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Server-side record of which carts are braking hard enough to glow.
 *
 * <p>The drive hook already knows this every physics tick; this just collects it and pushes
 * it to clients when it changes. Sending every tick would be a packet per tick per player
 * for something that changes rarely, so the set is compared against the last one sent.
 */
public final class BrakingTracker {

    /** Positions reported this tick, before they are published. */
    private static final List<double[]> pending = new ArrayList<>();
    private static Set<String> lastSent = new HashSet<>();

    /** Called from the drive hook while a cart is being braked. */
    public static void report(double x, double y, double z) {
        synchronized (pending) {
            pending.add(new double[]{ x, y, z });
        }
    }

    /** Called once per server tick. Publishes only when the set actually changed. */
    public static void flush(ServerLevel level) {
        List<double[]> snapshot;
        synchronized (pending) {
            snapshot = new ArrayList<>(pending);
            pending.clear();
        }

        // Round to a whole block for the comparison. Without this a cart creeping along a
        // brake would differ every tick and republish constantly.
        Set<String> key = new HashSet<>();
        for (double[] p : snapshot) {
            key.add((int) Math.floor(p[0]) + ":" + (int) Math.floor(p[1]) + ":" + (int) Math.floor(p[2]));
        }
        if (key.equals(lastSent)) {
            return;
        }
        lastSent = key;

        BrakingPayload payload = new BrakingPayload(snapshot);
        for (ServerPlayer p : level.players()) {
            PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private BrakingTracker() {}
}
