package dev.notzyvex.coasters_extras.net;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StationTracker {

    private static final int RESYNC_TICKS = 4;

    private static final Map<String, StationPayload.Entry> pending = new LinkedHashMap<>();
    private static String lastKey = "";
    private static long lastSent = Long.MIN_VALUE;

    private static final Map<String, StationPayload.Entry> latest = new LinkedHashMap<>();
    private static final Map<String, Long> latestAt = new LinkedHashMap<>();

    private static final int STALE_TICKS = 20;

    private static String key(BlockPos a, BlockPos b) {
        long lo = Math.min(a.asLong(), b.asLong());
        long hi = Math.max(a.asLong(), b.asLong());
        return lo + "/" + hi;
    }

    public static StationPayload.Entry read(BlockPos a, BlockPos b, long now) {
        if (a == null || b == null) {
            return null;
        }
        String k = key(a, b);
        synchronized (pending) {
            Long at = latestAt.get(k);
            if (at == null || now - at > STALE_TICKS) {
                return null;
            }
            return latest.get(k);
        }
    }

    public static void report(BlockPos a, BlockPos b, byte state, int ticksLeft, int dwell,
                              long now) {
        if (a == null || b == null) return;
        String key = key(a, b);
        StationPayload.Entry entry = new StationPayload.Entry(a, b, state, ticksLeft, dwell);
        synchronized (pending) {
            pending.put(key, entry);
            latest.put(key, entry);
            latestAt.put(key, now);
        }
    }

    public static void flush(ServerLevel level) {
        List<StationPayload.Entry> snapshot;
        synchronized (pending) {
            snapshot = new ArrayList<>(pending.values());
            pending.clear();
        }

        StringBuilder sb = new StringBuilder();
        for (StationPayload.Entry e : snapshot) {
            sb.append(e.a().asLong()).append(':').append(e.b().asLong())
              .append(':').append(e.state()).append(';');
        }
        String key = sb.toString();

        long now = level.getGameTime();
        boolean changed = !key.equals(lastKey);
        boolean due = now - lastSent >= RESYNC_TICKS && !snapshot.isEmpty();
        if (!changed && !due) {
            return;
        }
        lastKey = key;
        lastSent = now;

        StationPayload payload = new StationPayload(snapshot);
        for (ServerPlayer p : level.players()) {
            PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private StationTracker() {}
}
