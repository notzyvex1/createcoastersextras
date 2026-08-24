package dev.notzyvex.coasters_extras.net;

import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RepaintPayload(BlockPos anchor, BlockPos peer, ResourceLocation material)
        implements CustomPacketPayload {

    public static final Type<RepaintPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "repaint"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RepaintPayload> CODEC =
            StreamCodec.of(RepaintPayload::write, RepaintPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, RepaintPayload payload) {
        buf.writeBlockPos(payload.anchor());
        buf.writeBlockPos(payload.peer());
        buf.writeResourceLocation(payload.material());
    }

    private static RepaintPayload read(RegistryFriendlyByteBuf buf) {
        return new RepaintPayload(buf.readBlockPos(), buf.readBlockPos(),
                buf.readResourceLocation());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
