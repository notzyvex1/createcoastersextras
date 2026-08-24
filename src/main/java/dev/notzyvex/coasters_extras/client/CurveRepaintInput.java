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

/**
 * Right-click a placed curve while holding a block, and the curve becomes that block.
 *
 * <p>This is what people mean by a copycat: you show the thing a material and it takes it,
 * in place, without breaking anything. It works because {@code BezierConnection.setMaterial}
 * exists and the field behind it is not final -- the geometry stays exactly as built and only
 * the material changes.
 *
 * <p>Client-side by necessity: the curve under the crosshair is something only the client
 * knows, since Create computes the hovered bezier for the outline it draws. The server cannot
 * be asked where a player is pointing. So the client identifies the curve, and a packet asks
 * the server to make the change -- doing it locally would look right until the next chunk
 * load and then quietly revert.
 */
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

        // Holding a track item and sneaking: convert this section to that kind of track, in
        // place. Asked for by a ride builder whose complaint was that changing one section to a
        // brake meant destroying and relaying it, losing every micro-adjustment to the curve.
        //
        // A functional track IS its material -- the cart tick reads the effect off the curve's
        // material id -- so swapping the material is genuinely the whole job. The geometry is
        // never touched.
        //
        // Sneak, not a plain right-click. The base mod's track item lays a curve in TWO clicks,
        // one for each anchorpoint, and a plain click on a highlighted curve is ambiguous
        // between "convert this" and "I am partway through laying something".
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
            // Cancel anyway: the player aimed a block at a track meaning to recolour it, and
            // placing that block in mid-air instead is never what they wanted.
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

        // Otherwise the block you were holding gets placed against the track as well.
        event.setCanceled(true);
    }

    /**
     * The material a held track item lays, or null if this is not a track item.
     *
     * <p>Subclasses are tested before the base class they extend, which is not stylistic: every
     * one of ours extends the base mod's {@code CoasterTrackItem}, so checking that first would
     * match all of them and convert everything to plain track.
     */
    private static TrackMaterial coasters_extras$materialOf(ItemStack held) {
        var item = held.getItem();
        if (item instanceof dev.notzyvex.coasters_extras.track.VariantTrackItem variant) {
            return variant.material();
        }
        if (item instanceof dev.notzyvex.coasters_extras.track.BoostTrackItem functional) {
            return functional.material();
        }
        if (item instanceof dev.silvergold.simulatedcoasters.track.CoasterTrackItem) {
            // Plain coaster track, so a section can be turned back into ordinary track. Looked
            // up by id rather than held as a constant because it is the base mod's, and a null
            // here should mean "skip", not a crash at class load.
            return TrackMaterial.ALL.get(
                    ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_track"));
        }
        return null;
    }

    /** Swap the highlighted curve to {@code material}, or explain why nothing happened. */
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

        // Cancel regardless of the outcome below. The player sneak-clicked a track holding
        // track; letting that fall through starts laying a new curve from wherever they were
        // pointing, which is a worse outcome than doing nothing.
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

    /**
     * True if a curve already carries exactly the material we are about to paint on.
     *
     * <p>Compares the raw namespace and path rather than calling {@code id.equals(id)}, and that
     * is the whole point of this method. {@code ResourceLocationCoasterAliasMixin} makes EVERY
     * {@code coasters_extras:<x>_track} id report equal to
     * {@code simulatedcoasters:coaster_track} -- and every one of our track paths ends in
     * {@code _track} without exception, so a plain {@code equals} matched a base-mod curve
     * against any of our ~180 materials. The symptom was that aiming at ordinary Coaster Track
     * and painting it, or sneak-converting a section to a Brake, was refused with
     * "Already <something> Coaster Track" -- the two things this class exists to do.
     *
     * <p>Null-safe on both sides: a curve with no material is never "already" anything, and
     * the old expression's {@code getMaterial() != null} guard has to survive the rewrite or
     * this throws on exactly the case it used to handle.
     */
    private static boolean sameTrack(ResourceLocation want, TrackMaterial have) {
        if (want == null || have == null || have.id == null) {
            return false;
        }
        return want.getNamespace().equals(have.id.getNamespace())
                && want.getPath().equals(have.id.getPath());
    }

    private CurveRepaintInput() {}
}
