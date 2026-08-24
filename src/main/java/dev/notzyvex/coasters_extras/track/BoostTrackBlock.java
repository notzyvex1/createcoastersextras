package dev.notzyvex.coasters_extras.track;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The block backing our boost track material.
 *
 * <p>Create: Coasters Simulated's own {@code CoasterTrackMaterialBlock} is {@code final},
 * so we cannot extend it. It is only a thin wrapper over Create's {@link TrackBlock} with a
 * {@code (Properties, TrackMaterial)} constructor, so we mirror that shape directly instead.
 */
public class BoostTrackBlock extends TrackBlock {

    public BoostTrackBlock(BlockBehaviour.Properties properties, TrackMaterial material) {
        super(properties, material);
    }
}
