package dev.notzyvex.coasters_extras.net;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BrakingTracker {

    private static final List<double[]> pending = new ArrayList<>();
    private static Set<String> lastSent = new HashSet<>();

    public static void report(double x, double y, double z) {
        synchronized (pending) {
            pending.add(new double[]{ x, y, z });
        }
    }

    public static void flush(ServerLevel level) {
        List<double[]> snapshot;
        synchronized (pending) {
            snapshot = new ArrayList<>(pending);
            pending.clear();
        }

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
