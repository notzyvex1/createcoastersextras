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
