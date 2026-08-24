package dev.notzyvex.coasters_extras.client;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class BrakingCarts {

    private static final long TTL_MS = 400;
    private static final double RADIUS_SQ = 3.5 * 3.5;

    private static volatile List<double[]> points = new ArrayList<>();
    private static volatile long stamp = 0;

    public static void accept(List<double[]> incoming) {
        points = incoming;
        stamp = System.currentTimeMillis();
    }

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
