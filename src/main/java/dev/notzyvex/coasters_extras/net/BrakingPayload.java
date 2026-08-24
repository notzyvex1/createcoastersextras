package dev.notzyvex.coasters_extras.net;

import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record BrakingPayload(List<double[]> positions) implements CustomPacketPayload {

    public static final Type<BrakingPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "braking"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BrakingPayload> CODEC =
            StreamCodec.of(BrakingPayload::write, BrakingPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, BrakingPayload p) {
        buf.writeVarInt(p.positions().size());
        for (double[] xyz : p.positions()) {
            buf.writeDouble(xyz[0]);
            buf.writeDouble(xyz[1]);
            buf.writeDouble(xyz[2]);
        }
    }

    private static BrakingPayload read(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<double[]> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new double[]{ buf.readDouble(), buf.readDouble(), buf.readDouble() });
        }
        return new BrakingPayload(out);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
