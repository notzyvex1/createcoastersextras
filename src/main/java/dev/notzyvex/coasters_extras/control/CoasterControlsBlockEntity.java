package dev.notzyvex.coasters_extras.control;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CoasterControlsBlockEntity extends BlockEntity {

    private static final float BACK = -24f;
    private static final float NEUTRAL = 0f;
    private static final float FORWARD = 24f;

    private static final float EASE = 0.35f;

    private float angle = NEUTRAL;
    private float prevAngle = NEUTRAL;

    public CoasterControlsBlockEntity(BlockPos pos, BlockState state) {
        super(dev.notzyvex.coasters_extras.track.ModTracks.COASTER_CONTROLS_BE.get(), pos, state);
        this.angle = targetFor(state);
        this.prevAngle = this.angle;
    }

    private static float targetFor(BlockState state) {
        return switch (state.getValue(CoasterControlsBlock.THROTTLE)) {
            case 0 -> BACK;
            case 2 -> FORWARD;
            default -> NEUTRAL;
        };
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  CoasterControlsBlockEntity be) {
        be.prevAngle = be.angle;
        float target = targetFor(state);
        be.angle = Mth.lerp(EASE, be.angle, target);
        if (Math.abs(be.angle - target) < 0.05f) {
            be.angle = target;
        }
    }

    public float handleAngle(float partialTick) {
        return Mth.lerp(partialTick, prevAngle, angle);
    }

}
