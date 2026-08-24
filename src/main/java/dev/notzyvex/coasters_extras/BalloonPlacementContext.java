package dev.notzyvex.coasters_extras;

import net.minecraft.world.level.block.Block;

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
