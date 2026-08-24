package dev.notzyvex.coasters_extras.ponder;

import dev.notzyvex.coasters_extras.CoastersExtras;
import dev.notzyvex.coasters_extras.track.ModTrackVariants;
import dev.notzyvex.coasters_extras.track.ModTracks;
import dev.notzyvex.coasters_extras.track.TrackVariant;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

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

        helper.addStoryBoard(ModTracks.BOOST_TRACK.getId(),  "track/powered", TrackScenes::powered);
        helper.addStoryBoard(ModTracks.BRAKE_TRACK.getId(),  "track/powered", TrackScenes::powered);
        helper.addStoryBoard(ModTracks.LAUNCH_TRACK.getId(), "track/powered", TrackScenes::powered);

        helper.addStoryBoard(ModTracks.POWERED_BOOST_TRACK.getId(),
                             "track/powered", TrackScenes::boost);
        helper.addStoryBoard(ModTracks.POWERED_BOOST_TRACK.getId(),
                             "track/powered", TrackScenes::powered);
        helper.addStoryBoard(ModTrackVariants.ITEMS.get(TrackVariant.RAINBOW).getId(),
                             "track/rainbow", TrackScenes::rainbow);
    }
}
