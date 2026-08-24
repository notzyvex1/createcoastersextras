package dev.notzyvex.coasters_extras.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code LivingEntity.jumping} so a seated driver's spacebar can be read server-side.
 *
 * <p>The field is already kept up to date for us: a passenger's client sends
 * {@code ServerboundPlayerInputPacket} every tick, and the server applies it through
 * {@code LivingEntity#setPlayerInput}, which writes {@code jumping} alongside the {@code zza}
 * the throttle already reads. It is only {@code protected}, so this is purely about reach --
 * no new packets, no client-side keybinding, nothing to keep in sync.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityJumpAccessor {

    @Accessor("jumping")
    boolean coasters_extras$isJumping();
}
