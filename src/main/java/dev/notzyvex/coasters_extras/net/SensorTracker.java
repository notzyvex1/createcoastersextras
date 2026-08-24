package dev.notzyvex.coasters_extras.net;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Which sensor curves have a coaster on them, so a Sensor Block can ask.
 *
 * <p>A track curve cannot emit redstone: it is a connection between two anchorpoints, not a
 * block, and there is nothing in the world for a signal to come out of. The Sensor Block is
 * the missing block. The drive hook already knows the instant a cart crosses a sensor curve,
 * so it reports here and the block reads it -- the same split Create uses for its Train
 * Observer.
 *
 * <p>Server side only. A curve is identified by its two anchorpoints, ordered so that either
 * end names the same curve.
 */
public final class SensorTracker {

    /** Curve -> the last game tick a cart was seen on it. */
    private static final Map<String, Long> SEEN = new HashMap<>();

    /**
     * How long a curve stays "occupied" after the last sighting.
     *
     * <p>Not zero, because the physics tick can skip a game tick and a raw sighting would
     * flicker. Not long either: at six ticks the signal visibly lagged behind the coaster.
     * Two is one game tick of slack, which covers the skip and nothing else -- the light goes
     * out as the coaster leaves rather than a third of a second later.
     */
    private static final long HOLD_TICKS = 2;

    /** Stops the map growing without bound in a world full of dismantled sensors. */
    private static final long FORGET_AFTER = 400;

    public static String key(BlockPos a, BlockPos b) {
        if (a == null || b == null) return "";
        long x = a.asLong(), y = b.asLong();
        return Math.min(x, y) + "/" + Math.max(x, y);
    }

    /** Called from the drive hook while a cart is on a sensor curve. */
    public static void report(BlockPos a, BlockPos b, long now) {
        String k = key(a, b);
        if (k.isEmpty()) return;
        synchronized (SEEN) {
            SEEN.put(k, now);
            if (SEEN.size() > 64 && now % 200 == 0) {
                SEEN.entrySet().removeIf(e -> now - e.getValue() > FORGET_AFTER);
            }
        }
    }

    /** True if a coaster is on, or has just left, the curve joining these anchorpoints. */
    public static boolean occupied(BlockPos a, BlockPos b, long now) {
        String k = key(a, b);
        if (k.isEmpty()) return false;
        synchronized (SEEN) {
            Long seen = SEEN.get(k);
            return seen != null && now - seen <= HOLD_TICKS;
        }
    }

    private SensorTracker() {}
}
