package dev.notzyvex.coasters_extras.control;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CoasterControlsBlock extends HorizontalDirectionalBlock
        implements net.minecraft.world.level.block.EntityBlock {

    public static final com.mojang.serialization.MapCodec<CoasterControlsBlock> CODEC =
            simpleCodec(CoasterControlsBlock::new);

    public static final net.minecraft.world.level.block.state.properties.IntegerProperty THROTTLE =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("throttle", 0, 2);

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 13, 13);

    public CoasterControlsBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(THROTTLE, 1));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, THROTTLE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public @Nullable net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(
            BlockPos pos, BlockState state) {
        return new CoasterControlsBlockEntity(pos, state);
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity>
    net.minecraft.world.level.block.entity.@Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof CoasterControlsBlockEntity controls) {
                CoasterControlsBlockEntity.clientTick(lvl, pos, st, controls);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.isPassenger()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("coasters_extras.controls.sit_first")
                                 .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.CONSUME;
        }

        if (level.isClientSide()) {
            dev.notzyvex.coasters_extras.client.ClientDriving.toggle();
            return InteractionResult.SUCCESS;
        }

        boolean driving = CoasterControls.toggle(player, pos);
        if (!driving) {
            level.setBlock(pos, state.setValue(THROTTLE, 1), 3);
        }
        player.displayClientMessage(
                Component.translatable(driving ? "coasters_extras.controls.taken"
                                               : "coasters_extras.controls.released")
                         .withStyle(driving ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                        0.5F, driving ? 1.4F : 0.9F);
        return InteractionResult.SUCCESS;
    }
}
