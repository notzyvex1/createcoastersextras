package dev.notzyvex.coasters_extras.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import dev.notzyvex.coasters_extras.client.BeamModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackCenterBeamModel;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * Makes our track materials render on the block-entity path, not just the Flywheel one.
 *
 * <p>Everything we had fixed about track appearance was on the instancing path
 * ({@code CoasterCurveRailInstancing}), which is what the world uses. Ponder does not use it
 * -- a Ponder level renders through block entity renderers -- and the BER path resolves its
 * models like this:
 *
 * <pre>
 *   0: getstatic  CoasterTrackMaterials.COASTER
 *   3: invokevirtual TrackMaterial.getModelHolder()
 * </pre>
 *
 * <p>It never asks the curve what material it is. Every custom track therefore drew with the
 * base mod's own models, which is why a Brake Track whose texture is bright red appeared as
 * plain grey metal in its Ponder scene. Same story one line further down, where the centre
 * beam is a hardcoded static -- the identical bug {@code CenterBeamMixin} fixes for Flywheel.
 *
 * <p>Both redirects fall back to the original value for anything that is not ours, so the
 * base mod's own track is untouched.
 */
@Mixin(targets = "dev.silvergold.simulatedcoasters.client.track."
              + "AnchorPeerTrackCurveBerRender$CurveBerMaterials",
       remap = false)
public class CurveBerMaterialsMixin {

    /**
     * Swaps in the curve's real material.
     *
     * <p>The redirect handler takes the enclosing method's own parameters after its own, which
     * is how a static field access -- carrying no context of its own -- can still be answered
     * with "it depends which curve".
     */
    @Redirect(
            method = "from",
            at = @At(value = "FIELD",
                     target = "Ldev/silvergold/simulatedcoasters/CoasterTrackMaterials;"
                            + "COASTER:Lcom/simibubi/create/content/trains/track/TrackMaterial;",
                     opcode = Opcodes.GETSTATIC),
            require = 1)
    private static TrackMaterial coasters_extras$realMaterial(CoasterAnchorpointBlockEntity be,
                                                              BlockPos peer) {
        TrackMaterial ours = coasters_extras$materialOf(be, peer);
        return ours != null ? ours : CoasterTrackMaterials.COASTER;
    }

    /** Same fix for the centre beam, which is hardcoded to the base mod's segment. */
    @Redirect(
            method = "from",
            at = @At(value = "FIELD",
                     target = "Ldev/silvergold/simulatedcoasters/client/track/"
                            + "CoasterTrackCenterBeamModel;CENTER_BEAM_SEGMENT:"
                            + "Ldev/engine_room/flywheel/lib/model/baked/PartialModel;",
                     opcode = Opcodes.GETSTATIC),
            require = 1)
    private static PartialModel coasters_extras$realBeam(CoasterAnchorpointBlockEntity be,
                                                         BlockPos peer) {
        TrackMaterial ours = coasters_extras$materialOf(be, peer);
        if (ours != null) {
            PartialModel beam = BeamModels.forMaterial(ours.id.getNamespace(), ours.id.getPath());
            if (beam != null) return beam;
        }
        return CoasterTrackCenterBeamModel.CENTER_BEAM_SEGMENT;
    }

    /** The material of the curve joining this anchorpoint to that peer, if it is one of ours. */
    private static TrackMaterial coasters_extras$materialOf(CoasterAnchorpointBlockEntity be,
                                                            BlockPos peer) {
        try {
            if (be == null || peer == null) return null;
            Map<BlockPos, BezierConnection> curves = be.getAnchorPeerCurvesView();
            if (curves == null) return null;
            BezierConnection bc = curves.get(peer);
            if (bc == null || bc.getMaterial() == null) return null;
            return "coasters_extras".equals(bc.getMaterial().id.getNamespace())
                    ? bc.getMaterial() : null;
        } catch (Throwable ignored) {
            return null;   // fall back to theirs rather than break their renderer
        }
    }
}
