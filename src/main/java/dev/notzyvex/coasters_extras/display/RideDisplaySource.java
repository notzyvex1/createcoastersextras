package dev.notzyvex.coasters_extras.display;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.notzyvex.coasters_extras.net.RideTelemetry;
import dev.notzyvex.coasters_extras.sensor.SensorBlockEntity;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.function.Function;

/**
 * Reads live coaster numbers off a Sensor Block onto a Create display board.
 *
 * <p>One class, four readouts, chosen by the {@link Readout} handed to the constructor. Create
 * registers each as a separate {@code DisplaySource} so they appear as separate options in the
 * link's dropdown, but they differ only in which number they pull and how they word it -- four
 * near-identical files would have been four places to fix a formatting bug.
 *
 * <p><b>Why the Sensor Block and not a new one.</b> {@code DisplaySource.BY_BLOCK} is a MULTI
 * registry: several sources can hang off the same block. The Sensor Block is already trackside,
 * already stores the anchorpoint pair that names a curve, and already has a working binder for
 * pointing it at a rail. A dedicated telemetry block would have meant a model, a blockstate, a
 * recipe, a loot table and lang for no capability the sensor does not already have.
 */
public class RideDisplaySource extends DisplaySource {

    /** What a given instance reports. */
    public enum Readout {
        SPEED("speed", s -> fmt(s.speed()) + " b/s"),
        TOP_SPEED("top_speed", s -> fmt(s.topSpeed()) + " b/s"),
        CARTS("carts", s -> String.valueOf(s.carts())),
        STATUS("status", s -> s.carts() > 0 ? "RUNNING" : "CLEAR"),
        /**
         * The station countdown. Reads nothing from telemetry -- see
         * {@link RideDisplaySource#station} -- so its renderer is never called.
         */
        TIMER("timer", s -> "");

        final String id;
        final Function<RideTelemetry.Sample, String> render;

        Readout(String id, Function<RideTelemetry.Sample, String> render) {
            this.id = id;
            this.render = render;
        }

        /** One decimal place. Two is noise on a board read from across a park. */
        private static String fmt(double v) {
            return String.format("%.1f", v);
        }
    }

    private final Readout readout;

