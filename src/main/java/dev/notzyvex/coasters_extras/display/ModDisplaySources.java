package dev.notzyvex.coasters_extras.display;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import dev.notzyvex.coasters_extras.CoastersExtras;
import dev.notzyvex.coasters_extras.sensor.SensorRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the coaster readouts as Create display sources.
 *
 * <p>This is Create's public add-on surface -- {@code com.simibubi.create.api.behaviour.display}
 * -- not its internals, so it costs no mixins and does not care what Create rearranges elsewhere.
 * Registration is an ordinary NeoForge {@link DeferredRegister} into
 * {@link CreateRegistries#DISPLAY_SOURCE}.
 *
 * <p>Binding is separate from registering. A source exists once it is in the registry, but Create
 * only offers it in a Display Link's dropdown for blocks it has been attached to, via the
 * {@code BY_BLOCK} multi-registry. That attachment has to happen after registries are frozen,
 * which is why {@link #bind()} is called from common setup rather than from here.
 */
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

    /** The station countdown -- "Departing in 2.4s" -- for a board on a platform. */
    public static final DeferredHolder<DisplaySource, RideDisplaySource> TIMER =
            DISPLAY_SOURCES.register("coaster_timer",
                    () -> new RideDisplaySource(RideDisplaySource.Readout.TIMER));

    public static void register(IEventBus modBus) {
        DISPLAY_SOURCES.register(modBus);
    }

    /**
     * Attach every readout to the Sensor Block.
     *
     * <p>{@code BY_BLOCK} is a MULTI registry, so one block can carry several sources and the
     * player picks between them in the link screen. That is the whole reason this does not need
     * a block of its own: the Sensor Block already sits trackside and already stores the
     * anchorpoint pair that names a curve, which is exactly the address the readouts look up.
     */
    public static void bind() {
        var sensor = SensorRegistry.SENSOR_BLOCK.get();
        attach(sensor, "sensor", SPEED, TOP_SPEED, CARTS, STATUS, TIMER);

        // And on the anchorpoint itself, which is what someone building a station reaches for
        // first -- it is the block that owns the platform. Requiring a Sensor Block just to
        // read a countdown off the thing already measuring it is a chore with no payoff.
        //
        // Wrapped defensively and logged: this reaches into the BASE MOD's block registry, so
        // a version that renames or splits the anchorpoint must degrade to "sensor still works"
        // with a clear log line, not a silent "not a display source" that looks like our bug.
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

    /** Attach several sources to one block, logging the result so a failure is never silent. */
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
