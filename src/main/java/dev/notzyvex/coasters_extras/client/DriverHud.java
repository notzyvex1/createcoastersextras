package dev.notzyvex.coasters_extras.client;

import dev.notzyvex.coasters_extras.CoastersExtras;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The speedometer shown while driving a coaster.
 *
 * <p>Speed is measured from how far the player actually moved last tick rather than asked for
 * over the network. A rider is carried by the cart, so their own displacement <em>is</em> the
 * cart's speed -- which means the readout costs no packets, cannot fall out of sync, and works
 * on any server.
 *
 * <p>Smoothed, because a raw per-tick delta jitters by a block or two a second and an unstable
 * number is unreadable.
 *
 * <p>It takes over the experience bar's slot while driving, the way Create's Train Controls
 * do. That strip is the one piece of screen a player already watches without thinking, and
 * hiding the vanilla bar rather than drawing beside it keeps the corner from turning into a
 * pile of overlapping gauges.
 */
@EventBusSubscriber(modid = CoastersExtras.MOD_ID, value = Dist.CLIENT)
public final class DriverHud {

    /** Matches DRIVER_MAX_SPEED in the drive hook -- full bar means full throttle. */
    private static final double MAX_SPEED = 28.0;

    /** The experience bar's own geometry, which this replaces. */
    private static final int BAR_W = 182;
    private static final int BAR_H = 5;
    /** Ticks along the bar, so speed is readable as a position and not just a length. */
    private static final int NOTCHES = 7;

    /** Fraction of the new reading taken each tick. Low enough to be steady, high enough to feel live. */
    private static final double SMOOTHING = 0.25;

    private static Vec3 last = null;
    private static double speed = 0;
    private static boolean driving = false;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            last = null;
            driving = false;
            return;
        }

        driving = ClientDriving.isDriving();
        Vec3 now = mc.player.position();
        if (last != null) {
            // Distance covered in one tick, in blocks; twenty ticks to the second.
            double sample = now.distanceTo(last) * 20.0;
            speed += (sample - speed) * SMOOTHING;
        }
        last = now;
    }

    /** Registered on the mod bus, above everything else so the gauge is never buried. */
    @EventBusSubscriber(modid = CoastersExtras.MOD_ID, value = Dist.CLIENT,
                        bus = EventBusSubscriber.Bus.MOD)
    public static final class Layers {
        @SubscribeEvent
        static void register(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR,
                    ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "driver_hud"),
                    DriverHud::render);
        }
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (!driving) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // Exactly where the experience bar sits, because that is what it is standing in for.
        int x = (g.guiWidth() - BAR_W) / 2;
        int y = g.guiHeight() - 29;

        double clamped = Math.max(0, Math.min(MAX_SPEED, speed));
        double fraction = clamped / MAX_SPEED;
        int filled = (int) Math.round(fraction * BAR_W);

        // Track, with a lip on top and a shadow under, so it reads as recessed.
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, 0xFF0B0D10);
        g.fill(x, y, x + BAR_W, y + BAR_H, 0xFF22262C);
        g.fill(x, y, x + BAR_W, y + 1, 0xFF15181C);

        if (filled > 0) {
            // Shaded along its length rather than one flat colour: the leading edge carries
            // the colour of the speed you are actually at, and the tail shows where you came
            // from, which is what makes a glance tell you whether you are still accelerating.
            for (int i = 0; i < filled; i++) {
                int rgb = cellColour((double) i / BAR_W);
                g.fill(x + i, y + 1, x + i + 1, y + BAR_H, 0xFF000000 | rgb);
            }
            g.fill(x + filled - 1, y, x + filled, y + BAR_H, 0xFFFFFFFF);   // leading edge
        }

        // Notches, drawn over the fill so they stay readable at any speed.
        for (int n = 1; n < NOTCHES; n++) {
            int nx = x + (int) Math.round(BAR_W * (n / (double) NOTCHES));
            g.fill(nx, y, nx + 1, y + BAR_H, 0x50000000);
        }

        // Readout centred above the bar, with the throttle beside it.
        String value = String.format("%.1f", clamped);
        float input = mc.player == null ? 0 : mc.player.zza;
        String arrow = input > 0.05F ? "▲" : input < -0.05F ? "▼" : "●";
        int arrowColour = input > 0.05F ? 0xFF6BD46B
                        : input < -0.05F ? 0xFFD46B6B : 0xFF6A7078;

        String label = value + " b/s";
        int tw = font.width(label) + 2 + font.width(arrow);
        int tx = (g.guiWidth() - tw) / 2;
        int ty = y - 11;
        g.drawString(font, label, tx, ty, 0xFFE8ECF2, true);
        g.drawString(font, arrow, tx + font.width(label) + 2, ty, arrowColour, true);
    }

    /** Hides the experience bar while driving, so the two do not stack. */
    @SubscribeEvent
    static void onRenderLayer(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event) {
        if (driving && VanillaGuiLayers.EXPERIENCE_BAR.equals(event.getName())) {
            event.setCanceled(true);
        }
    }

    /** Green at rest, amber through the middle, red at the top of the range. */
    private static int cellColour(double t) {
        float hue = (float) (0.33 * (1.0 - t));   // 0.33 green -> 0.0 red
        return java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f) & 0xFFFFFF;
    }

    private DriverHud() {}
}