    public RideDisplaySource(Readout readout) {
        this.readout = readout;
    }

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context,
                                              DisplayTargetStats stats) {
        Level level = context.level();
        BlockEntity be = context.getSourceBlockEntity();

        // An ANCHORPOINT can be the source too, not just a Sensor Block.
        //
        // Requiring a sensor was the original design and it is the wrong shape for a station:
        // the station already has two anchorpoints, one at each end of its platform, and making
        // someone place and bind a separate block just to read the countdown off the thing that
        // owns it is a chore with no payoff. Create told them plainly what was wrong -- "not a
        // display source" -- which is the right error for a block nobody registered.
        if (be instanceof CoasterAnchorpointBlockEntity anchor) {
            return fromAnchorpoint(anchor, level.getGameTime());
        }

        if (!(be instanceof SensorBlockEntity sensor) || !sensor.isLinked()) {
            // Not an error worth shouting about: a sensor that has not been pointed at a rail yet
            // is a half-built ride, and a board reading "--" says that better than a red warning.
            return List.of(Component.literal("--").withStyle(ChatFormatting.DARK_GRAY));
        }

        BlockPos a = sensor.getAnchorA();
        BlockPos b = sensor.getAnchorB();

        if (readout == Readout.TIMER) {
            return station(a, b, level.getGameTime());
        }

        RideTelemetry.Sample sample = RideTelemetry.read(a, b, level.getGameTime());

        return List.of(Component.literal(readout.render.apply(sample)));
    }

    /**
     * Read the curve an anchorpoint belongs to.
     *
     * <p>An anchorpoint can join several curves, so the right one is found by asking which of
     * its peers has anything to report -- for the timer, which one is a station with a ride on
     * it. Picking the first peer blindly would read whichever curve happened to be built first.
     */
    private List<MutableComponent> fromAnchorpoint(CoasterAnchorpointBlockEntity anchor,
                                                   long now) {
        BlockPos self = anchor.getBlockPos();
        var peers = anchor.getAnchorPeerCurvesView();
        if (peers == null || peers.isEmpty()) {
            return List.of(Component.literal("--").withStyle(ChatFormatting.DARK_GRAY));
        }

        for (BlockPos peer : peers.keySet()) {
            if (readout == Readout.TIMER) {
                if (dev.notzyvex.coasters_extras.net.StationTracker.read(self, peer, now) != null) {
                    return station(self, peer, now);
                }
            } else if (RideTelemetry.read(self, peer, now).carts() > 0) {
                return List.of(Component.literal(
                        readout.render.apply(RideTelemetry.read(self, peer, now))));
            }
        }

        // Nothing live on any of them. Fall back to the first peer so the board still shows a
        // resting value -- "Empty", or a zeroed speed -- rather than a dash that looks broken.
        BlockPos first = peers.keySet().iterator().next();
        return readout == Readout.TIMER
                ? station(self, first, now)
                : List.of(Component.literal(
                        readout.render.apply(RideTelemetry.read(self, first, now))));
    }

    /**
     * The station countdown, worded for a board rather than a tooltip.
     *
     * <p>Read from {@code StationTracker}, where the drive hook already reports every station
     * state for the goggle overlay. The countdown exists nowhere else -- it lives inside that
     * hook, on the server only -- so the tracker had to start keeping its last report rather
     * than draining it every flush.
     *
     * <p>Two lines while counting down -- the wording and a filling bar -- and one line for
     * every other state, because "Held" has nothing to fill. Speed and cart count stay out of
     * it; they are separate sources for anyone who wants a second board.
     */
    private static List<MutableComponent> station(BlockPos a, BlockPos b, long now) {
        var entry = dev.notzyvex.coasters_extras.net.StationTracker.read(a, b, now);
        if (entry == null) {
            // Nothing on the platform. Not an error -- an empty station is the normal state
            // for most of a ride, and the board should say so plainly.
            return List.of(Component.literal("Empty").withStyle(ChatFormatting.DARK_GRAY));
        }
        return switch (entry.state()) {
            case dev.notzyvex.coasters_extras.net.StationPayload.STATE_WAITING -> List.of(
                    Component.literal(String.format("Departing in %.1fs",
                            entry.ticksLeft() / 20.0)),
                    bar(entry.ticksLeft(), entry.dwell()));
            case dev.notzyvex.coasters_extras.net.StationPayload.STATE_HELD ->
                    List.of(Component.literal("Held"));
            case dev.notzyvex.coasters_extras.net.StationPayload.STATE_ARRIVING ->
                    List.of(Component.literal("Arriving"));
            case dev.notzyvex.coasters_extras.net.StationPayload.STATE_LEAVING ->
                    List.of(Component.literal("Departing"));
            default -> List.of(Component.literal("--").withStyle(ChatFormatting.DARK_GRAY));
        };
    }

    /** Cells in the countdown bar. Fits a one-block-wide display board with room to spare. */
    private static final int BAR_CELLS = 10;

    /**
     * A filling bar for the dwell, as a second line.
     *
     * <p>Drawn with block characters rather than colour alone, so it still reads on a board too
     * far away to resolve individual pixels -- and so it survives a monochrome display target.
     * It FILLS as the countdown runs down, which is the direction people read a progress bar,
     * even though the number beside it is counting toward zero.
     */
    private static MutableComponent bar(int ticksLeft, int dwell) {
        int filled = dwell <= 0 ? BAR_CELLS
                : (int) Math.round(BAR_CELLS * (1.0 - (double) ticksLeft / dwell));
        filled = Math.max(0, Math.min(BAR_CELLS, filled));
        return Component.literal("█".repeat(filled) + "░".repeat(BAR_CELLS - filled));
    }

    /**
     * Zero the record when the link's signal resets.
     *
     * <p>Only the top-speed board does anything here. The others report what is happening right
     * now and have nothing to clear -- and resetting the high-water mark from any of them would
     * mean a park owner wiping their own record by rewiring an unrelated sign.
     */
    @Override
    public void onSignalReset(DisplayLinkContext context) {
        if (readout != Readout.TOP_SPEED) {
            return;
        }
        if (context.getSourceBlockEntity() instanceof SensorBlockEntity sensor
                && sensor.isLinked()) {
            RideTelemetry.resetTopSpeed(sensor.getAnchorA(), sensor.getAnchorB());
        }
    }

    /**
     * How often the board refreshes without a redstone pulse.
     *
     * <p>Create's default is far slower than a speedometer wants. Ten ticks is twice a second:
     * fast enough that the number tracks a coaster going past, slow enough that it stays legible
     * rather than blurring, and it costs one map lookup.
     */
    @Override
    public int getPassiveRefreshTicks() {
        return readout == Readout.TOP_SPEED ? 20 : 10;
    }

    @Override
    protected String getTranslationKey() {
        return "coaster_" + readout.id;
    }
}
