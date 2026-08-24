package dev.notzyvex.coasters_extras.net;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class RideTelemetry {

    public record Sample(double speed, double topSpeed, int carts, long seen) {
        public static final Sample NONE = new Sample(0, 0, 0, Long.MIN_VALUE);
    }

    private static final long FRESH_TICKS = 20;

    private static final long CART_HOLD_TICKS = 10;

    private static final long FORGET_AFTER = 400;

    private static final Map<String, Sample> SAMPLES = new HashMap<>();

    private static final Map<String, Map<Integer, Long>> CARTS = new HashMap<>();

    public static void report(BlockPos a, BlockPos b, int cartId, double speed, long now) {
        String k = SensorTracker.key(a, b);
        if (k.isEmpty()) return;

        int carts;
        synchronized (CARTS) {
            Map<Integer, Long> seen = CARTS.computeIfAbsent(k, x -> new HashMap<>());
            seen.put(cartId, now);
            seen.entrySet().removeIf(e -> now - e.getValue() > CART_HOLD_TICKS);
            carts = seen.size();
        }

        synchronized (SAMPLES) {
            Sample prev = SAMPLES.get(k);
            double top = prev != null && now - prev.seen() <= FORGET_AFTER
                    ? Math.max(prev.topSpeed(), speed)
                    : speed;
            SAMPLES.put(k, new Sample(speed, top, carts, now));

            if (SAMPLES.size() > 64 && now % 200 == 0) {
                SAMPLES.entrySet().removeIf(e -> now - e.getValue().seen() > FORGET_AFTER);
                synchronized (CARTS) {
                    CARTS.keySet().removeIf(key -> !SAMPLES.containsKey(key));
                }
            }
        }
    }

    public static Sample read(BlockPos a, BlockPos b, long now) {
        String k = SensorTracker.key(a, b);
        if (k.isEmpty()) return Sample.NONE;
        synchronized (SAMPLES) {
            Sample s = SAMPLES.get(k);
            if (s == null) return Sample.NONE;
            if (now - s.seen() > FRESH_TICKS) {
                return new Sample(0, s.topSpeed(), 0, s.seen());
            }
            return s;
        }
    }

    public static void resetTopSpeed(BlockPos a, BlockPos b) {
        String k = SensorTracker.key(a, b);
        if (k.isEmpty()) return;
        synchronized (SAMPLES) {
            Sample s = SAMPLES.get(k);
            if (s != null) {
                SAMPLES.put(k, new Sample(s.speed(), 0, s.carts(), s.seen()));
            }
        }
    }

    private RideTelemetry() {}
}
