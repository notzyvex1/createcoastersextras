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
            return fallback;
        }

        TrackMaterial ours = TrackMaterial.ALL.get(id);
        return ours != null ? ours : fallback;
    }
}
