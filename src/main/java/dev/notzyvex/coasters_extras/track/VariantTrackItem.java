package dev.notzyvex.coasters_extras.track;

import com.simibubi.create.content.trains.track.TrackMaterial;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cosmetic track variant that lays exactly like the base mod's coaster track.
 *
 * <p>Extends their {@link CoasterTrackItem} so we inherit the whole anchorpoint workflow --
 * curves, bezier handles, validation. Previously these used Create's TrackBlockItem, which
 * placed them on the ground like ordinary Create track; that was the wrong mechanic.
 *
 * <p>Their placement code hardcodes the coaster material, so we publish ours into
 * {@link TrackPlacementContext} for the duration of the call and let
 * {@code CoasterTrackPlacementMixin} substitute it.
 *
 * <p><b>Sneak + right-click a block and the track becomes that block.</b> Every variant is a
 * copycat, not just the one called Copycat -- which is what people reach for, because Create's
 * own copycats work by showing them a material. Sneak is what keeps that from fighting
 * placement: a plain right-click still lays track, so the workflow nobody asked to change
 * is untouched.
 */
public class VariantTrackItem extends CoasterTrackItem {

    private final TrackVariant variant;

    public VariantTrackItem(Item.Properties properties, TrackVariant variant) {
        super(properties);
        this.variant = variant;
    }

    public TrackMaterial material() {
        return ModTrackVariants.MATERIALS.get(variant);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return super.useOn(ctx);
        }

        Level level = ctx.getLevel();
        BlockState state = level.getBlockState(ctx.getClickedPos());
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        TrackVariant match = TrackVariant.forBlock(path);

        if (match == null) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("coasters_extras.copycat.no_match",
                                state.getBlock().getName()).withStyle(ChatFormatting.RED), true);
            }
            // Consume rather than fall through: a sneak-click that missed should do nothing,
            // not quietly start laying track somewhere you were not aiming.
            return InteractionResult.CONSUME;
        }
        if (match == variant) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("coasters_extras.copycat.already",
                                Component.literal(match.displayName())
                                        .withStyle(ChatFormatting.WHITE))
                                .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.CONSUME;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = ctx.getItemInHand();
        // The whole stack converts -- one at a time would mean sixty-four clicks to change
        // the track you were about to lay.
        player.setItemInHand(ctx.getHand(), new ItemStack(
                ModTrackVariants.ITEMS.get(match).get(), held.getCount()));

        player.displayClientMessage(
                Component.translatable("coasters_extras.copycat.matched",
                        Component.literal(match.displayName())
                                .withStyle(ChatFormatting.WHITE))
                        .withStyle(ChatFormatting.GREEN), true);
        level.playSound(null, ctx.getClickedPos(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 0.7F, 1.2F);
        return InteractionResult.SUCCESS;
    }
}
