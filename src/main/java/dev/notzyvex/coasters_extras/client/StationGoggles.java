package dev.notzyvex.coasters_extras.client;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
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

/**
 * The goggle tooltip shown on a station anchorpoint.
 *
 * <p>Create's convention: a grey heading, then indented lines underneath. The state line is
 * rewritten as the station moves through arriving, waiting, held and departing, so the same
 * tooltip that counts down also announces the dispatch rather than just vanishing.
 *
 * <p>The countdown itself is server knowledge -- the dwell timer lives in the drive hook,
 * which never runs on a client -- so it arrives over the wire and is read out of
 * {@link StationStates}. When no cart is present there is nothing to count, and the tooltip
 * shows the configured dwell instead so the anchor is still useful while you are building.
 */
public final class StationGoggles {

    private static final int BAR_CELLS = 10;

    /**
     * A lang line in OUR namespace.
     *
     * <p>{@code CreateLang.translate} prepends {@code create.}, so using it for our own keys
     * asks the game for {@code create.coasters_extras.goggles.station} -- which does not
     * exist, and Minecraft renders the raw key. Building with our own namespace is the whole
     * fix; everything else about Create's goggle formatting is worth keeping.
     */
    private static LangBuilder lang(String key, Object... args) {
        return new LangBuilder(CoastersExtras.MOD_ID).translate(key, args);
    }

    /** Literal text, for values interpolated into a line. */
    private static LangBuilder text(String value) {
        return new LangBuilder(CoastersExtras.MOD_ID).text(value);
    }


    /** True if this block entity is an anchorpoint carrying at least one station curve. */
    public static boolean isStationAnchor(BlockEntity be) {
        return curves(be) != null && hasStationCurve(be);
    }

