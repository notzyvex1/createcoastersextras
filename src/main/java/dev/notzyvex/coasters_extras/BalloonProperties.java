package dev.notzyvex.coasters_extras;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Block properties matching Create: Coasters Simulated's red balloon as closely as
 * possible, so our balloons feel identical to break, place and push.
 *
 * <p>Their {@code RedBalloonBlock.defaultProperties()} is public, so once we depend on
 * their jar at compile time we should call that instead of duplicating values here --
 * that way we track any changes they make. Kept standalone for now so the mod builds
 * without their jar present.
 */
public final class BalloonProperties {

    public static BlockBehaviour.Properties create() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(0.1F)          // pops almost instantly, like wool/glass
                .sound(SoundType.WOOL)
                .noOcclusion()
                .isSuffocating((s, l, p) -> false)
                .isViewBlocking((s, l, p) -> false)
                .pushReaction(PushReaction.DESTROY);
    }

    private BalloonProperties() {}
}
