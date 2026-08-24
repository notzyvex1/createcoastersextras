package dev.notzyvex.coasters_extras.client;

import dev.notzyvex.coasters_extras.BalloonColor;
import dev.notzyvex.coasters_extras.CoastersExtras;
import dev.notzyvex.coasters_extras.ModBlocks;
import dev.notzyvex.coasters_extras.track.ModTrackVariants;
import dev.notzyvex.coasters_extras.track.ModTracks;
import dev.notzyvex.coasters_extras.track.TrackVariant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = CoastersExtras.MOD_ID, value = Dist.CLIENT,
                    bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    private static ResourceLocation banner(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                CoastersExtras.MOD_ID, "textures/gui/section/" + name + ".png");
    }

    private static final TabSections.Banner BANNER_BALLOONS =
            TabSections.Banner.sprite(ResourceLocation.fromNamespaceAndPath(
                    CoastersExtras.MOD_ID, "section/banner_balloons"));
    private static final TabSections.Banner BANNER_TRACKS =
            TabSections.Banner.scroll(banner("banner_tracks"), 20f);
    private static final TabSections.Banner BANNER_FUNCTIONAL =
            TabSections.Banner.autoFrames(banner("creative_casing_banner"), 3);
    private static final TabSections.Banner BANNER_CONTROLS =
            TabSections.Banner.autoFrames(banner("casing_banner"), 2);

    private static final ResourceLocation SIMULATED_RED_BALLOON =
            ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "red_balloon");

    private static final BalloonColor[] BALLOON_ORDER = {
            BalloonColor.ORANGE, BalloonColor.YELLOW, BalloonColor.LIME, BalloonColor.GREEN,
            BalloonColor.CYAN, BalloonColor.LIGHT_BLUE, BalloonColor.BLUE, BalloonColor.PURPLE,
            BalloonColor.MAGENTA, BalloonColor.PINK, BalloonColor.BROWN,
            BalloonColor.WHITE, BalloonColor.LIGHT_GRAY, BalloonColor.GRAY, BalloonColor.BLACK,
    };

    @SubscribeEvent
    static void onRegisterAdditionalModels(
            net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(CoasterControlsRenderer.HANDLE);
    }

    @SubscribeEvent
    static void onRegisterParticleProviders(
            net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                dev.notzyvex.coasters_extras.particle.ModParticles.SPLASH.get(),
                SplashParticle.Provider::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(
            net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                dev.notzyvex.coasters_extras.track.ModTracks.COASTER_CONTROLS_BE.get(),
                CoasterControlsRenderer::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            defineTabSections();
            net.createmod.ponder.foundation.PonderIndex.addPlugin(
                    new dev.notzyvex.coasters_extras.ponder.CoastersExtrasPonderPlugin());
        });
    }

    private static void defineTabSections() {
        List<ItemStack> balloons = new ArrayList<>();
        Item red = BuiltInRegistries.ITEM.getOptional(SIMULATED_RED_BALLOON).orElse(null);
        if (red != null) {
            balloons.add(new ItemStack(red));
        }
        for (BalloonColor color : BALLOON_ORDER) {
            balloons.add(new ItemStack(ModBlocks.BALLOON_ITEMS.get(color).get()));
        }
        balloons.add(new ItemStack(ModBlocks.BALLOON_ITEMS.get(BalloonColor.RAINBOW).get()));

        List<ItemStack> materials = new ArrayList<>();
        for (TrackVariant v : TrackVariant.values()) {
            materials.add(new ItemStack(ModTrackVariants.ITEMS.get(v).get()));
        }

        List<ItemStack> functional = new ArrayList<>();
        functional.add(new ItemStack(ModTracks.BOOST_TRACK.get()));
        functional.add(new ItemStack(ModTracks.POWERED_BOOST_TRACK.get()));
        functional.add(new ItemStack(ModTracks.BRAKE_TRACK.get()));
        functional.add(new ItemStack(ModTracks.SENSOR_TRACK.get()));
        functional.add(new ItemStack(ModTracks.STATION_TRACK.get()));
        functional.add(new ItemStack(ModTracks.SLIPPERY_TRACK.get()));
        functional.add(new ItemStack(ModTracks.BOBSLED_TRACK.get()));
        functional.add(new ItemStack(ModTracks.SPLASH_TRACK.get()));
        functional.add(new ItemStack(ModTracks.LAUNCH_TRACK.get()));
        functional.add(new ItemStack(ModTracks.REVERSE_TRACK.get()));
        for (TrackVariant v : new TrackVariant[]{
                TrackVariant.RAINBOW, TrackVariant.ROSE_QUARTZ, TrackVariant.BRASS}) {
            functional.add(new ItemStack(ModTrackVariants.ITEMS.get(v).get()));
        }

        List<ItemStack> controls = new ArrayList<>();
        controls.add(new ItemStack(ModTracks.COASTER_CONTROLS.get()));
        controls.add(new ItemStack(
                dev.notzyvex.coasters_extras.sensor.SensorRegistry.SENSOR_BLOCK_ITEM.get()));

        TabSections.define(Component.literal("Balloons"), BANNER_BALLOONS, balloons, true);
        TabSections.define(Component.literal("Controls"), BANNER_CONTROLS, controls, false);
        TabSections.define(Component.literal("Functional Tracks"),
                BANNER_FUNCTIONAL, functional, false, TabSections.TITLE_PURPLE);
        TabSections.define(Component.literal("Tracks"),   BANNER_TRACKS,   materials, false);
    }

    private ClientSetup() {}
}
