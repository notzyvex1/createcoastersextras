package dev.notzyvex.coasters_extras.net;

import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * What every occupied station is currently doing, sent to clients.
 *
 * <p>Two things on the client need this and neither can work it out for itself. The goggle
 * tooltip has to name a state and print a countdown, and the platform light has to know when
 * to run. Both are answers only the server has -- the dwell timer lives in the drive hook,
 * which never executes client-side.
 *
 * <p>Keyed by the two anchorpoint positions rather than by cart, because that is the question
 * both consumers actually ask: the tooltip is looking at an anchorpoint, and the light is
 * drawn along the curve between a pair of them.
 *
 * <p>Sent only when something changes, and only while a cart is on a station, so an idle
 * world sends nothing at all.
 */
public record StationPayload(List<Entry> entries) implements CustomPacketPayload {

    /** Rolling in, being slowed toward the far anchorpoint. */
    public static final byte STATE_ARRIVING = 0;
    /** Stopped at the far anchorpoint, counting down to dispatch. */
    public static final byte STATE_WAITING = 1;
    /** Stopped and pinned by a redstone signal -- the countdown is not running. */
    public static final byte STATE_HELD = 2;
    /** Dispatched, under power, not yet clear of the platform. */
    public static final byte STATE_LEAVING = 3;

    /**
     * @param a         one anchorpoint of the station curve
     * @param b         the other
     * @param state     one of the STATE_ constants
     * @param ticksLeft ticks until dispatch, or 0 when not counting
     * @param dwell     the full dwell in ticks, so the client can draw a progress bar
     */
    public record Entry(BlockPos a, BlockPos b, byte state, int ticksLeft, int dwell) {}

    public static final Type<StationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "station"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StationPayload> CODEC =
            StreamCodec.of(StationPayload::write, StationPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, StationPayload p) {
        buf.writeVarInt(p.entries().size());
        for (Entry e : p.entries()) {
            buf.writeBlockPos(e.a());
            buf.writeBlockPos(e.b());
            buf.writeByte(e.state());
            buf.writeVarInt(e.ticksLeft());
            buf.writeVarInt(e.dwell());
        }
    }

    private static StationPayload read(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Entry> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new Entry(buf.readBlockPos(), buf.readBlockPos(),
                    buf.readByte(), buf.readVarInt(), buf.readVarInt()));
        }
        return new StationPayload(out);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
