package dev.notzyvex.coasters_extras.control;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carries the smoothly-moving handle angle for a Coaster Controls block.
 *
 * <p>The throttle a driver has chosen still lives on the block state as a 0/1/2 property, which
 * is what the server sets and what syncs to every client for free. This block entity does not
 * store that -- it stores the <em>visual</em> angle of the handle, which eases toward the angle
 * the current throttle asks for a little each tick. That easing is the whole point: Create's
 * Train Controls lever glides between positions rather than snapping, and a snapping lever is
 * exactly what the three-model block state looked like before this.
 *
 * <p>Two angles are kept -- last tick's and this tick's -- so the renderer can interpolate
 * between them with the frame's partial tick and the motion stays smooth at any framerate,
 * not just on the 20 ticks a second the logic runs at.
 */
public class CoasterControlsBlockEntity extends BlockEntity {

    /** Where the handle rests, and how far it throws each way, in degrees. */
    private static final float BACK = -24f;
    private static final float NEUTRAL = 0f;
    private static final float FORWARD = 24f;

    /** Fraction of the remaining gap closed per tick. Higher snaps faster; this glides. */
    private static final float EASE = 0.35f;

    private float angle = NEUTRAL;
    private float prevAngle = NEUTRAL;

    public CoasterControlsBlockEntity(BlockPos pos, BlockState state) {
        super(dev.notzyvex.coasters_extras.track.ModTracks.COASTER_CONTROLS_BE.get(), pos, state);
        this.angle = targetFor(state);
        this.prevAngle = this.angle;
    }

    /** The angle the handle should reach for the throttle currently on the block state. */
    private static float targetFor(BlockState state) {
        return switch (state.getValue(CoasterControlsBlock.THROTTLE)) {
            case 0 -> BACK;
            case 2 -> FORWARD;
            default -> NEUTRAL;
        };
    }

    /** Runs on the client only: ease the visual angle toward the throttle's target. */
    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  CoasterControlsBlockEntity be) {
        be.prevAngle = be.angle;
        float target = targetFor(state);
        be.angle = Mth.lerp(EASE, be.angle, target);
        // Snap the last hair so it settles instead of forever approaching.
        if (Math.abs(be.angle - target) < 0.05f) {
            be.angle = target;
        }
    }

    /** Handle angle for this frame, interpolated across the partial tick. */
    public float handleAngle(float partialTick) {
        return Mth.lerp(partialTick, prevAngle, angle);
    }

}
