package dev.notzyvex.coasters_extras.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the base mod's path system accept our track materials as coaster track.
 *
 * <p>{@code isCoasterGraphBezier} is the master predicate -- literally
 * {@code bezier.getMaterial().id.equals(COASTER.id)} -- and the graph, path tracing,
 * edge sampling and track-frame code all gate on it. Our materials fail it, so a curve
 * built from one is invisible to the whole coaster path system: no centre beam, no
 * coaster geometry, and carts do not treat it as track.
 *
 * <p>Matching on the namespace rather than a fixed list means every track we add -- the
 * 16 material variants plus boost, brake and sensor -- passes automatically.
 *
 * <p>Deliberately scoped to this predicate. Placement and drops are left alone so a
 * variant track still lays and breaks as itself rather than as theirs.
 */
@Mixin(targets = "dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager",
       remap = false)
public class CoasterPathGraphManagerMixin {

    @Inject(method = "isCoasterGraphBezier", at = @At("HEAD"), cancellable = true)
    private static void coasters_extras$ourTracksCount(BezierConnection bezier,
                                                       CallbackInfoReturnable<Boolean> cir) {
        if (bezier == null || bezier.getMaterial() == null) {
            return;
        }
        if ("coasters_extras".equals(bezier.getMaterial().id.getNamespace())) {
            cir.setReturnValue(true);
        }
        // otherwise fall through to their own check, so their track is unaffected
    }
}
