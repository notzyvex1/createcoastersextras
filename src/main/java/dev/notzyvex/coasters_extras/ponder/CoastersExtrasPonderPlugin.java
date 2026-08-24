package dev.notzyvex.coasters_extras.ponder;

import dev.notzyvex.coasters_extras.CoastersExtras;
import dev.notzyvex.coasters_extras.track.ModTrackVariants;
import dev.notzyvex.coasters_extras.track.ModTracks;
import dev.notzyvex.coasters_extras.track.TrackVariant;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers a Ponder scene for each track that does something.
 *
 * <p>Client only. Everything this reaches is client rendering code, so nothing outside
 * {@code client.ClientSetup} may reference this class.
 *
 * <p>Scenes are keyed by ITEM id, because that is what Ponder looks up when W is pressed over
 * a stack. Our {@code *_track_material} blocks have no block item, so registering the block id
 * would produce a scene nobody could ever open.
 *
 * <p>Deliberately implements the interface rather than extending Create's
 * {@code CreatePonderPlugin}: that class's shared-text registration is namespaced by the
 * calling mod, so inheriting it would mint a copy of Create's entire shared-text table under
 * our own id. The scenes still get Create's level-restore handling, because Ponder runs the
 * restore hook for every registered plugin rather than only the one that owns the scene --
 * which is also why the coaster curves in these structures come back correctly on rewind.
 */
public class CoastersExtrasPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return CoastersExtras.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(ModTracks.BOOST_TRACK.getId(),    "track/boost",    TrackScenes::boost);
        helper.addStoryBoard(ModTracks.BRAKE_TRACK.getId(),    "track/brake",    TrackScenes::brake);
        helper.addStoryBoard(ModTracks.STATION_TRACK.getId(),  "track/station",  TrackScenes::station);
        helper.addStoryBoard(ModTracks.SENSOR_TRACK.getId(),   "track/sensor",   TrackScenes::sensor);
        helper.addStoryBoard(ModTracks.SLIPPERY_TRACK.getId(), "track/slippery", TrackScenes::slippery);
        helper.addStoryBoard(ModTracks.LAUNCH_TRACK.getId(),   "track/launch",   TrackScenes::launch);
        helper.addStoryBoard(ModTracks.SPLASH_TRACK.getId(),   "track/splash",   TrackScenes::splash);
        helper.addStoryBoard(ModTracks.REVERSE_TRACK.getId(),  "track/reverse",  TrackScenes::reverse);
        helper.addStoryBoard(ModTracks.BOBSLED_TRACK.getId(),  "track/bobsled",  TrackScenes::bobsled);

        // The Powered dial is a setting rather than a block, so its scene is attached to each
        // of the three tracks that carry it. Ponder pages through them.
        helper.addStoryBoard(ModTracks.BOOST_TRACK.getId(),  "track/powered", TrackScenes::powered);
        helper.addStoryBoard(ModTracks.BRAKE_TRACK.getId(),  "track/powered", TrackScenes::powered);
        helper.addStoryBoard(ModTracks.LAUNCH_TRACK.getId(), "track/powered", TrackScenes::powered);

        // Powered Boost had no scene at all. It gets the boost scene and the powered one --
        // but BOTH must load track/powered, whose structure contains powered boost track.
        // Pointing it at track/boost showed orange boost rails while the text talked about
        // the powered one.
        helper.addStoryBoard(ModTracks.POWERED_BOOST_TRACK.getId(),
                             "track/powered", TrackScenes::boost);
        helper.addStoryBoard(ModTracks.POWERED_BOOST_TRACK.getId(),
                             "track/powered", TrackScenes::powered);
        helper.addStoryBoard(ModTrackVariants.ITEMS.get(TrackVariant.RAINBOW).getId(),
                             "track/rainbow", TrackScenes::rainbow);
    }
}
