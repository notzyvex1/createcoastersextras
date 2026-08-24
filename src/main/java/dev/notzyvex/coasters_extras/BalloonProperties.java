package dev.notzyvex.coasters_extras;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class BalloonProperties {

    public static BlockBehaviour.Properties create() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(0.1F)
                .sound(SoundType.WOOL)
                .noOcclusion()
                .isSuffocating((s, l, p) -> false)
                .isViewBlocking((s, l, p) -> false)
                .pushReaction(PushReaction.DESTROY);
    }

    private BalloonProperties() {}
}
