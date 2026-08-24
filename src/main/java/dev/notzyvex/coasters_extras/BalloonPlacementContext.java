package dev.notzyvex.coasters_extras;

import net.minecraft.world.level.block.Block;

/**
 * Carries "which balloon is the player actually holding" from the interaction event into
 * the base mod's spawner.
 *
 * <p>{@code BalloonSpawner.tryAttach(ServerLevel, BlockPos)} takes no item argument and
 * hardcodes {@code RED_BALLOON.get().defaultBlockState()}, so without this every balloon
 * we place would appear red regardless of the item used.
 *
 * <p>A thread local is safe here: the capture and the swap both happen inline on the
 * server thread within a single right-click, and it is always cleared in a finally block.
 */
public final class BalloonPlacementContext {

    private static final ThreadLocal<Block> HELD = new ThreadLocal<>();

    public static void set(Block block) {
        HELD.set(block);
    }

    public static Block get() {
        return HELD.get();
    }

    public static void clear() {
        HELD.remove();
    }

    private BalloonPlacementContext() {}
}
