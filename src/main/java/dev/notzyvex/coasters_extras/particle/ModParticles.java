package dev.notzyvex.coasters_extras.particle;

import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Our own particle types.
 *
 * <p>Common code, not client code: a {@link ParticleType} is the network identity of a
 * particle and the server needs it to name one in a spawn packet. Only the thing that
 * DRAWS it -- the provider, registered in {@code ClientSetup} -- is client-side.
 *
 * <p>{@link SimpleParticleType} carries no payload beyond the type itself, which is all a
 * splash needs; position and velocity travel in the spawn packet rather than in the
 * options. Anything that needed a colour or a size baked in would want a custom
 * {@code ParticleOptions} with its own codec instead.
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CoastersExtras.MOD_ID);

    /**
     * The water thrown off splash track.
     *
     * <p>{@code false} is the limiter flag, matching vanilla's own {@code minecraft:splash}:
     * the particle respects the client's particle setting rather than overriding it, so
     * someone on Decreased is not buried in droplets by a fast coaster.
     *
     * <p>The name must be {@code splash} because
     * {@code assets/coasters_extras/particles/splash.json} is matched to the type by id -- that
     * file is what hands the provider its eight frames. Get the name wrong and nothing warns
     * loudly: the json is written off as a "redundant texture list" at debug level, the sprite
     * set is never filled, and the first cart onto splash track throws an NPE out of the
     * particle renderer.
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPLASH =
            PARTICLE_TYPES.register("splash", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }

    private ModParticles() {}
}
