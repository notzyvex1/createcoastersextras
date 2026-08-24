package dev.notzyvex.coasters_extras;

import dev.silvergold.simulatedcoasters.balloon.RedBalloonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A balloon in one of our colours.
 *
 * <p>Extends the base mod's {@link RedBalloonBlock} rather than reimplementing it, so we
 * inherit the block entity, voxel shapes, sub-level collision shape and placement logic
 * verbatim. Everything that makes a balloon <em>behave</em> like a balloon lives there.
 *
 * <p>Subclassing alone is not enough: their systems test
 * {@code state.is(RED_BALLOON.get())} rather than {@code instanceof}, so this class only
 * works in combination with the mixins in {@code dev.notzyvex.coasters_extras.mixin}, which
 * widen those checks to the {@code coasters_extras:balloons} tag.
 */
public class ExtraBalloonBlock extends RedBalloonBlock {

    private final BalloonColor color;

    public ExtraBalloonBlock(BalloonColor color, BlockBehaviour.Properties props) {
        super(props);
        this.color = color;
    }

    public BalloonColor getColor() {
        return color;
    }

    /**
     * Middle-click needs to return <em>our</em> balloon, not theirs -- the inherited
     * implementation hands back a red one.
     */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }
}
