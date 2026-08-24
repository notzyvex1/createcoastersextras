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

/**
 * Draws the junction plates and girders on our tracks, so a curve looks attached to its
 * anchorpoint instead of stopping short of it.
 *
 * <p>The curve visual keeps a private {@code isCoaster} flag, set in its constructor by
 * comparing the curve's material to the base mod's own:
 *
 * <pre>
 *   22: getstatic     CoasterTrackMaterials.COASTER
 *   31: putfield      isCoaster:Z
 * </pre>
 *
 * <p>False for every one of ours. And at offset 251 that flag guards a jump clean over the
 * block that instances the anchorpoint hardware -- {@code createFlywheelPlate} at 498 and 534,
 * {@code createFlywheelGirders} at 570 and 606, one pair per end of the curve. So our rails
 * were drawn and the joint furniture between them and the anchorpoint simply never was.
 *
 * <p>This is the third hardcoded comparison against {@code COASTER} to bite us, after
 * {@code useSplineRails} sending our curves down the plain fallback path and the block-entity
 * renderer resolving its models from it. This one hid the longest because it fails silently:
 * nothing renders wrong, something just does not render.
 *
 * <p>Geometry was ruled out first. Our {@code segment_left}, {@code segment_right},
 * {@code segment_center_beam} and {@code tie} OBJs are byte-identical to the base mod's, so a
 * seam was never going to be explained by the meshes.
 *
 * <p>The redirect answers with the curve's OWN material, which makes the comparison a match and
 * costs nothing for anyone else -- anything that is not ours still sees the original field.
 */
@Mixin(targets = "dev.silvergold.simulatedcoasters.client.track."
              + "AnchorPeerTrackCurveVisual$BezierTrackVisual",
       remap = false)
public class CurveVisualAnchorHardwareMixin {

    /**
     * @param outer the synthetic enclosing-instance parameter every inner-class constructor
     *              carries; unused, but it occupies slot 0 and the handler will not bind
     *              without it
     */
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
