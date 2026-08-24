package dev.notzyvex.coasters_extras;

import dev.silvergold.simulatedcoasters.balloon.RedBalloonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ExtraBalloonBlock extends RedBalloonBlock {

    private final BalloonColor color;

    public ExtraBalloonBlock(BalloonColor color, BlockBehaviour.Properties props) {
        super(props);
        this.color = color;
    }

    public BalloonColor getColor() {
        return color;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }
}
