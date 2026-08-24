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

/**
 * Adds the station readout to the goggle tooltip.
 *
 * <p>Targets {@link KineticBlockEntity} rather than the anchorpoint itself because
 * {@code CoasterAnchorpointBlockEntity} does not declare {@code addToGoggleTooltip} -- it
 * inherits it -- and Mixin can only inject into a method the target class actually holds.
 * Injecting one level up and filtering by what the block entity can do keeps this to a single
 * cheap check for every other kinetic block in the game.
 *
 * <p>At RETURN rather than TAIL so the return value can be corrected: a station anchor with
 * no kinetic stats to report would otherwise answer "nothing to show" and Create would
 * discard the lines we just added.
 */
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
            // a tooltip is never worth breaking every kinetic block in the game over
        }
    }
}
