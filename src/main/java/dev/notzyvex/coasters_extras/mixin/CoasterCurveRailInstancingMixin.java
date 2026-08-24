package dev.notzyvex.coasters_extras.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
    }
}
