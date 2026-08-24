package dev.notzyvex.coasters_extras.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Renders our tracks with the base mod's coaster geometry instead of Create's plain one.
 *
 * <p>This is the fix for variant tracks looking wrong beside a normal coaster track --
 * no centre beam, different height, different sleeper spacing. Their renderer gates on:
 *
 * <pre>
 *   private static boolean useSplineRails(BezierConnection bc) {
 *       return bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id);
 *   }
 *   ...
 *   if (!useSplineRails(bc)) { ...plain fallback... }
 * </pre>
 *
 * Our materials failed it, so every variant took the fallback path. The assets were never
 * the problem -- they were proven byte-identical to theirs; the renderer simply refused to
 * use the spline path for them.
 *
 * <p>Matching on namespace means every track we add passes automatically.
 *
 * <p>Note the centre beam model is a single hardcoded static in
 * {@code CoasterTrackCenterBeamModel}, so it always draws from their folder regardless of
 * material. That is fine: the beam is metal on every variant, and only the rails and
 * sleepers carry the material.
 */
@Mixin(targets = "dev.silvergold.simulatedcoasters.client.track.CoasterCurveRailInstancing",
       remap = false)
public class CoasterCurveRailInstancingMixin {

    @Inject(method = "useSplineRails", at = @At("HEAD"), cancellable = true)
    private static void coasters_extras$ourTracksUseSplines(BezierConnection bc,
                                                            CallbackInfoReturnable<Boolean> cir) {
        if (bc == null || bc.getMaterial() == null) {
            return;
        }
        if ("coasters_extras".equals(bc.getMaterial().id.getNamespace())) {
            cir.setReturnValue(true);
        }
        // otherwise fall through to their check, leaving their own track untouched
    }
}
