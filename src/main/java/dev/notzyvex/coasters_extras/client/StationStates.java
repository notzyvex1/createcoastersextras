package dev.notzyvex.coasters_extras.client;

import dev.notzyvex.coasters_extras.net.StationPayload;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side view of what every occupied station is doing.
 *
 * <p>Read by the goggle tooltip, which runs every frame, so this is a plain list scan -- there
 * are only ever a handful of occupied stations in a world.
 *
 * <p>Entries expire on their own. If the server goes quiet for any reason -- cart removed,
 * chunk unloaded, dimension change -- the tooltip stops rather than freezing on a stale
 * countdown.
 *
 * <p>{@link #active()} has no caller since the platform light was removed. It is kept because
 * this class is the client's whole view of station state and the next thing that wants to draw
 * one needs exactly this: every occupied station, already expired if the data is stale.
 */
public final class StationStates {

    private static final long TTL_MS = 750;

    private static volatile List<StationPayload.Entry> entries = new ArrayList<>();
    private static volatile long stamp = 0;

    public static void accept(List<StationPayload.Entry> incoming) {
        entries = incoming;
        stamp = System.currentTimeMillis();
    }

    private static boolean stale() {
        return System.currentTimeMillis() - stamp > TTL_MS;
    }

    /** Everything currently occupied, or an empty list once the data has gone stale. */
    public static List<StationPayload.Entry> active() {
        return stale() ? List.of() : entries;
    }

    /** The station this anchorpoint belongs to, or null if it is not currently occupied. */
    public static StationPayload.Entry at(BlockPos anchor) {
        if (anchor == null || stale()) return null;
        for (StationPayload.Entry e : entries) {
            if (anchor.equals(e.a()) || anchor.equals(e.b())) return e;
        }
        return null;
    }

    private StationStates() {}
}
