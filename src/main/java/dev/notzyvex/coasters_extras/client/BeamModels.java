package dev.notzyvex.coasters_extras.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.notzyvex.coasters_extras.CoastersExtras;
import dev.notzyvex.coasters_extras.track.ModTracks;
import dev.notzyvex.coasters_extras.track.TrackVariant;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-material centre beam models.
 *
 * <p>The base mod draws the beam from a single hardcoded static:
 *
 * <pre>
 *   PartialModel beamPartial = beamDyed ? ..._DYED : CoasterTrackCenterBeamModel.CENTER_BEAM_SEGMENT;
 * </pre>
 *
 * where {@code CENTER_BEAM_SEGMENT} always points at
 * {@code simulatedcoasters:block/track/coaster_track/segment_center_beam}. The ties and rails
 * beside it come from {@code bc.getMaterial().getModelHolder()} and so follow the curve's
 * material correctly -- only the beam does not. That is why every one of our tracks had a
 * grey strip running down the middle of an otherwise coloured track.
 *
 * <p>Our model folders were copied whole from theirs, so a {@code segment_center_beam} already
 * exists for every material and points at that material's texture. It simply was never used.
 * These are registered so Flywheel bakes them, and the mixin picks the right one per curve.
 */
@EventBusSubscriber(modid = CoastersExtras.MOD_ID, value = Dist.CLIENT,
                    bus = EventBusSubscriber.Bus.MOD)
public final class BeamModels {

    /** Keyed by the material's registry path, e.g. {@code oak_track}. */
    private static final Map<String, PartialModel> BEAMS = new HashMap<>();

    static {
        for (TrackVariant v : TrackVariant.values()) {
            put(v.trackName());
        }
        put("boost_track");
        put("brake_track");
        put("sensor_track");
        put("station_track");
        put("slippery_track");
    }

    private static void put(String track) {
        BEAMS.put(track, PartialModel.of(ResourceLocation.fromNamespaceAndPath(
                CoastersExtras.MOD_ID, "block/track/" + track + "/segment_center_beam")));
    }

    @SubscribeEvent
    static void registerAdditional(ModelEvent.RegisterAdditional event) {
        for (PartialModel m : BEAMS.values()) {
            event.register(ModelResourceLocation.standalone(m.modelLocation()));
        }
    }

    /**
     * The beam for this material, or null to leave the base mod's choice alone.
     *
     * <p>Takes the id as a string rather than a {@code TrackMaterial} so the calling mixin
     * does not have to resolve any type of ours beyond this class.
     */
    public static PartialModel forMaterial(String namespace, String path) {
        if (!CoastersExtras.MOD_ID.equals(namespace)) {
            return null;
        }
        return BEAMS.get(path);
    }

    private BeamModels() {}
}
