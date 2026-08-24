package dev.notzyvex.coasters_extras.client;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.content.trains.track.BezierConnection;
import net.createmod.catnip.lang.LangBuilder;
import dev.notzyvex.coasters_extras.CoastersExtras;
import dev.notzyvex.coasters_extras.net.StationPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Map;
import dev.notzyvex.coasters_extras.StationBoostBehaviour;

public final class StationGoggles {

    private static final int BAR_CELLS = 10;

    private static LangBuilder lang(String key, Object... args) {
        return new LangBuilder(CoastersExtras.MOD_ID).translate(key, args);
    }

    private static LangBuilder text(String value) {
        return new LangBuilder(CoastersExtras.MOD_ID).text(value);
    }

    public static boolean isStationAnchor(BlockEntity be) {
        return curves(be) != null && hasStationCurve(be);
    }

    public static boolean append(BlockEntity be, List<Component> tooltip) {
        BlockPos pos = be.getBlockPos();
        String kind = curveKind(be);
        if (kind != null && !"station_track".equals(kind)) {
            return appendTrack(be, pos, kind, tooltip);
        }
        lang("goggles.station")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        StationPayload.Entry entry = StationStates.at(pos);
        if (entry == null) {
            StationBoostBehaviour dial =
                    BlockEntityBehaviour.get(be.getLevel(), pos, StationBoostBehaviour.TYPE);
            int seconds = (dial != null && dial.value > 0)
                    ? dial.value : StationBoostBehaviour.STATION_DWELL_DEFAULT;
            lang("goggles.station.idle")
                    .style(ChatFormatting.DARK_GRAY)
                    .forGoggles(tooltip, 1);
            lang("goggles.station.dwell",
                            text(seconds + "s").style(ChatFormatting.AQUA))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);

            lang("goggles.station.boost",
                            text(dial != null && dial.launch > 0
                                    ? dial.launch + " b/s" : "Default")
                                    .style(ChatFormatting.AQUA))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);

