package dev.notzyvex.coasters_extras.net;

import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * "Make the curve between these two anchorpoints use this material."
 *
 * <p>Sent when a player right-clicks a placed curve while holding a block. The curve under
 * the crosshair is client knowledge -- Create publishes it, the server has no idea where
 * anyone is pointing -- but the change has to happen server-side or it lasts until the next
 * chunk load. So the client names the two ends and the material, and the server does the work.
 *
 * <p>Only the two positions travel, not the curve. Anything the client sent about the curve
 * itself would be a client-supplied description of world state, and the server would have to
 * verify it anyway; the anchorpoints are enough to find the real one.
 */
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
