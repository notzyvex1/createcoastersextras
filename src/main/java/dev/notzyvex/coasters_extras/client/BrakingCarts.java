package dev.notzyvex.coasters_extras.client;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side cache of where carts are braking, used to tint their wheels.
 *
 * <p>Holds a handful of points at most, so the lookup the renderer does per wheel is a short
 * linear scan -- cheap enough to run every frame, unlike searching the track graph.
 *
 * <p>Entries expire on their own. If the server stops sending (cart left the brake, player
 * changed dimension, connection hiccup) the glow fades instead of sticking forever.
 */
public final class BrakingCarts {

    private static final long TTL_MS = 400;
    private static final double RADIUS_SQ = 3.5 * 3.5;

    private static volatile List<double[]> points = new ArrayList<>();
    private static volatile long stamp = 0;

    public static void accept(List<double[]> incoming) {
        points = incoming;
        stamp = System.currentTimeMillis();
    }

    /** True if this wheel belongs to a cart that is currently braking. */
    public static boolean isBraking(Vec3 wheel) {
        if (System.currentTimeMillis() - stamp > TTL_MS) {
            return false;
        }
        for (double[] p : points) {
            double dx = p[0] - wheel.x, dy = p[1] - wheel.y, dz = p[2] - wheel.z;
            if (dx * dx + dy * dy + dz * dz <= RADIUS_SQ) {
                return true;
            }
        }
        return false;
    }

    private BrakingCarts() {}
}
