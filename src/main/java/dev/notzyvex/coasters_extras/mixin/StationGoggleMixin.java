package dev.notzyvex.coasters_extras.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.notzyvex.coasters_extras.client.StationGoggles;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = KineticBlockEntity.class, remap = false)
public class StationGoggleMixin {

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
    private void coasters_extras$stationReadout(List<Component> tooltip, boolean isSneaking,
                                                CallbackInfoReturnable<Boolean> cir) {
        try {
            BlockEntity self = (BlockEntity) (Object) this;
            if (!StationGoggles.isStationAnchor(self)) return;
            if (StationGoggles.append(self, tooltip)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable ignored) {
        }
    }
}
