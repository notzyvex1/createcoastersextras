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

@Mixin(targets = "dev.silvergold.simulatedcoasters.balloon.BalloonAttachInteraction", remap = false)
public class BalloonAttachInteractionMixin {

    @Redirect(
            method = "onRightClickBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/core/Holder;)Z"
            )
    )
    private static boolean coasters_extras$anyBalloonItemCounts(ItemStack stack, Holder<Item> holder) {
        return stack.is(holder) || stack.is(ModTags.Items.BALLOONS);
    }

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

    @Inject(method = "onRightClickBlock", at = @At("RETURN"))
    private static void coasters_extras$clearHeld(PlayerInteractEvent.RightClickBlock event,
                                                 CallbackInfo ci) {
        BalloonPlacementContext.clear();
    }
}
