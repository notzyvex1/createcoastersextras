package dev.notzyvex.coasters_extras.track;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackMaterialFactory;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.resources.ResourceLocation;

/**
 * Our custom coaster track materials, registered through Create's public addon API.
 *
 * <p>This mirrors how Create: Coasters Simulated builds its own {@code coaster_track}
 * material, which in turn is the same pattern Create: Steam 'n' Rails uses for its ~20
 * variants -- so this is a supported extension point, not a hack.
 *
 * <p>{@code standardModels()} makes Create look for models under
 * {@code <namespace>:block/track/<path>/...}, which is exactly where our copied OBJ
 * meshes live ({@code coasters_extras:block/track/boost_track/}).
 *
 * <p>NOTE: this registers the track's <em>appearance and placement</em> only. Making a
 * cart actually accelerate on it is separate work -- see PROJECT.md.
 */
public final class ModTrackMaterials {

    public static final TrackMaterial BOOST = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "boost_track"))
            .lang("Boost Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::boostBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();

    /**
     * Sensor track -- yellow hazard rails. Emits a redstone signal when a coaster passes
     * over it. Registered here; the detection itself shares the cart-finding machinery
     * with the boost track and is not wired yet.
     */
    public static final TrackMaterial SENSOR = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "sensor_track"))
            .lang("Sensor Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::sensorBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();

    /**
     * Brake track -- hazard bars. Slows a coaster to a configurable target speed, and
     * throws sparks when something arrives far too fast. Registered here; the braking
     * itself shares cart detection with the boost track and is not wired yet.
     */
    public static final TrackMaterial BRAKE = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "brake_track"))
            .lang("Brake Coaster Track")
            .block(NonNullSupplier.lazy(() -> ModTrackMaterials::brakeBlock))
            .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
            .standardModels()
            .noRecipeGen()
            .build();


    /**
     * Station track -- catches a cart, holds it, then sends it on its way.
     *
     * <p>Completes the circuit: without it every ride needs a manual shove to start. The
     * anchorpoint dial sets the dwell in seconds, and a redstone signal on the anchor holds
     * the cart indefinitely so a station can be gated.
     */
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

    /** Slippery track -- near-frictionless, a cart keeps its momentum across it. */
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

    /**
     * Bobsled track -- the cart's rail guide is relaxed on this material so it slides across
     * the channel and banks up the walls instead of running rigidly on the rail. The physics
     * swap lives in {@code CoasterGuideBobsledMixin}, which recognises a curve by this id.
     */
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

    /**
     * Splash track -- water rails. As a cart crosses, a big splash bursts off BOTH sides of
     * the track with a splash sound, and the water gives a little drag. Registered here; the
     * effect shares cart detection with the boost track (see CoasterCartDriveMixin).
     */
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

    /**
     * Launch track -- the hydraulic/LSM launch. From a standstill it kicks a cart HARD up to a
     * high target speed, far stronger than a boost, then lets go. The staple of every modern
     * coaster. Shares cart detection with the boost track (see CoasterCartDriveMixin).
     */
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

    /**
     * Reverse track -- flips a cart's travel direction as it crosses, once per pass. Turns any
     * dead-end into a shuttle and lets you build boomerang layouts. Shares cart detection with
     * the boost track (see CoasterCartDriveMixin).
     */
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

    /**
     * Powered Boost track -- a boost that only pushes while its anchorpoint has redstone.
     *
     * <p>A plain Boost is always on, which makes it scenery you build around rather than a
     * machine you control. Gating it on a signal is what turns a coaster into something a
     * redstone circuit can drive: launch on a button, dispatch on a timer, hold a section shut
     * until a door opens.
     */
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

    /** Forces class-init so the material is built before Create reads the registry. */
    public static void init() {}

    private ModTrackMaterials() {}
}
