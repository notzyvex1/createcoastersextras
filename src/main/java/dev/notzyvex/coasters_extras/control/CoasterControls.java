package dev.notzyvex.coasters_extras.control;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Who is currently driving a coaster.
 *
 * <p>Server-side only, and deliberately a set of players rather than a map of players to
 * carts. A cart is a physics sub-level whose blocks live in a fixed plot while the cart
 * itself flies around the world, so the position of the Controls block says nothing about
 * where its cart currently is. What is reliably true is that a driver is *sitting on* the
 * cart they are driving, so the drive hook matches them by proximity -- the rider moves with
 * the cart, and the block does not.
 *
 * <p>Not persisted. Standing up, logging out or the server restarting should all end up with
 * nobody driving, which is what an empty set on startup gives for free.
 */
public final class CoasterControls {

    /**
     * Driver -> the Controls block they took hold of.
     *
     * <p>The position is not used to find their cart -- a cart's blocks sit in a fixed plot
     * while the cart itself moves, so it could not be. It is remembered so the lever on that
     * particular stand can be thrown to match what the driver is asking for.
     */
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

    /** Returns the state it ended up in. */
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

    /**
     * Drops anyone who has stopped riding.
     *
     * <p>Called from the drive hook rather than hooked to a dismount event: there are several
     * ways to stop riding and only one of them is pressing shift. Checking the condition that
     * actually matters is shorter than catching every way of reaching it.
     */
    public static void dropIfNotRiding(Player player) {
        if (player != null && !player.isPassenger()) {
            stop(player);
        }
    }

    private CoasterControls() {}
}
