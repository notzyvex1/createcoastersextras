package dev.notzyvex.coasters_extras;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CoastersExtras.MOD_ID);

    private static final ResourceLocation SIMULATED_RED_BALLOON =
            ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "red_balloon");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + CoastersExtras.MOD_ID))
                    .icon(() -> new ItemStack(
                            ModBlocks.BALLOON_ITEMS.get(BalloonColor.RAINBOW).get()))
                    .displayItems((params, output) -> {
                        Item red = net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getOptional(SIMULATED_RED_BALLOON).orElse(null);
                        if (red != null) {
                            output.accept(new ItemStack(red));
                        } else {
                            CoastersExtras.LOGGER.warn(
                                    "Could not find {} for the creative tab; skipping it. "
                                  + "Has Create: Coasters Simulated changed its registry names?",
                                    SIMULATED_RED_BALLOON);
                        }
                        for (BalloonColor color : BalloonColor.values()) {
                            output.accept(ModBlocks.BALLOON_ITEMS.get(color).get());
                        }
                        for (dev.notzyvex.coasters_extras.track.TrackVariant v
                                : dev.notzyvex.coasters_extras.track.TrackVariant.values()) {
                            output.accept(dev.notzyvex.coasters_extras.track.ModTrackVariants
                                    .ITEMS.get(v).get());
                        }
                        output.accept(dev.notzyvex.coasters_extras.track.ModTracks.BOOST_TRACK.get());
                        output.accept(dev.notzyvex.coasters_extras.track.ModTracks.BRAKE_TRACK.get());
                        output.accept(dev.notzyvex.coasters_extras.track.ModTracks.SENSOR_TRACK.get());
                        output.accept(dev.notzyvex.coasters_extras.track.ModTracks.STATION_TRACK.get());
        output.accept(dev.notzyvex.coasters_extras.track.ModTracks.BOBSLED_TRACK.get());
                        output.accept(dev.notzyvex.coasters_extras.track.ModTracks.SLIPPERY_TRACK.get());
                        output.accept(dev.notzyvex.coasters_extras.track.ModTracks.COASTER_CONTROLS.get());
                        output.accept(dev.notzyvex.coasters_extras.sensor.SensorRegistry.SENSOR_BLOCK_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private ModCreativeTabs() {}
}
