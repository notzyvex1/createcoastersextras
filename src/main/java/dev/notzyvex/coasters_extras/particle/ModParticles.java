package dev.notzyvex.coasters_extras.particle;

import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CoastersExtras.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPLASH =
            PARTICLE_TYPES.register("splash", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }

    private ModParticles() {}
}
