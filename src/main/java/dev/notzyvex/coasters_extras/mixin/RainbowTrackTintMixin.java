package dev.notzyvex.coasters_extras.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.lib.instance.ColoredLitOverlayInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.silvergold.simulatedcoasters.client.track.bent.BentRailInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Colours the Rainbow Track a segment at a time, so a whole ride is one spectrum.
 *
 * <p>Three attempts to do this with a texture all failed for the same reason: a tie samples
 * a narrow slice of its texture rather than the full width, so a gradient collapsed to
 * roughly one colour and hard bands collapsed to "whichever band sits at the left". No
 * arrangement of pixels fixes that, because the model decides what gets sampled.
 *
 * <p>The base mod already solves this for its own dyed track. Every rail segment is a
 * separate {@link Instance} carrying its own RGB, written by their {@code applyRgbMultiplier}
 * and committed with {@code setChanged()}. Giving each segment a different hue is therefore
 * a per-instance colour change, not a texture problem -- and it works no matter how the
 * model is unwrapped.
 *
 * <p>Runs at TAIL so their own tint is applied first and then overwritten; anything they do
 * with dyes on their track is untouched, because this returns immediately unless the curve
 * is ours.
 */
@Mixin(targets = "dev.silvergold.simulatedcoasters.client.track.CoasterCurveRailInstancing",
       remap = false)
public class RainbowTrackTintMixin {

    /** How much of the spectrum one curve spans. Below 1.0 a short curve is not all red. */
    private static final double SWEEP = 0.85;

    @Inject(method = "apply", at = @At("TAIL"))
    private static void coasters_extras$rainbow(Vec3 visualPos,
                                                BezierConnection bc,
                                                TransformedInstance[] ties,
                                                @Nullable BentRailInstance[] bentLeft,
                                                @Nullable BentRailInstance[] bentRight,
                                                @Nullable BentRailInstance[] bentCenterBeam,
                                                int railDiffuseRgbPacked,
                                                int beamDiffuseRgbPacked,
                                                @Nullable Level level,
                                                CallbackInfo ci) {
        try {
            if (bc == null || bc.getMaterial() == null) return;
            var id = bc.getMaterial().id;
            if (!"coasters_extras".equals(id.getNamespace())
                    || !"rainbow_track".equals(id.getPath())) {
                return;
            }
            tint(bentLeft);
            tint(bentRight);
            tint(bentCenterBeam);
            tint(ties);
        } catch (Throwable ignored) {
            // a colour is never worth breaking someone's track rendering over
        }
    }

    /**
     * Walks a segment array and gives each entry its own hue along the curve.
     *
     * <p>Takes Object[] so the same code serves both the rail arrays and the tie array.
     * The injection signature itself must name the exact types -- Mixin matches on the
     * descriptor, and declaring these as Object there made the whole injection fail to
     * apply.
     */
    private static void tint(@Nullable Object[] arr) {
        if (arr == null || arr.length == 0) return;
        for (int i = 0; i < arr.length; i++) {
            if (!(arr[i] instanceof ColoredLitOverlayInstance inst)) continue;
            float hue = (float) ((double) i / arr.length * SWEEP);
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f);
            inst.red   = (byte) ((rgb >> 16) & 0xFF);
            inst.green = (byte) ((rgb >> 8) & 0xFF);
            inst.blue  = (byte) (rgb & 0xFF);
            if (inst instanceof Instance flywheel) {
                flywheel.setChanged();   // without this the buffer keeps the old colour
            }
        }
    }
}
