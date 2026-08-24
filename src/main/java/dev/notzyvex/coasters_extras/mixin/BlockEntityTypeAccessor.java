package dev.notzyvex.coasters_extras.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(BlockEntityType.class)
public interface BlockEntityTypeAccessor {

    @Accessor("validBlocks")
    Set<Block> coasters_extras$getValidBlocks();

    @Accessor("validBlocks")
    void coasters_extras$setValidBlocks(Set<Block> blocks);
}
