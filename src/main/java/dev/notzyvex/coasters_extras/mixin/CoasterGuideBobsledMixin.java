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

/**
 * Bobsled track: free the cart to ROLL, while keeping it pinned to the rail.
 *
 * <h2>What the base mod does</h2>
 *
 * {@code CoasterTrackGuideConstraint.updateAfterSnap} pins each cart to the rail line with a
 * Sable {@code GenericConstraint} whose locked axes are:
 * <pre>
 *   RAIL_GUIDE_LOCKED_AXES = LINEAR_Y, LINEAR_Z, ANGULAR_X, ANGULAR_Y, ANGULAR_Z
 * </pre>
 * Only {@code LINEAR_X} (forward along the rail) is free -- that is why a normal cart runs on
 * rails: it cannot move sideways, cannot drop, cannot roll.
 *
 * <h2>What a bobsled changes</h2>
 *
 * A bobsled is not railed. It slides in a trough and rides up the banked walls, so it needs:
 * <ul>
 *   <li>{@code ANGULAR_X} <b>unlocked</b> -- free to roll, so it can lean into a corner.</li>
 * </ul>
 * {@code LINEAR_Z} stays locked so it does not fall through the floor, and yaw/pitch stay
 * locked so it still follows the track's heading rather than spinning out. This redirect swaps
 * in that relaxed set, but only for an edge whose curve is our {@code bobsled_track} material;
 * every other track keeps the rigid rail exactly as before.
 *
 * <h2>Why LINEAR_Y is locked again</h2>
 *
 * The first version of this unlocked {@code LINEAR_Y} as well, on the reasoning that a bobsled
 * slides across its channel. It does -- but a coaster curve has no channel. There is no
 * collision geometry either side of the rail, so "free to slide sideways" meant "free to slide
 * off", and the track dropped every cart that entered it. Building the trough would mean
 * authoring collision walls along an arbitrary bezier, which is a large piece of work in
 * Sable's collision layer for a result the player would mostly not see.
 *
 * <p>The bank is the part you actually see, and it does not need the trough. So this now frees
 * ONLY {@code ANGULAR_X}: the cart is still pinned to the rail exactly like every other track
 * and cannot come off it, but it is free to roll. {@code CoasterCartDriveMixin} then drives
 * that roll from the corner the cart is taking, which is the same thing physics would have
 * produced if the walls existed.
 */
@Mixin(targets = "dev.silvergold.simulatedcoasters.track.cart.CoasterTrackGuideConstraint",
       remap = false)
public abstract class CoasterGuideBobsledMixin {

    /** The base mod's rigid rail lock, read back for every non-bobsled track unchanged. */
    @Shadow(remap = false)
    @Final
    private static Set<ConstraintJointAxis> RAIL_GUIDE_LOCKED_AXES;

    /**
     * Bobsled lock: everything the rail locks EXCEPT roll.
     *
     * <p>Lateral slide stays LOCKED. Freeing it is what made carts fall off the side, and the
     * lean it was meant to produce is driven directly in {@code CoasterCartDriveMixin} instead.
     */
    @Unique
    private static final Set<ConstraintJointAxis> COASTERS_EXTRAS$BOBSLED_AXES = EnumSet.of(
            ConstraintJointAxis.LINEAR_Y,     // STAY ON THE RAIL -- see below
            ConstraintJointAxis.LINEAR_Z,     // keep it out of the floor
            ConstraintJointAxis.ANGULAR_Y,    // keep it pointed along the track
            ConstraintJointAxis.ANGULAR_Z);   // no pitch spin

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
                // Compare the raw namespace and path, NOT id.equals(): our
                // ResourceLocationCoasterAliasMixin makes every "coasters_extras:*_track" id
                // report equal to "simulatedcoasters:coaster_track", so a plain .equals() here
                // matched the DEFAULT track too and relaxed its rail guide -- which is exactly
                // why a cart fell straight through default track. getNamespace()/getPath() return
                // the real strings and are not aliased, so this matches ONLY the real bobsled.
                if (id != null
                        && "coasters_extras".equals(id.getNamespace())
                        && "bobsled_track".equals(id.getPath())) {
                    return COASTERS_EXTRAS$BOBSLED_AXES;
                }
            }
        } catch (Throwable ignored) {
            // Any base-mod surprise: fall through to the rigid rail, never crash the physics.
        }
        return RAIL_GUIDE_LOCKED_AXES;
    }
}
