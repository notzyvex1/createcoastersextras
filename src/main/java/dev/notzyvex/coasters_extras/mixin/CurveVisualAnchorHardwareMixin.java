package dev.notzyvex.coasters_extras.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerTrackCurveVisual;
import net.minecraft.core.BlockPos;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.silvergold.simulatedcoasters.client.track."
              + "AnchorPeerTrackCurveVisual$BezierTrackVisual",
       remap = false)
public class CurveVisualAnchorHardwareMixin {

    @Redirect(
            method = "<init>",
            at = @At(value = "FIELD",
                     target = "Ldev/silvergold/simulatedcoasters/CoasterTrackMaterials;"
                            + "COASTER:Lcom/simibubi/create/content/trains/track/TrackMaterial;",
                     opcode = Opcodes.GETSTATIC),
            require = 1)
    private static TrackMaterial coasters_extras$countOurTracksAsCoasters(
            AnchorPeerTrackCurveVisual outer, BlockPos pos, BezierConnection curve) {
        if (curve != null && curve.getMaterial() != null
                && "coasters_extras".equals(curve.getMaterial().id.getNamespace())) {
            return curve.getMaterial();
        }
        return CoasterTrackMaterials.COASTER;
    }
}
