package dev.notzyvex.coasters_extras.net;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Live numbers about what is happening on a track curve, so a display board can ask.
 *
 * <p>The drive hook already computes a cart's speed every physics tick and then throws it away,
 * because until now nothing wanted it. This keeps the last value per curve so a Create Display
 * Link can read it, in the same shape as {@link SensorTracker}: a curve is named by its two
 * anchorpoints, ordered so either end names the same curve, and everything is server side.
 *
 * <p>Top speed is deliberately a HIGH-WATER MARK rather than a live value. "Current speed" and
 * "fastest this ride has ever gone" are different questions and a board showing the second one
 * is what makes a ride entrance sign worth building; it survives the cart leaving, and only a
 * redstone reset clears it.
 *
 * <p><b>Thread safety.</b> Every access is synchronised on its map. The drive hook runs from the
 * physics tick and the display link reads from the server tick, and whether those are the same
 * thread is not something the base mod documents -- the existing code hedges as if they are not.
 * Synchronising is cheap here and a {@code ConcurrentModificationException} inside a physics tick
 * is not.
 */
public final class RideTelemetry {

    /** One curve's numbers. */
    public record Sample(double speed, double topSpeed, int carts, long seen) {
        public static final Sample NONE = new Sample(0, 0, 0, Long.MIN_VALUE);
    }

    /**
     * How long a reading stays current after the last sighting.
     *
     * <p>Longer than {@link SensorTracker}'s two ticks on purpose. A sensor light should go out
     * the moment the coaster leaves; a speed board that blanked that fast would flicker to zero
     * between physics ticks and be unreadable. Twenty ticks is one second of hold, which reads as
     * "the ride is running" rather than as a strobe.
     */
    private static final long FRESH_TICKS = 20;

    /** A cart counts as on the curve for this long after its last report. */
    private static final long CART_HOLD_TICKS = 10;

    /** Stops the maps growing without bound in a world full of dismantled rides. */
    private static final long FORGET_AFTER = 400;

    private static final Map<String, Sample> SAMPLES = new HashMap<>();

    /** Curve -> (cart id -> last tick seen), so a cart count does not need the physics engine. */
    private static final Map<String, Map<Integer, Long>> CARTS = new HashMap<>();

    /** Called from the drive hook for every cart on every curve, every physics tick. */
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
            // The high-water mark carries across, and a stale entry is treated as a fresh start
            // so a rebuilt ride does not inherit the record of the one that stood there before.
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

    /** The curve's current numbers, or {@link Sample#NONE} if nothing has been on it lately. */
    public static Sample read(BlockPos a, BlockPos b, long now) {
        String k = SensorTracker.key(a, b);
        if (k.isEmpty()) return Sample.NONE;
        synchronized (SAMPLES) {
            Sample s = SAMPLES.get(k);
            if (s == null) return Sample.NONE;
            if (now - s.seen() > FRESH_TICKS) {
                // Stale: the ride is not running. The top speed is still real, so it is kept and
                // only the live numbers are zeroed -- that is the whole point of a record board.
                return new Sample(0, s.topSpeed(), 0, s.seen());
            }
            return s;
        }
    }

    /** Clears the high-water mark for one curve. Wired to the display link's reset signal. */
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
