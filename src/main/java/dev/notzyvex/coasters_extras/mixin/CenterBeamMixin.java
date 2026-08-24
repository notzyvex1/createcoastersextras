package dev.notzyvex.coasters_extras.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.notzyvex.coasters_extras.client.BeamModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.silvergold.simulatedcoasters.client.track.AnchorPeerTrackCurveVisual$BezierTrackVisual",
       remap = false)
public class CenterBeamMixin {

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Ldev/silvergold/simulatedcoasters/client/track/CoasterTrackCenterBeamModel;"
                           + "CENTER_BEAM_SEGMENT:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"
            ),
            require = 0
    )
    private PartialModel coasters_extras$ourBeam(@Local(argsOnly = true) BezierConnection bc) {
        PartialModel theirs =
                dev.silvergold.simulatedcoasters.client.track.CoasterTrackCenterBeamModel.CENTER_BEAM_SEGMENT;
        if (bc == null || bc.getMaterial() == null) {
            return theirs;
        }
        ResourceLocation id = bc.getMaterial().id;
        PartialModel ours = BeamModels.forMaterial(id.getNamespace(), id.getPath());
        return ours != null ? ours : theirs;
    }
}
