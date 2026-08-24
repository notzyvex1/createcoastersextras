package dev.notzyvex.coasters_extras.display;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import dev.notzyvex.coasters_extras.CoastersExtras;
import dev.notzyvex.coasters_extras.sensor.SensorRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDisplaySources {

    public static final DeferredRegister<DisplaySource> DISPLAY_SOURCES =
            DeferredRegister.create(CreateRegistries.DISPLAY_SOURCE, CoastersExtras.MOD_ID);

    public static final DeferredHolder<DisplaySource, RideDisplaySource> SPEED =
            DISPLAY_SOURCES.register("coaster_speed",
                    () -> new RideDisplaySource(RideDisplaySource.Readout.SPEED));

    public static final DeferredHolder<DisplaySource, RideDisplaySource> TOP_SPEED =
            DISPLAY_SOURCES.register("coaster_top_speed",
                    () -> new RideDisplaySource(RideDisplaySource.Readout.TOP_SPEED));

    public static final DeferredHolder<DisplaySource, RideDisplaySource> CARTS =
            DISPLAY_SOURCES.register("coaster_carts",
                    () -> new RideDisplaySource(RideDisplaySource.Readout.CARTS));

    public static final DeferredHolder<DisplaySource, RideDisplaySource> STATUS =
            DISPLAY_SOURCES.register("coaster_status",
                    () -> new RideDisplaySource(RideDisplaySource.Readout.STATUS));

    public static final DeferredHolder<DisplaySource, RideDisplaySource> TIMER =
            DISPLAY_SOURCES.register("coaster_timer",
                    () -> new RideDisplaySource(RideDisplaySource.Readout.TIMER));

    public static void register(IEventBus modBus) {
        DISPLAY_SOURCES.register(modBus);
    }

    public static void bind() {
        var sensor = SensorRegistry.SENSOR_BLOCK.get();
        attach(sensor, "sensor", SPEED, TOP_SPEED, CARTS, STATUS, TIMER);

        try {
            var anchorpoint = dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks
                    .COASTER_ANCHORPOINT.get();
            if (anchorpoint == null) {
                CoastersExtras.LOGGER.warn(
                        "[display] anchorpoint block resolved to null -- display links on a "
                        + "station will say 'not a display source'. Base mod version changed?");
                return;
            }
            attach(anchorpoint, "anchorpoint (" + net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(anchorpoint) + ")", TIMER, SPEED, TOP_SPEED, CARTS, STATUS);
        } catch (Throwable t) {
            CoastersExtras.LOGGER.error(
                    "[display] could not attach sources to the coaster anchorpoint; the Sensor "
                    + "Block route still works. Cause:", t);
        }
    }

    @SafeVarargs
    private static void attach(net.minecraft.world.level.block.Block block, String label,
                               DeferredHolder<DisplaySource, RideDisplaySource>... sources) {
        if (block == null) {
            CoastersExtras.LOGGER.warn("[display] {} block is null -- skipped", label);
            return;
        }
        int n = 0;
        for (var holder : sources) {
            DisplaySource.BY_BLOCK.add(block, holder.get());
            n++;
        }
        CoastersExtras.LOGGER.info("[display] attached {} readouts to {}", n, label);
    }

    private ModDisplaySources() {}
}
