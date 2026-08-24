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

/**
 * Client-only setup. Currently just declares the creative tab's sections.
 *
 * <p>Runs at client setup rather than static init because the item registry has to be
 * populated before stacks can be built from it.
 */
@EventBusSubscriber(modid = CoastersExtras.MOD_ID, value = Dist.CLIENT,
                    bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    /**
     * A banner texture path for {@code GuiGraphics.blit}.
     *
     * <p>Spelled out in full -- {@code textures/...png} -- because blit hands the location
     * straight to the texture manager, which treats it as a literal file path. Only the model
     * and sprite loaders add the {@code textures/} prefix and the {@code .png} suffix, and a
     * location written in that shorter style renders as the missing-texture chequer.
     */
    private static ResourceLocation banner(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                CoastersExtras.MOD_ID, "textures/gui/section/" + name + ".png");
    }

    /**
     * The balloons banner is a SPRITE banner, so vanilla animates it from
     * {@code textures/gui/sprites/section/banner_balloons.png.mcmeta} and the frame count comes
     * from the image rather than from a number repeated here. Its sky runs morning to night, so
     * it should keep moving rather than only animating under the cursor.
     */
    private static final TabSections.Banner BANNER_BALLOONS =
            TabSections.Banner.sprite(ResourceLocation.fromNamespaceAndPath(
                    CoastersExtras.MOD_ID, "section/banner_balloons"));
    /**
     * The Tracks banner is a SCROLLING banner: its texture is one 8036px strip holding every
     * track material in the mod, 28px each, and it slides rightward forever. Sorted greys
     * first then by hue, so the loop reads as one journey through the whole set rather than as
     * a shuffle. At 20px/s a new material arrives about every second and a half, and the full
     * cycle takes around six and a half minutes -- long enough that it never looks like a loop.
     */
    private static final TabSections.Banner BANNER_TRACKS =
            TabSections.Banner.scroll(banner("banner_tracks"), 20f);
    /** Create's creative casing -- the perforated plate. Marks the tracks that DO something. */
    private static final TabSections.Banner BANNER_FUNCTIONAL =
            TabSections.Banner.autoFrames(banner("creative_casing_banner"), 3);
    /**
     * Every Create casing in turn -- andesite, brass, copper, railway, shadow steel, refined
     * radiance, creative -- each dissolving pixel by pixel into the next, so Controls reads as
     * machinery rather than as more track. Fifty-six frames at two ticks each is about five and
     * a half seconds for the full cycle, slow enough to look like the material is turning over
     * rather than flickering. At rest it is plain andesite; the cycle only runs on hover.
     */
    private static final TabSections.Banner BANNER_CONTROLS =
            TabSections.Banner.autoFrames(banner("casing_banner"), 2);

    /** The base mod's red balloon, so the colour set reads as complete. */
    private static final ResourceLocation SIMULATED_RED_BALLOON =
            ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "red_balloon");

    /**
     * Balloon order in the tab, straight off the reference sheet: warm through the spectrum to
     * cold, then brown, then white down to black.
     *
     * <p>Deliberately not {@code BalloonColor.values()}. That is Minecraft's dye order, which
     * puts magenta second and light blue fourth and is not sorted by anything a person can see;
     * a wall of balloons is picked from by eye, so it wants a spectrum.
     *
     * <p>Red is absent because it belongs to the base mod and is prepended separately, and
     * rainbow is absent because it is appended last -- it is not a dye colour and does not
     * belong anywhere in the sweep.
     */
    private static final BalloonColor[] BALLOON_ORDER = {
            BalloonColor.ORANGE, BalloonColor.YELLOW, BalloonColor.LIME, BalloonColor.GREEN,
            BalloonColor.CYAN, BalloonColor.LIGHT_BLUE, BalloonColor.BLUE, BalloonColor.PURPLE,
            BalloonColor.MAGENTA, BalloonColor.PINK, BalloonColor.BROWN,
            BalloonColor.WHITE, BalloonColor.LIGHT_GRAY, BalloonColor.GRAY, BalloonColor.BLACK,
    };


    /** Bake the handle-only model even though no block state points at it; the renderer does. */
    @SubscribeEvent
    static void onRegisterAdditionalModels(
            net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(CoasterControlsRenderer.HANDLE);
    }

    /**
     * Teach the particle engine how to draw our splash.
     *
     * <p>The type itself is registered on the common side; this only supplies the renderer.
     * {@code registerSpriteSet} is the overload to use because
     * {@code assets/coasters_extras/particles/splash.json} exists and lists eight frames --
     * the engine builds a sprite set from that file and passes it to the provider.
     * {@code registerSpecial} is the wrong overload here -- it is for types with no json, and
     * using it would leave the frames unread and the sprite unset.
     */
    @SubscribeEvent
    static void onRegisterParticleProviders(
            net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                dev.notzyvex.coasters_extras.particle.ModParticles.SPLASH.get(),
                SplashParticle.Provider::new);
    }

    /** Hook the animated-handle renderer to the controls block entity. */
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
            // Ponder collects its plugins at load-complete, which is strictly after this,
            // so registering here lands inside the window.
            net.createmod.ponder.foundation.PonderIndex.addPlugin(
                    new dev.notzyvex.coasters_extras.ponder.CoastersExtrasPonderPlugin());
        });
    }

    private static void defineTabSections() {
        // The sixteen dye colours and nothing else. Red comes from the base mod, which already
        // registers it -- ours would be a duplicate item with a duplicate recipe.
        //
        List<ItemStack> balloons = new ArrayList<>();
        Item red = BuiltInRegistries.ITEM.getOptional(SIMULATED_RED_BALLOON).orElse(null);
        if (red != null) {
            balloons.add(new ItemStack(red));
        }
        for (BalloonColor color : BALLOON_ORDER) {
            balloons.add(new ItemStack(ModBlocks.BALLOON_ITEMS.get(color).get()));
        }
        balloons.add(new ItemStack(ModBlocks.BALLOON_ITEMS.get(BalloonColor.RAINBOW).get()));

        // Every cosmetic variant, block-based ones included.
        //
        // They used to be hidden on the grounds that they were Copycat Track plumbing rather
        // than tracks in their own right, reachable only through the wand. That was the wrong
        // way round: the live copycat is not happening, so these ARE the tracks -- hiding two
        // hundred real materials behind one item is hiding the mod's biggest feature.
        //
        // Enum order does the sorting for free: the hand-made ones are declared first, so they
        // still lead the section and the block-based ones follow.
        List<ItemStack> materials = new ArrayList<>();
        for (TrackVariant v : TrackVariant.values()) {
            materials.add(new ItemStack(ModTrackVariants.ITEMS.get(v).get()));
        }

        // Functional tracks: the ones that DO something to a cart. Split out from the materials
        // because they are chosen for behaviour, not appearance -- someone laying a brake needs to
        // find it without scrolling past forty colours of stone.
        //
        // Rainbow, Rose Quartz and Brass are materials by that rule, but they sit here too: they
        // are the three hand-drawn ones and burying them among the generated variants is how
        // nobody ever finds them.
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

        // Order matters and is deliberate: Balloons, then the plain track materials, then the
        // functional ones, then Controls last. Materials before functional because that is the
        // order you build in -- you lay track first and decide what it does second -- and
        // Controls last because it is the smallest group and belongs at the bottom rather than
        // splitting the two track sections apart.
        // Functional before Tracks: the functional set is five items and the materials set is
        // 280, so putting materials second means you scroll past a wall of stone to reach the
        // brakes. The short, behaviour-carrying list goes first.
        // Section order, set by the dev: Balloons, Controls, Functional Tracks, Tracks.
        TabSections.define(Component.literal("Balloons"), BANNER_BALLOONS, balloons, true);
        TabSections.define(Component.literal("Controls"), BANNER_CONTROLS, controls, false);
        // Purple, not gold: this is the section that is entirely ours, so it should
        // not read as another row of Create.
        TabSections.define(Component.literal("Functional Tracks"),
                BANNER_FUNCTIONAL, functional, false, TabSections.TITLE_PURPLE);
        TabSections.define(Component.literal("Tracks"),   BANNER_TRACKS,   materials, false);
    }

    private ClientSetup() {}
}
