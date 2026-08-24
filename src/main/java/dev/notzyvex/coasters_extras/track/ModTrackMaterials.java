package dev.notzyvex.coasters_extras.track;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackMaterialFactory;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.resources.ResourceLocation;

public final class ModTrackMaterials {

    public static final TrackMaterial BOOST = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "boost_track"))
            .lang("Boost Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::boostBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();

    public static final TrackMaterial SENSOR = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "sensor_track"))
            .lang("Sensor Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::sensorBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();

    public static final TrackMaterial BRAKE = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "brake_track"))
            .lang("Brake Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::brakeBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();

    public static final TrackMaterial STATION = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "station_track"))
            .lang("Station Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::stationBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();

    private static TrackBlock stationBlock() {
        return ModTracks.STATION_TRACK_MATERIAL.get();
    }

    public static final TrackMaterial SLIPPERY = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "slippery_track"))
            .lang("Slippery Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::slipperyBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();

    private static TrackBlock slipperyBlock() {
        return ModTracks.SLIPPERY_TRACK_MATERIAL.get();
    }

    public static final TrackMaterial BOBSLED = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "bobsled_track"))
            .lang("Bobsled Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::bobsledBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();

    private static TrackBlock bobsledBlock() {
        return ModTracks.BOBSLED_TRACK_MATERIAL.get();
    }

    public static final TrackMaterial SPLASH = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "splash_track"))
            .lang("Splash Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::splashBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();

    private static TrackBlock splashBlock() {
        return ModTracks.SPLASH_TRACK_MATERIAL.get();
    }

    public static final TrackMaterial LAUNCH = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "launch_track"))
            .lang("Launch Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::launchBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();

    private static TrackBlock launchBlock() {
        return ModTracks.LAUNCH_TRACK_MATERIAL.get();
    }

    public static final TrackMaterial REVERSE = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "reverse_track"))
            .lang("Reverse Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::reverseBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();

    private static TrackBlock reverseBlock() {
        return ModTracks.REVERSE_TRACK_MATERIAL.get();
    }

    private static TrackBlock brakeBlock() {
        return ModTracks.BRAKE_TRACK_MATERIAL.get();
    }

    private static TrackBlock sensorBlock() {
        return ModTracks.SENSOR_TRACK_MATERIAL.get();
    }

    private static TrackBlock boostBlock() {
        return ModTracks.BOOST_TRACK_MATERIAL.get();
    }

    public static final TrackMaterial POWERED_BOOST = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID,
                    "powered_boost_track"))
            .lang("Powered Boost Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::poweredBoostBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();

    private static TrackBlock poweredBoostBlock() {
        return ModTracks.POWERED_BOOST_TRACK_MATERIAL.get();
    }

    public static void init() {}

    private ModTrackMaterials() {}
}