    /**
     * Appends the station lines. Returns true if anything was added, so the caller can report
     * that the tooltip is non-empty.
     */
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
            // Nothing parked. Show what it is set to do, which is what you want while building.
            StationBoostBehaviour dial =
                    BlockEntityBehaviour.get(be.getLevel(), pos, StationBoostBehaviour.TYPE);
            // Zero on the dial means "never touched", and the drive code reads it that way:
            // CoasterCartDriveMixin does `dialled > 0 ? dialled : STATION_DWELL_DEFAULT`, so an
            // untouched station really does hold for 3 seconds. Printing the raw 0 told players
            // it holds for no time at all, which is why a working station was being reported as
            // broken -- and why the fix people reached for was scrolling, then reporting that
            // the scroll was broken too.
            //
            // The defaults have to stay in step, so this reads the shared constant rather
            // than repeating the number -- the dial prints the same fallback.
            int seconds = (dial != null && dial.value > 0)
                    ? dial.value : StationBoostBehaviour.STATION_DWELL_DEFAULT;
            lang("goggles.station.idle")
                    .style(ChatFormatting.DARK_GRAY)
                    .forGoggles(tooltip, 1);
            lang("goggles.station.dwell",
                            text(seconds + "s").style(ChatFormatting.AQUA))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);

            // The launch speed, from the second dial. Shown as "default" when untouched
            // rather than as a number, because the number would be the config value and
            // printing it here would imply this station is set to it -- the same confusion
            // the dwell line caused by printing a raw 0.
            lang("goggles.station.boost",
                            text(dial != null && dial.launch > 0
                                    ? dial.launch + " b/s" : "Default")
                                    .style(ChatFormatting.AQUA))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);

            // Which way it sends the ride out. "auto" is the untouched state and means the
            // coaster keeps whatever direction it arrived with -- worth printing rather than
            // leaving blank, because a station that always sends rides the same way and one
            // that just passes them through look identical until one surprises you.
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
                // Counting down. One decimal place: whole seconds tick too coarsely to read
                // as a live countdown, and two decimals is noise at 20 updates a second.
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


    /**
     * Colour for how much dwell is left: green with time to spare, red on the point of
     * dispatch. Applied to the number AND the filled bar cells, so both say the same thing.
     *
     * <p>Banded rather than a smooth ramp. Ten steps of a gradient produce ten colours nobody
     * can tell apart at tooltip size, where four distinct bands are read at a glance -- and a
     * glance is all anyone gives a goggle tooltip while a coaster is arriving.
     *
     * <p>Hex via {@code color()} rather than {@link ChatFormatting}: the sixteen named colours
     * have no orange, and green-to-red without one is a jump rather than a countdown.
     */
    private static int dwellColour(double left) {
        if (left > 0.66) return 0x5BE36A;
        if (left > 0.33) return 0xFFD23B;
        if (left > 0.15) return 0xFF7A2D;
        return 0xFF2D2D;
    }

    /** A filled-to-empty bar showing how much of the dwell has elapsed. */
    private static LangBuilder bar(int ticksLeft, int dwell, int hex) {
        int filled = dwell <= 0 ? 0
                : (int) Math.round(BAR_CELLS * (1.0 - (double) ticksLeft / dwell));
        filled = Math.max(0, Math.min(BAR_CELLS, filled));
        // Filled cells carry the countdown colour; the unfilled ones stay dark so the bar
        // still reads as a bar rather than as two colours of text.
        return text("█".repeat(filled)).color(hex)
                .add(text("█".repeat(BAR_CELLS - filled))
                        .style(ChatFormatting.DARK_GRAY));
    }

    /** The anchorpoint's curve map, or null if this is not an anchorpoint at all. */
    @SuppressWarnings("unchecked")
    private static Map<BlockPos, BezierConnection> curves(BlockEntity be) {
        if (be == null) return null;
        try {
            Object view = be.getClass().getMethod("getAnchorPeerCurvesView").invoke(be);
            return view instanceof Map ? (Map<BlockPos, BezierConnection>) view : null;
        } catch (Throwable ignored) {
            // Not an anchorpoint. Every kinetic block entity reaches this method, so a miss
            // is the normal case rather than an error.
            return null;
        }
    }

    private static boolean hasStationCurve(BlockEntity be) {
        return curveKind(be) != null;
    }

    /**
     * The path of the first curve of ours on this anchorpoint, or null if there is none.
     *
     * <p>Was a station-only test, which is why goggles showed nothing anywhere else -- you
     * could see a station's countdown but had to open a dial to find out what a boost or brake
     * was set to. Any track with a dial is worth reading.
     */
    static String curveKind(BlockEntity be) {
        Map<BlockPos, BezierConnection> map = curves(be);
        if (map == null) return null;
        for (BezierConnection bc : map.values()) {
            if (bc == null || bc.getMaterial() == null) continue;
            var id = bc.getMaterial().id;
            // Raw namespace and path, never id.equals(): our alias mixin makes every one of
            // our ids compare equal to the base mod's plain track.
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

    /** Human name for each track, so the first goggle line says what you are looking at. */
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

    /** What the primary dial on this track is called. Matches the label on the board itself. */
    private static String settingName(String kind) {
        return switch (kind) {
            case "brake_track"   -> "Brake Intensity";
            case "launch_track"  -> "Launch Intensity";
            case "reverse_track" -> "Reverse Boost";
            case "splash_track"  -> "Water Boost";
            default              -> "Boost Speed";
        };
    }

    /**
     * Every dial on a non-station anchorpoint, in one readout.
     *
     * <p>The three dials have to be edited separately -- Create's value board carries one range
     * for every row, so a 0..200 speed and a two-way toggle cannot share one -- but nothing
     * stops them being READ together. This is the answer to "why are there three boxes": look
     * at the anchorpoint with goggles and all of it is one glance, then open whichever one you
     * actually want to change.
     */
    private static boolean appendTrack(BlockEntity be, BlockPos pos, String kind,
                                       List<Component> tooltip) {
        text(trackName(kind)).style(ChatFormatting.GRAY).forGoggles(tooltip);

        StationBoostBehaviour dial =
                BlockEntityBehaviour.get(be.getLevel(), pos, StationBoostBehaviour.TYPE);
        // Zero means untouched everywhere except a brake, where it is a real setting: a dead
        // stop. Printing "Default" there would be a lie.
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

        // Only the three tracks that carry the Powered dial.
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
