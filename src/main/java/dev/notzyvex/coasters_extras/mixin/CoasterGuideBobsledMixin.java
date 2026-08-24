package dev.notzyvex.coasters_extras.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.EnumSet;
import java.util.Set;

@Mixin(targets = "dev.silvergold.simulatedcoasters.track.cart.CoasterTrackGuideConstraint",
       remap = false)
public abstract class CoasterGuideBobsledMixin {

    @Shadow(remap = false)
    @Final
    private static Set<ConstraintJointAxis> RAIL_GUIDE_LOCKED_AXES;

    @Unique
    private static final Set<ConstraintJointAxis> COASTERS_EXTRAS$BOBSLED_AXES = EnumSet.of(
            ConstraintJointAxis.LINEAR_Y,
            ConstraintJointAxis.LINEAR_Z,
            ConstraintJointAxis.ANGULAR_Y,
            ConstraintJointAxis.ANGULAR_Z);

    @Unique
    private static final ResourceLocation COASTERS_EXTRAS$BOBSLED =
            ResourceLocation.fromNamespaceAndPath("coasters_extras", "bobsled_track");

    @Redirect(
            method = "updateAfterSnap",
            at = @At(
                    value = "FIELD",
                    opcode = org.objectweb.asm.Opcodes.GETSTATIC,
                    target = "Ldev/silvergold/simulatedcoasters/track/cart/"
                           + "CoasterTrackGuideConstraint;RAIL_GUIDE_LOCKED_AXES:Ljava/util/Set;"
            ),
            require = 0,
            remap = false
    )
    private static Set<ConstraintJointAxis> coasters_extras$axesFor(
            @Local(argsOnly = true) CoasterPathEdge railEdge) {
        try {
            var bc = railEdge.bezier();
            if (bc != null && bc.getMaterial() != null) {
                ResourceLocation id = bc.getMaterial().id;
                if (id != null
                        && "coasters_extras".equals(id.getNamespace())
                        && "bobsled_track".equals(id.getPath())) {
                    return COASTERS_EXTRAS$BOBSLED_AXES;
                }
            }
        } catch (Throwable ignored) {
        }
        return RAIL_GUIDE_LOCKED_AXES;
    }
}
