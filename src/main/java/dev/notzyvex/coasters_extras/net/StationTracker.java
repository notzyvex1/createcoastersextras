package dev.notzyvex.coasters_extras.net;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects what each occupied station is doing and pushes it to clients.
 *
 * <p>The drive hook reports here on the physics tick, which can run more than once per game
 * tick; {@link #flush} then publishes at most one packet per tick.
 *
 * <p>Publishes when the state changes, and otherwise every {@link #RESYNC_TICKS} ticks so the
 * countdown stays honest without the client having to guess. Once every station is empty a
 * single clearing packet goes out and then nothing more.
 */
public final class StationTracker {

    /** How often a running countdown is refreshed even when nothing else changed. */
    private static final int RESYNC_TICKS = 4;

    /** Keyed by the pair of anchorpoints so two reports for the same curve collapse. */
    private static final Map<String, StationPayload.Entry> pending = new LinkedHashMap<>();
    private static String lastKey = "";
    private static long lastSent = Long.MIN_VALUE;

    /**
     * The last thing each station said, kept for anything server-side that wants to ask.
     *
     * <p>Separate from {@link #pending}, which is drained every flush and so is empty most of
     * the time. A display link is evaluated on the server on its own schedule, nowhere near a
     * flush, so it needs somewhere the answer persists rather than a queue.
     *
     * <p>Stamped with the tick it was written. A station that stops reporting has gone empty,
     * and a board must say so rather than freeze on the last countdown it ever saw.
     */
    private static final Map<String, StationPayload.Entry> latest = new LinkedHashMap<>();
    private static final Map<String, Long> latestAt = new LinkedHashMap<>();

    /** Ticks a report stays readable. Comfortably longer than the physics tick interval. */
    private static final int STALE_TICKS = 20;

    /** Key for a curve, order-independent so either end asks the same question. */
    private static String key(BlockPos a, BlockPos b) {
        long lo = Math.min(a.asLong(), b.asLong());
        long hi = Math.max(a.asLong(), b.asLong());
        return lo + "/" + hi;
    }

    /** What this station is doing right now, or null if nothing is on it. */
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

    /** Called from the drive hook for every cart sitting on a station curve. */
    public static void report(BlockPos a, BlockPos b, byte state, int ticksLeft, int dwell,
                              long now) {
        if (a == null || b == null) return;
        // The KEY is order-independent so two reports for one curve collapse into one entry.
        // The ENTRY keeps the order it was given -- a is the end the cart came from and b is
        // the end it stops at, and the platform light needs to know which way that is.
        String key = key(a, b);
        StationPayload.Entry entry = new StationPayload.Entry(a, b, state, ticksLeft, dwell);
        synchronized (pending) {
            pending.put(key, entry);
            latest.put(key, entry);
            latestAt.put(key, now);
        }
    }

    /** Called once per server tick. */
    public static void flush(ServerLevel level) {
        List<StationPayload.Entry> snapshot;
        synchronized (pending) {
            snapshot = new ArrayList<>(pending.values());
            pending.clear();
        }

        // Compare on everything except the countdown, which changes every tick by design.
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
