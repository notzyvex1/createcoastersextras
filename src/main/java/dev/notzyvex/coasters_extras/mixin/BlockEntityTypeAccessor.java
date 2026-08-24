package dev.notzyvex.coasters_extras.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/**
 * Exposes {@link BlockEntityType}'s valid-block set so we can add our balloons to the base
 * mod's balloon block entity type.
 *
 * <p>A {@code BlockEntityType} is created with a fixed set of blocks it may attach to.
 * Their {@code BALLOON_BE} was registered listing only {@code red_balloon}, so even though
 * our blocks inherit {@code newBlockEntity} from {@code RedBalloonBlock}, vanilla's
 * {@code isValid} check would reject them and the block entity would be dropped on load.
 */
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeAccessor {

    @Accessor("validBlocks")
    Set<Block> coasters_extras$getValidBlocks();

    @Accessor("validBlocks")
    void coasters_extras$setValidBlocks(Set<Block> blocks);
}
