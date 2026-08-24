package dev.notzyvex.coasters_extras.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import dev.silvergold.simulatedcoasters.client.track.TrackOutlineBezierAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class PickBlockOurTrackMixin {

    private static final ThreadLocal<BezierConnection> AIMED = new ThreadLocal<>();

    @Inject(method = "pickBlock", at = @At("HEAD"))
    private void coasters_extras$captureAim(CallbackInfo ci) {
        AIMED.set(coasters_extras$selectedCurve());
    }

    @Inject(method = "pickBlock", at = @At("RETURN"))
    private void coasters_extras$pickOurTrack(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        LocalPlayer player = mc.player;
        BezierConnection bc = AIMED.get();
        AIMED.remove();
        if (player == null) {
            return;
        }

        if (bc == null || bc.getMaterial() == null) {
            return;
        }

        ResourceLocation id = bc.getMaterial().id;
        if (!"coasters_extras".equals(id.getNamespace())) {
            return;
        }

        Item ours = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (ours == null) {
            return;
        }

        Inventory inv = player.getInventory();
        ItemStack held = inv.getSelected();
        if (held.is(ours)) {
            return;
        }
        ItemStack stack = new ItemStack(ours);
        inv.setItem(inv.selected, stack);

        if (mc.gameMode != null && mc.gameMode.getPlayerMode().isCreative()) {
            mc.gameMode.handleCreativeModeItemAdd(stack, inv.selected);
        }
    }

    private static BezierConnection coasters_extras$selectedCurve() {
        try {
            TrackBlockOutline.BezierPointSelection selection = TrackBlockOutline.result;
            if (selection == null) {
                return null;
            }
            return TrackOutlineBezierAccess.primaryBezierForSelection(selection);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
