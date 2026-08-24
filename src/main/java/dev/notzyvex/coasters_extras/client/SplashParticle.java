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

public class SplashParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected SplashParticle(ClientLevel level, double x, double y, double z,
                             double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;

        this.gravity = 1.2F;
        this.friction = 0.96F;

        this.setSize(0.02F, 0.02F);

        this.quadSize *= 1.7F;

        this.lifetime = 11 + this.random.nextInt(5);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }

        this.setSpriteFromAge(this.sprites);

        int fadeFrom = this.lifetime * 2 / 3;
        if (this.age > fadeFrom) {
            float left = (float) (this.lifetime - this.age)
                       / (float) Math.max(1, this.lifetime - fadeFrom);
            this.setAlpha(Math.max(0.0F, left));
        }

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
