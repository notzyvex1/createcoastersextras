package dev.notzyvex.coasters_extras.client;

import net.minecraft.client.Minecraft;

/**
 * Whether this client is driving, so the speedometer knows to show.
 *
 * <p>Kept client-side rather than synced. The block toggles both copies from the same
 * condition in the same method, so they cannot disagree without the interaction itself having
 * failed -- and a stale flag costs a speedometer, not a desynced ride.
 */
public final class ClientDriving {

    private static boolean driving;

    public static void toggle() {
        driving = !driving;
    }

    /** True only while still riding: standing up ends control on the server too. */
    public static boolean isDriving() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isPassenger()) {
            driving = false;
        }
        return driving;
    }

    private ClientDriving() {}
}
