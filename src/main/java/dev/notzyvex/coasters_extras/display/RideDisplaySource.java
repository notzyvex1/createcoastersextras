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

public class RideDisplaySource extends DisplaySource {

    public enum Readout {
        SPEED("speed", s -> fmt(s.speed()) + " b/s"),
        TOP_SPEED("top_speed", s -> fmt(s.topSpeed()) + " b/s"),
        CARTS("carts", s -> String.valueOf(s.carts())),
        STATUS("status", s -> s.carts() > 0 ? "RUNNING" : "CLEAR"),
        TIMER("timer", s -> "");

        final String id;
        final Function<RideTelemetry.Sample, String> render;

        Readout(String id, Function<RideTelemetry.Sample, String> render) {
            this.id = id;
            this.render = render;
        }

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

        if (be instanceof CoasterAnchorpointBlockEntity anchor) {
            return fromAnchorpoint(anchor, level.getGameTime());
        }

        if (!(be instanceof SensorBlockEntity sensor) || !sensor.isLinked()) {
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

        BlockPos first = peers.keySet().iterator().next();
        return readout == Readout.TIMER
                ? station(self, first, now)
                : List.of(Component.literal(
                        readout.render.apply(RideTelemetry.read(self, first, now))));
    }

    private static List<MutableComponent> station(BlockPos a, BlockPos b, long now) {
        var entry = dev.notzyvex.coasters_extras.net.StationTracker.read(a, b, now);
        if (entry == null) {
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

    private static final int BAR_CELLS = 10;

    private static MutableComponent bar(int ticksLeft, int dwell) {
        int filled = dwell <= 0 ? BAR_CELLS
                : (int) Math.round(BAR_CELLS * (1.0 - (double) ticksLeft / dwell));
        filled = Math.max(0, Math.min(BAR_CELLS, filled));
        return Component.literal("█".repeat(filled) + "░".repeat(BAR_CELLS - filled));
    }

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

    @Override
    public int getPassiveRefreshTicks() {
        return readout == Readout.TOP_SPEED ? 20 : 10;
    }

    @Override
    protected String getTranslationKey() {
        return "coaster_" + readout.id;
    }
}
