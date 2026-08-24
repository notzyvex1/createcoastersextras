package dev.notzyvex.coasters_extras.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The splash thrown off water track, drawn from our own eight-frame sprite.
 *
 * <p>Written rather than inherited because no vanilla provider does what this needs.
 * {@code SplashParticle.Provider} and {@code WaterDropParticle.Provider} both call
 * {@code pickSprite}, which draws ONE sprite at random out of the set and keeps it for the
 * particle's whole life -- point either at an eight-frame sheet and you get eight kinds of
 * still image, never an animation. The class that does animate, {@code SimpleAnimatedParticle},
 * runs {@code setSpriteFromAge} every tick but also returns full brightness from
 * {@code getLightColor}, so water would glow in an unlit tunnel.
 *
 * <p>So: {@link TextureSheetParticle} for the sprite plumbing, {@code setSpriteFromAge} for the
 * animation, and the inherited {@code getLightColor} so the droplets take the block light where
 * they land.
 *
 * <p>The frame index is {@code age * (frames - 1) / lifetime} -- see
 * {@code ParticleEngine.MutableSpriteSet#get} -- so the lifetime below is chosen to be a little
 * longer than the frame count, which gives each of the eight frames a tick or two on screen.
 */
public class SplashParticle extends TextureSheetParticle {

    /** The eight frames listed in {@code assets/coasters_extras/particles/splash.json}. */
    private final SpriteSet sprites;

    protected SplashParticle(ClientLevel level, double x, double y, double z,
                             double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        // The seven-argument constructor is the one that scatters: it jitters the speed it is
        // given, renormalises it and adds a little lift, which is where the outward spray comes
        // from. The four-argument one leaves the velocity at zero.
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;

        // Particle#tick applies gravity as yd -= 0.04 * gravity, so this is not the same scale
        // as WaterDropParticle's 0.06 -- 1.2 here lands at 0.048 per tick, a touch floatier than
        // a vanilla droplet, which suits a thrown sheet of water better than a falling drip.
        this.gravity = 1.2F;
        this.friction = 0.96F;

        // Collision box, not draw size. Kept tiny so a droplet does not shelf on the rail it
        // was thrown from; quadSize below is what actually sets how big it looks.
        this.setSize(0.02F, 0.02F);

        // The default quad is 0.1..0.2 wide, which is a droplet. This is a sheet of water off
        // the side of a cart, so it wants to be noticeably bigger -- and the 32x32 art has the
        // resolution to carry it.
        this.quadSize *= 1.7F;

        this.lifetime = 11 + this.random.nextInt(5);
        this.setSpriteFromAge(sprites);
    }

    /**
     * Translucent, not opaque.
     *
     * <p>{@code PARTICLE_SHEET_OPAQUE} disables blending outright, so every partly transparent
     * pixel in the generated art would render as solid white. Water has soft edges; it needs
     * the blend.
     */
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();          // ages, applies gravity and friction, moves, removes at lifetime
        if (this.removed) {
            return;
        }

        this.setSpriteFromAge(this.sprites);

        // Fade out over the back third rather than vanishing mid-air on the last tick.
        int fadeFrom = this.lifetime * 2 / 3;
        if (this.age > fadeFrom) {
            float left = (float) (this.lifetime - this.age)
                       / (float) Math.max(1, this.lifetime - fadeFrom);
            this.setAlpha(Math.max(0.0F, left));
        }

        // Landing in water ends the droplet instead of letting it sink through the surface.
        // Straight out of WaterDropParticle: take whichever is higher, the block's collision
        // shape or the fluid height, and remove once the particle is below it.
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        double surface = Math.max(
                this.level.getBlockState(pos)
                        .getCollisionShape(this.level, pos)
                        .max(Direction.Axis.Y, this.x - pos.getX(), this.z - pos.getZ()),
                this.level.getFluidState(pos).getHeight(this.level, pos));
        if (surface > 0.0 && this.y < pos.getY() + surface) {
            this.remove();
        }
    }

    /**
     * Handed the sprite set built from our particle json and asked for one particle per spawn.
     *
     * <p>Registered with {@code RegisterParticleProvidersEvent#registerSpriteSet}, which is the
     * overload that supplies a {@link SpriteSet}. {@code registerSpecial} is for types with no
     * json at all; ours has one, and taking that route would leave the sprite null.
     */
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new SplashParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
