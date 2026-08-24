package dev.notzyvex.coasters_extras.mixin;

import dev.notzyvex.coasters_extras.BalloonPlacementContext;
import dev.silvergold.simulatedcoasters.balloon.RedBalloonBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.silvergold.simulatedcoasters.balloon.BalloonSpawner", remap = false)
public class BalloonSpawnerMixin {

    @Redirect(
            method = "tryAttach",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/silvergold/simulatedcoasters/balloon/RedBalloonBlock;"
                           + "defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"
            )
    )
    private static BlockState coasters_extras$useHeldBalloon(RedBalloonBlock original) {
        Block held = BalloonPlacementContext.get();
        return held != null ? held.defaultBlockState() : original.defaultBlockState();
    }
}
