package dev.notzyvex.coasters_extras.net;

import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record StationPayload(List<Entry> entries) implements CustomPacketPayload {

    public static final byte STATE_ARRIVING = 0;
    public static final byte STATE_WAITING = 1;
    public static final byte STATE_HELD = 2;
    public static final byte STATE_LEAVING = 3;

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
