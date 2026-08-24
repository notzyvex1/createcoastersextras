package dev.notzyvex.coasters_extras.control;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CoasterControls {

    private static final Map<UUID, BlockPos> DRIVERS = new HashMap<>();

    public static boolean isDriving(Player player) {
        if (player == null) return false;
        synchronized (DRIVERS) {
            return DRIVERS.containsKey(player.getUUID());
        }
    }

    public static BlockPos stand(Player player) {
        synchronized (DRIVERS) {
            return player == null ? null : DRIVERS.get(player.getUUID());
        }
    }

    public static boolean toggle(Player player, BlockPos stand) {
        synchronized (DRIVERS) {
            if (DRIVERS.remove(player.getUUID()) != null) {
                return false;
            }
            DRIVERS.put(player.getUUID(), stand.immutable());
            return true;
        }
    }

    public static void stop(Player player) {
        synchronized (DRIVERS) {
            DRIVERS.remove(player.getUUID());
        }
    }

    public static void dropIfNotRiding(Player player) {
        if (player != null && !player.isPassenger()) {
            stop(player);
        }
    }

    private CoasterControls() {}
}