            dev.notzyvex.coasters_extras.SendDirectionBehaviour sendDial =
                    com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(
                            be.getLevel(), be.getBlockPos(),
                            dev.notzyvex.coasters_extras.SendDirectionBehaviour.TYPE);
            dev.notzyvex.coasters_extras.SendDirection sendDir =
                    sendDial != null ? sendDial.direction()
                                     : dev.notzyvex.coasters_extras.SendDirection.AUTO;
            lang("goggles.station.direction",
                            Component.translatable(sendDir.getTranslationKey())
                                    .copy().withStyle(ChatFormatting.AQUA))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);
            return true;
        }

        switch (entry.state()) {
            case StationPayload.STATE_ARRIVING ->
                    lang("goggles.station.arriving")
                            .style(ChatFormatting.YELLOW)
                            .forGoggles(tooltip, 1);

            case StationPayload.STATE_HELD ->
                    lang("goggles.station.held")
                            .style(ChatFormatting.RED)
                            .forGoggles(tooltip, 1);

            case StationPayload.STATE_LEAVING ->
                    lang("goggles.station.departing")
                            .style(ChatFormatting.GREEN)
                            .forGoggles(tooltip, 1);

            default -> {
                double secs = entry.ticksLeft() / 20.0;
                double left = entry.dwell() <= 0 ? 0
                        : Math.max(0.0, Math.min(1.0,
                                (double) entry.ticksLeft() / entry.dwell()));
                int hex = dwellColour(left);
                lang("goggles.station.departing_in",
                                text(String.format("%.1fs", secs)).color(hex))
                        .style(ChatFormatting.GRAY)
                        .forGoggles(tooltip, 1);
                bar(entry.ticksLeft(), entry.dwell(), hex).forGoggles(tooltip, 1);
            }
        }
        return true;
    }

    private static int dwellColour(double left) {
        if (left > 0.66) return 0x5BE36A;
        if (left > 0.33) return 0xFFD23B;
        if (left > 0.15) return 0xFF7A2D;
        return 0xFF2D2D;
    }

    private static LangBuilder bar(int ticksLeft, int dwell, int hex) {
        int filled = dwell <= 0 ? 0
                : (int) Math.round(BAR_CELLS * (1.0 - (double) ticksLeft / dwell));
        filled = Math.max(0, Math.min(BAR_CELLS, filled));
        return text("█".repeat(filled)).color(hex)
                .add(text("█".repeat(BAR_CELLS - filled))
                        .style(ChatFormatting.DARK_GRAY));
    }

    @SuppressWarnings("unchecked")
    private static Map<BlockPos, BezierConnection> curves(BlockEntity be) {
        if (be == null) return null;
        try {
            Object view = be.getClass().getMethod("getAnchorPeerCurvesView").invoke(be);
            return view instanceof Map ? (Map<BlockPos, BezierConnection>) view : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasStationCurve(BlockEntity be) {
        return curveKind(be) != null;
    }

    static String curveKind(BlockEntity be) {
        Map<BlockPos, BezierConnection> map = curves(be);
        if (map == null) return null;
        for (BezierConnection bc : map.values()) {
            if (bc == null || bc.getMaterial() == null) continue;
            var id = bc.getMaterial().id;
            if (!"coasters_extras".equals(id.getNamespace())) continue;
            switch (id.getPath()) {
                case "station_track", "boost_track", "powered_boost_track", "brake_track",
                     "launch_track", "reverse_track", "splash_track" -> {
                    return id.getPath();
                }
                default -> { }
            }
        }
        return null;
    }

    private static String trackName(String kind) {
        return switch (kind) {
            case "boost_track"          -> "Boost Track";
            case "powered_boost_track"  -> "Powered Boost Track";
            case "brake_track"          -> "Brake Track";
            case "launch_track"         -> "Launch Track";
            case "reverse_track"        -> "Reverse Track";
            case "splash_track"         -> "Splash Track";
            default                     -> "Coaster Track";
        };
    }

    private static String settingName(String kind) {
        return switch (kind) {
            case "brake_track"   -> "Brake Intensity";
            case "launch_track"  -> "Launch Intensity";
            case "reverse_track" -> "Reverse Boost";
            case "splash_track"  -> "Water Boost";
            default              -> "Boost Speed";
        };
    }

    private static boolean appendTrack(BlockEntity be, BlockPos pos, String kind,
                                       List<Component> tooltip) {
        text(trackName(kind)).style(ChatFormatting.GRAY).forGoggles(tooltip);

        StationBoostBehaviour dial =
                BlockEntityBehaviour.get(be.getLevel(), pos, StationBoostBehaviour.TYPE);
        String speed;
        if (dial == null || dial.value <= 0) {
            speed = "brake_track".equals(kind) && dial != null && dial.value == 0
                    ? "Full stop" : "Default";
        } else {
            speed = dial.value + " b/s";
        }
        text(settingName(kind) + ": ")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);
        text("  " + speed).style(ChatFormatting.AQUA).forGoggles(tooltip, 1);

        dev.notzyvex.coasters_extras.SendDirectionBehaviour sendDial =
                BlockEntityBehaviour.get(be.getLevel(), pos,
                        dev.notzyvex.coasters_extras.SendDirectionBehaviour.TYPE);
        dev.notzyvex.coasters_extras.SendDirection sendDir =
                sendDial != null ? sendDial.direction()
                                 : dev.notzyvex.coasters_extras.SendDirection.AUTO;
        text("Send: ").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        tooltip.set(tooltip.size() - 1,
                tooltip.get(tooltip.size() - 1).copy()
                        .append(Component.translatable(sendDir.getTranslationKey())
                                .copy().withStyle(ChatFormatting.AQUA)));

        if ("launch_track".equals(kind) || "brake_track".equals(kind)
                || "boost_track".equals(kind)) {
            dev.notzyvex.coasters_extras.LaunchTriggerBehaviour trigger =
                    BlockEntityBehaviour.get(be.getLevel(), pos,
                            dev.notzyvex.coasters_extras.LaunchTriggerBehaviour.TYPE);
            boolean needs = trigger != null && trigger.needsSignal();
            text("Powered: ").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            tooltip.set(tooltip.size() - 1,
                    tooltip.get(tooltip.size() - 1).copy()
                            .append(text(needs ? "On Redstone" : "Always")
                                    .style(needs ? ChatFormatting.RED : ChatFormatting.AQUA)
                                    .component()));
        }
        return true;
    }

    private StationGoggles() {}
}
