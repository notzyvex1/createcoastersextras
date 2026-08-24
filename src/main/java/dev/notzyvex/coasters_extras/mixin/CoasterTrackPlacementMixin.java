package dev.notzyvex.coasters_extras.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.track.TrackMaterial;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lays OUR track material instead of the base mod's when one of our track items is used.
 *
 * <p>{@code CoasterTrackPlacement} reads {@code CoasterTrackMaterials.COASTER} and hands it
 * to the curve builders; the resulting {@code BezierConnection} stores the material, so
 * rendering, persistence and drops all read it back off the placed curve. Redirecting the
 * field read is therefore enough to make an entire curve ours.
 *
 * <h2>Why the material comes from {@link Local &#64;Local}, not the base mod's static</h2>
 *
 * The base mod keeps the placing item in a single {@code static ItemStack lastItem}. That one
 * field is shared by the whole game, which is the root of two separate reports:
 *
 * <ul>
 *   <li><b>Multiplayer.</b> Every player shares it, so whoever placed last decides what
 *       everyone else places -- a friend laying brake track gets the host's boost track.</li>
 *   <li><b>Scroll wheel (single player too).</b> Switching hotbar slot does not always
 *       refresh it, so the next placement resolves against the item held a moment ago and
 *       comes out as plain track.</li>
 * </ul>
 *
 * <p>Rather than track that static, {@code &#64;Local(argsOnly = true)} pulls the
 * {@code ItemStack} straight out of the method the field read happens in -- the exact stack
 * driving THIS placement, in the thread doing it. Nothing shared, nothing to go stale, and no
 * capture plumbing on the entry points. (This matches the fix TeakIvy verified on a live
 * server; an earlier build here used a ThreadLocal populated at three inject points, which
 * worked but leaned on the stale static as a fallback.)
 *
 * <p><b>Why this touches none of our own classes.</b> Mixin code is injected into the
 * <em>target's</em> module, and under NeoForge every mod is a separate module, so our classes
 * are not resolvable from in there -- an earlier version that called a helper in our package
 * hard-crashed on startup. Everything here uses only vanilla, Create, MixinExtras, or the
 * target's own members. Our track item and its material share a registry path, so the held
 * item's id doubles as the material lookup key.
 */
@Mixin(targets = "dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement", remap = false)
public abstract class CoasterTrackPlacementMixin {

    @Redirect(
            method = "*",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Ldev/silvergold/simulatedcoasters/CoasterTrackMaterials;"
                           + "COASTER:Lcom/simibubi/create/content/trains/track/TrackMaterial;"
            ),
            require = 0
    )
    private static TrackMaterial coasters_extras$heldMaterial(@Local(argsOnly = true) ItemStack stack) {
        TrackMaterial fallback = dev.silvergold.simulatedcoasters.CoasterTrackMaterials.COASTER;

        if (stack == null || stack.isEmpty()) {
            return fallback;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !"coasters_extras".equals(id.getNamespace())) {
            return fallback;   // their track, or anything else: unchanged behaviour
        }

        TrackMaterial ours = TrackMaterial.ALL.get(id);
        return ours != null ? ours : fallback;
    }
}
