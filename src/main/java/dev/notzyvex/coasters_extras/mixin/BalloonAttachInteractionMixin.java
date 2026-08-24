package dev.notzyvex.coasters_extras.mixin;

import dev.notzyvex.coasters_extras.BalloonPlacementContext;
import dev.notzyvex.coasters_extras.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets our balloon items attach to sub-levels the way the base mod's red one does, and
 * records which colour was used so the spawner can place the right block.
 *
 * <p>This is the most important mixin in the mod. Their {@code RedBalloonBlock} returns
 * {@code null} from {@code getStateForPlacement}, so a balloon is never placed as an
 * ordinary block -- <em>all</em> placement goes through
 * {@code BalloonAttachInteraction.onRightClickBlock}, which gates on the held item. Miss
 * that check and our balloons do nothing at all when right-clicked.
 *
 * <p>Descriptor matters: the call is {@code ItemStack.is(Holder)}, not
 * {@code ItemStack.is(Item)} -- they compare against the {@code DeferredItem} (itself a
 * {@code Holder}) without calling {@code .get()}. Targeting the {@code Item} overload
 * silently matches nothing.
 */
@Mixin(targets = "dev.silvergold.simulatedcoasters.balloon.BalloonAttachInteraction", remap = false)
public class BalloonAttachInteractionMixin {

    @Redirect(
            method = "onRightClickBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/core/Holder;)Z"
            )
            // deliberately NOT require = 0: if this stops matching, the mod is broken and
            // we want a loud failure at launch rather than balloons that quietly do nothing.
    )
    private static boolean coasters_extras$anyBalloonItemCounts(ItemStack stack, Holder<Item> holder) {
        return stack.is(holder) || stack.is(ModTags.Items.BALLOONS);
    }

    /** Record the held balloon so {@link BalloonSpawnerMixin} can place the right colour. */
    @Inject(method = "onRightClickBlock", at = @At("HEAD"))
    private static void coasters_extras$captureHeld(PlayerInteractEvent.RightClickBlock event,
                                                   CallbackInfo ci) {
        BalloonPlacementContext.clear();
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        ItemStack held = event.getEntity().getMainHandItem();
        if (held.is(ModTags.Items.BALLOONS)
                && held.getItem() instanceof BlockItem blockItem) {
            BalloonPlacementContext.set(blockItem.getBlock());
        }
    }

    /** Always clear, so a stale colour can never leak into an unrelated placement. */
    @Inject(method = "onRightClickBlock", at = @At("RETURN"))
    private static void coasters_extras$clearHeld(PlayerInteractEvent.RightClickBlock event,
                                                 CallbackInfo ci) {
        BalloonPlacementContext.clear();
    }
}
