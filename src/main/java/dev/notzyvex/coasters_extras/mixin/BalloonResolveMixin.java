package dev.notzyvex.coasters_extras.mixin;

import dev.notzyvex.coasters_extras.ModTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes Create: Coasters Simulated recognise <em>our</em> balloons as balloons.
 *
 * <p>Every balloon check in the base mod bottoms out in {@code state.is(RED_BALLOON.get())} --
 * an identity check against their one block, not an {@code instanceof}. Verified by
 * disassembly:
 * <pre>
 *   isBalloonBlockState(state) -> state.is(RED_BALLOON.get())
 *   isBalloonSubLevel(sub)     -> sub.getPlot()...getBlockState(ZERO).is(RED_BALLOON.get())
 * </pre>
 * Since {@code findBalloonAt}, {@code targetsBalloonCell} and {@code findBalloonAtImpact}
 * all route through those two, redirecting the {@code BlockState.is} call inside this class
 * widens the entire tether / pop-cascade / attach system in one place.
 *
 * <p>We match on a block tag rather than a hard-coded list so future balloons (and other
 * addons) work without touching this mixin.
 *
 * <p>Targeted by string so we do not need the base mod on the compile classpath, and the
 * handler uses only vanilla types so we do not need Sable either.
 */
@Mixin(targets = "dev.silvergold.simulatedcoasters.balloon.BalloonResolve", remap = false)
public class BalloonResolveMixin {

    @Redirect(
            // Only the two predicates below call BlockState.is directly; the other methods
            // (findBalloonAt, targetsBalloonCell, findBalloonAtImpact) delegate to
            // isBalloonSubLevel, so widening these two covers the whole system.
            // Named explicitly rather than "*" so we never redirect a call site we have
            // not actually inspected.
            method = { "isBalloonBlockState", "isBalloonSubLevel" },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            ),
            require = 0   // tolerate the base mod refactoring these call sites
    )
    private static boolean coasters_extras$anyBalloonCounts(BlockState state, Block block) {
        // Preserve their original answer first, then widen to our balloons.
        return state.is(block) || state.is(ModTags.Blocks.BALLOONS);
    }
}
