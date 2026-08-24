package dev.notzyvex.coasters_extras.client;

import net.minecraft.client.Minecraft;

public final class ClientDriving {

    private static boolean driving;

    public static void toggle() {
        driving = !driving;
    }

    public static boolean isDriving() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isPassenger()) {
            driving = false;
        }
        return driving;
    }

    private ClientDriving() {}
}
