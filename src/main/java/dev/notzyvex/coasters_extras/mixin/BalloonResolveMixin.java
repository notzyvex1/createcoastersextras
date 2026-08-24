package dev.notzyvex.coasters_extras.mixin;

import dev.notzyvex.coasters_extras.ModTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.silvergold.simulatedcoasters.balloon.BalloonResolve", remap = false)
public class BalloonResolveMixin {

    @Redirect(
            method = { "isBalloonBlockState", "isBalloonSubLevel" },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            ),
            require = 0
    )
    private static boolean coasters_extras$anyBalloonCounts(BlockState state, Block block) {
        return state.is(block) || state.is(ModTags.Blocks.BALLOONS);
    }
}
