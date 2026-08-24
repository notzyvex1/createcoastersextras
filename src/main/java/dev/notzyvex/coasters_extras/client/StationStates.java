package dev.notzyvex.coasters_extras.client;

import dev.notzyvex.coasters_extras.net.StationPayload;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

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

    public static List<StationPayload.Entry> active() {
        return stale() ? List.of() : entries;
    }

    public static StationPayload.Entry at(BlockPos anchor) {
        if (anchor == null || stale()) return null;
        for (StationPayload.Entry e : entries) {
            if (anchor.equals(e.a()) || anchor.equals(e.b())) return e;
        }
        return null;
    }

    private StationStates() {}
}
