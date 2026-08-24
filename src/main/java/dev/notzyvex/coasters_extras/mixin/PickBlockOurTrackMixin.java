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

/**
 * Gives back <em>our</em> track when picking one of our curves.
 *
 * <p>Side effect of {@link ResourceLocationCoasterAliasMixin}: their pick-block handler
 * hardcodes the item once its material check passes --
 *
 * <pre>
 *   if (bc.getMaterial().id.equals(COASTER.id))
 *       return new ItemStack(SimulatedCoasters.COASTER_TRACK.get());
 * </pre>
 *
 * Since our ids now compare equal, picking an Oak Track handed back their Coaster Track.
 *
 * <p>Runs after theirs and corrects the result: if the curve under the crosshair really
 * belongs to us, swap in the matching item. Our item and its material deliberately share a
 * registry path, so the material id doubles as the item id.
 */
@Mixin(Minecraft.class)
public class PickBlockOurTrackMixin {

    /**
     * The curve under the crosshair, read before their handler runs.
     *
     * <p>Reading it at RETURN meant the first pick always handed back their track and only a
     * second pick gave ours: their handler consumes and clears the hit state as part of
     * doing its job, so by RETURN there was nothing left to read. The value seen on the
     * second press was the one left over from the first.
     */
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
            return;   // genuinely their track: leave their behaviour alone
        }

        Item ours = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (ours == null) {
            return;
        }

        Inventory inv = player.getInventory();
        ItemStack held = inv.getSelected();
        if (held.is(ours)) {
            return;   // already correct
        }
        ItemStack stack = new ItemStack(ours);
        inv.setItem(inv.selected, stack);

        // ...and tell the SERVER about it.
        //
        // This is why the fix looked like it did not work. inv.setItem is client-side only, so
        // the corrected stack was a ghost: it appeared in the hotbar and reverted the moment
        // anything resynced the slot, which is what "picking gives their track" actually looked
        // like from the player's side. Vanilla's own creative pick-block does not stop at the
        // inventory either -- it goes through handleCreativeModeItemAdd, and so must this.
        //
        // Creative only, deliberately. In survival, vanilla pick-block moves an EXISTING stack
        // to hand rather than conjuring one, and handing a survival player a track they do not
        // own would be duplication, not a fix.
        if (mc.gameMode != null && mc.gameMode.getPlayerMode().isCreative()) {
            mc.gameMode.handleCreativeModeItemAdd(stack, inv.selected);
        }
    }

    /**
     * The curve currently under the crosshair.
     *
     * <p>Was reflecting on {@code AnchorPeerCurveHit.isActive()} and {@code .curve()} as
     * STATIC methods. They are not -- that class is a record and those are instance
     * accessors, so every call threw, the catch swallowed it, and this silently returned
     * null. The correction below never ran once, which is exactly the bug it was written to
     * fix still being reported.
     *
     * <p>Now it goes through the same two public entry points the base mod's own handler
     * uses: Create publishes the hovered bezier in {@link TrackBlockOutline#result}, and the
     * base mod resolves that selection to a curve. Both are ordinary classes, so no
     * reflection and no guessing.
     */
    private static BezierConnection coasters_extras$selectedCurve() {
        try {
            TrackBlockOutline.BezierPointSelection selection = TrackBlockOutline.result;
            if (selection == null) {
                return null;
            }
            return TrackOutlineBezierAccess.primaryBezierForSelection(selection);
        } catch (Throwable ignored) {
            // A pick-block correction is never worth a crash. Losing it means the player
            // gets the base mod's track -- the behaviour before any of this existed.
            return null;
        }
    }
}
