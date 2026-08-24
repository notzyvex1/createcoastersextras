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

/**
 * Draws the centre beam from the curve's own material instead of the base mod's.
 *
 * <p>Their visual builds ties and rails from {@code bc.getMaterial().getModelHolder()}, which
 * follows the curve's material correctly, but takes the beam from a single hardcoded static
 * pinned to {@code simulatedcoasters:.../segment_center_beam}. On any track of ours that left
 * a grey strip down the middle of an otherwise coloured track -- most obvious on boost and
 * sensor, where everything either side of it is bright.
 *
 * <p>Redirecting the field read is enough: the model our folders already ship for that
 * material is substituted, and their own tracks are untouched because the lookup returns null
 * for any namespace but ours.
 */
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
