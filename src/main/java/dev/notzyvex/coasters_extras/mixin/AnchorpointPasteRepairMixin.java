package dev.notzyvex.coasters_extras.mixin;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity",
       remap = false)
public class AnchorpointPasteRepairMixin {

    @Inject(method = "onLoad", at = @At("TAIL"))
    private void coasters_extras$repairAfterPaste(CallbackInfo ci) {
        try {
            CoasterAnchorpointBlockEntity self = (CoasterAnchorpointBlockEntity) (Object) this;
            if (self.getLevel() == null || self.getLevel().isClientSide()) {
                return;
            }
            self.repairAnchorPeerCurveKeys();
        } catch (Throwable ignored) {
        }
    }
}
