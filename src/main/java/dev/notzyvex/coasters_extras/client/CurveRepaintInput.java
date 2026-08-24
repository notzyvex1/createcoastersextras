package dev.notzyvex.coasters_extras.client;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.content.trains.track.TrackMaterial;
import net.minecraft.resources.ResourceLocation;
import dev.notzyvex.coasters_extras.CoastersExtras;
import dev.notzyvex.coasters_extras.net.RepaintPayload;
import dev.notzyvex.coasters_extras.track.ModTrackVariants;
import dev.notzyvex.coasters_extras.track.TrackVariant;
import dev.silvergold.simulatedcoasters.client.track.TrackOutlineBezierAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CoastersExtras.MOD_ID, value = Dist.CLIENT)
public final class CurveRepaintInput {

    @SubscribeEvent
    static void onRightClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        ItemStack held = mc.player.getItemInHand(event.getHand());

        TrackBlockOutline.BezierPointSelection selection = TrackBlockOutline.result;
        if (selection == null) {
            return;
        }

        if (mc.player.isShiftKeyDown()) {
            TrackMaterial swap = coasters_extras$materialOf(held);
            if (swap != null) {
                coasters_extras$convert(mc, selection, swap, event);
                return;
            }
        }

        if (!(held.getItem() instanceof BlockItem block)) {
            return;
        }

        BezierConnection curve;
        BlockPos anchor;
        BlockPos peer;
        try {
            curve = TrackOutlineBezierAccess.primaryBezierForSelection(selection);
            if (curve == null || curve.bePositions == null) {
                return;
            }
            anchor = curve.bePositions.getFirst();
            peer = curve.bePositions.getSecond();
        } catch (Throwable ignored) {
            return;
        }
        if (anchor == null || peer == null) {
            return;
        }

        String path = BuiltInRegistries.BLOCK.getKey(block.getBlock()).getPath();
        TrackVariant match = TrackVariant.forBlock(path);
        if (match == null) {
            mc.player.displayClientMessage(
                    Component.translatable("coasters_extras.copycat.no_match",
                            block.getBlock().getName()).withStyle(ChatFormatting.RED), true);
            event.setCanceled(true);
            return;
        }

        var material = ModTrackVariants.MATERIALS.get(match);
        if (material == null) {
            return;
        }
        if (sameTrack(material.id, curve.getMaterial())) {
            mc.player.displayClientMessage(
                    Component.translatable("coasters_extras.copycat.already",
                            Component.literal(match.displayName())
                                    .withStyle(ChatFormatting.WHITE))
                            .withStyle(ChatFormatting.GRAY), true);
            event.setCanceled(true);
            return;
        }

        PacketDistributor.sendToServer(new RepaintPayload(anchor, peer, material.id));

        mc.player.displayClientMessage(
                Component.translatable("coasters_extras.copycat.painted",
                        Component.literal(match.displayName())
                                .withStyle(ChatFormatting.WHITE))
                        .withStyle(ChatFormatting.GREEN), true);
        mc.level.playSound(mc.player, mc.player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS,
                0.7F, 1.3F);

        event.setCanceled(true);
    }

    private static TrackMaterial coasters_extras$materialOf(ItemStack held) {
        var item = held.getItem();
        if (item instanceof dev.notzyvex.coasters_extras.track.VariantTrackItem variant) {
            return variant.material();
        }
        if (item instanceof dev.notzyvex.coasters_extras.track.BoostTrackItem functional) {
            return functional.material();
        }
        if (item instanceof dev.silvergold.simulatedcoasters.track.CoasterTrackItem) {
            return TrackMaterial.ALL.get(
                    ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_track"));
        }
        return null;
    }

    private static void coasters_extras$convert(Minecraft mc,
                                                TrackBlockOutline.BezierPointSelection selection,
                                                TrackMaterial material,
                                                InputEvent.InteractionKeyMappingTriggered event) {
        BezierConnection curve;
        BlockPos anchor;
        BlockPos peer;
        try {
            curve = TrackOutlineBezierAccess.primaryBezierForSelection(selection);
            if (curve == null || curve.bePositions == null) {
                return;
            }
            anchor = curve.bePositions.getFirst();
            peer = curve.bePositions.getSecond();
        } catch (Throwable ignored) {
            return;
        }
        if (anchor == null || peer == null) {
            return;
        }

        event.setCanceled(true);

        if (sameTrack(material.id, curve.getMaterial())) {
            mc.player.displayClientMessage(
                    Component.translatable("coasters_extras.copycat.already",
                            Component.literal(material.langName)
                                    .withStyle(ChatFormatting.WHITE))
                            .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        PacketDistributor.sendToServer(new RepaintPayload(anchor, peer, material.id));

        mc.player.displayClientMessage(
                Component.translatable("coasters_extras.copycat.painted",
                        Component.literal(material.langName).withStyle(ChatFormatting.WHITE))
                        .withStyle(ChatFormatting.GREEN), true);
        mc.level.playSound(mc.player, mc.player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS,
                0.7F, 1.3F);
    }

    private static boolean sameTrack(ResourceLocation want, TrackMaterial have) {
        if (want == null || have == null || have.id == null) {
            return false;
        }
        return want.getNamespace().equals(have.id.getNamespace())
                && want.getPath().equals(have.id.getPath());
    }

    private CurveRepaintInput() {}
}
