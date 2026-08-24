package dev.notzyvex.coasters_extras.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Emits redstone while a coaster is on the Sensor Track it is linked to.
 *
 * <p>The Sensor Track detects a coaster perfectly well; what it cannot do is emit a signal,
 * because a track curve is a connection between two anchorpoints rather than a block in the
 * world. There is nothing there for redstone to come out of. This block is that missing
 * piece, and it does not have to be anywhere near the track it watches -- link it, put it
 * where the wiring is.
 *
 * <p>Powers like a lever: full strength, every side, including through the block it sits on,
 * so a Redstone Link or a door beside it just works.
 */
public class SensorBlock extends Block implements EntityBlock {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public SensorBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SensorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        // Server only: the link is server state and so is the coaster that trips it.
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof SensorBlockEntity sensor) {
                SensorBlockEntity.serverTick(lvl, pos, st, sensor);
            }
        };
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    /** Also powers the block it is attached to, so it can drive a door or a link directly. */
    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        return state.getValue(POWERED) ? 15 : 0;
    }
}
