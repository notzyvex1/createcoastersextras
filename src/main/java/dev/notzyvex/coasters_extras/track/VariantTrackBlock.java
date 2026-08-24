package dev.notzyvex.coasters_extras.track;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Placed-track block for a cosmetic {@link TrackVariant}. Purely visual. */
public class VariantTrackBlock extends TrackBlock {
    public VariantTrackBlock(BlockBehaviour.Properties properties, TrackMaterial material) {
        super(properties, material);
    }
}
