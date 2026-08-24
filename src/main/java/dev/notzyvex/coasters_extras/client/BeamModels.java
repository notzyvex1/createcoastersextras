package dev.notzyvex.coasters_extras.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.notzyvex.coasters_extras.CoastersExtras;
import dev.notzyvex.coasters_extras.track.TrackVariant;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = CoastersExtras.MOD_ID, value = Dist.CLIENT,
                    bus = EventBusSubscriber.Bus.MOD)
public final class BeamModels {

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

    public static PartialModel forMaterial(String namespace, String path) {
        if (!CoastersExtras.MOD_ID.equals(namespace)) {
            return null;
        }
        return BEAMS.get(path);
    }

    private BeamModels() {}
}
