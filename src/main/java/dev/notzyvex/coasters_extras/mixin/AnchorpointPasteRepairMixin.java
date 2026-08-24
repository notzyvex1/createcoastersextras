package dev.notzyvex.coasters_extras.mixin;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes coaster track survive being pasted somewhere it was not built.
 *
 * <p>An anchorpoint stores its curves in a map keyed by the <em>absolute</em> position of the
 * anchorpoint at the far end. The curve geometry beside it is stored relative to the owning
 * block entity, so the shape is fine wherever it lands -- but the keys still point at wherever
 * the track was originally built. Paste a structure, schematic or world-edit copy of a coaster
 * and every key is wrong.
 *
 * <p>The base mod already solves this: {@code repairAnchorPeerCurveKeys()} recomputes each key
 * from its curve's own endpoints. It is simply never called outside Ponder, where their restore
 * hook runs it on every rewind -- which is why track works in a Ponder scene and not in a
 * pasted build.
 *
 * <p>Calling their own repair on load fixes it everywhere. It is idempotent, so on ordinary
 * loads where the keys are already right it does nothing, and it costs one pass over a map
 * that holds at most two entries.
 */
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
            // their internals; a failed repair should cost a pasted curve, not the chunk load
        }
    }
}
