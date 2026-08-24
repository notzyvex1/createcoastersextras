package dev.notzyvex.coasters_extras.net;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class SensorTracker {

    private static final Map<String, Long> SEEN = new HashMap<>();

    private static final long HOLD_TICKS = 2;

    private static final long FORGET_AFTER = 400;

    public static String key(BlockPos a, BlockPos b) {
        if (a == null || b == null) return "";
        long x = a.asLong(), y = b.asLong();
        return Math.min(x, y) + "/" + Math.max(x, y);
    }

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
