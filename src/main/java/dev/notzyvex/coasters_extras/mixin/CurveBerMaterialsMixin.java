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

@Mixin(targets = "dev.silvergold.simulatedcoasters.client.track."
              + "AnchorPeerTrackCurveBerRender$CurveBerMaterials",
       remap = false)
public class CurveBerMaterialsMixin {

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
            return null;
        }
    }
}
