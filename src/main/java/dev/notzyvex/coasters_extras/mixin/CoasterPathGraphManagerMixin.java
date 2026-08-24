package dev.notzyvex.coasters_extras.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
    }
}
