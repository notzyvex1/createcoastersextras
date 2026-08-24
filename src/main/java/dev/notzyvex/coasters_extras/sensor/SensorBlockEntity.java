package dev.notzyvex.coasters_extras.sensor;

import dev.notzyvex.coasters_extras.net.SensorTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Remembers which Sensor Track this block watches, and powers the block while a coaster is on it.
 *
 * <p>The link is stored as the two anchorpoints that a curve runs between, because that pair
 * is what identifies a curve -- the curve itself is not addressable. The pair is written onto
 * the item when you link it and copied in here when the block is placed, which is why an
 * unlinked Sensor Block cannot be placed at all: there would be nothing for it to watch.
 */
public class SensorBlockEntity extends BlockEntity {

    private static final String KEY_A = "SensorA";
    private static final String KEY_B = "SensorB";

    private @Nullable BlockPos anchorA;
    private @Nullable BlockPos anchorB;

    public SensorBlockEntity(BlockPos pos, BlockState state) {
        super(SensorRegistry.SENSOR_BLOCK_ENTITY.get(), pos, state);
    }

    public void link(BlockPos a, BlockPos b) {
        this.anchorA = a;
        this.anchorB = b;
        setChanged();
    }

    public boolean isLinked() {
        return anchorA != null && anchorB != null;
    }

    public @Nullable BlockPos getAnchorA() {
        return anchorA;
    }

    public @Nullable BlockPos getAnchorB() {
        return anchorB;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  SensorBlockEntity be) {
        if (!be.isLinked()) {
            return;
        }
        boolean busy = SensorTracker.occupied(be.anchorA, be.anchorB, level.getGameTime());
        boolean was = state.getValue(SensorBlock.POWERED);

        if (was == busy) {
            // Holding: a spark now and then, so a live sensor reads as live rather than as a
            // block that happens to be lit.
            if (busy && level instanceof ServerLevel sl && level.getGameTime() % 6 == 0) {
                Vec3 c = Vec3.atCenterOf(pos);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.45, c.z,
                                 1, 0.28, 0.28, 0.28, 0.01);
            }
            return;
        }

        level.setBlock(pos, state.setValue(SensorBlock.POWERED, busy), 3);
        // Tell the neighbours, or a redstone line running off it never notices.
        level.updateNeighborsAt(pos, state.getBlock());

        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Vec3 c = Vec3.atCenterOf(pos);
        if (busy) {
            // Trip: a burst of sparks off every face, and two layered sounds -- a bulb's
            // electrical thunk under a sharp contact click. One alone reads as a lamp or as
            // a tripwire; together they read as a relay closing.
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.45, c.z,
                             14, 0.42, 0.42, 0.42, 0.08);
            sl.sendParticles(ParticleTypes.CRIT, c.x, c.y + 0.45, c.z,
                             4, 0.3, 0.3, 0.3, 0.02);
            sl.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS,
                         0.7F, 1.45F);
            sl.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS,
                         0.35F, 1.9F);
        } else {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.45, c.z,
                             3, 0.22, 0.22, 0.22, 0.01);
            sl.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS,
                         0.5F, 1.35F);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (anchorA != null) tag.putLong(KEY_A, anchorA.asLong());
        if (anchorB != null) tag.putLong(KEY_B, anchorB.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        anchorA = tag.contains(KEY_A) ? BlockPos.of(tag.getLong(KEY_A)) : null;
        anchorB = tag.contains(KEY_B) ? BlockPos.of(tag.getLong(KEY_B)) : null;
    }

    /** Writes a link straight into a tag, for stamping onto the item when you bind it. */
    public static CompoundTag linkTag(BlockPos a, BlockPos b) {
        CompoundTag tag = new CompoundTag();
        tag.putLong(KEY_A, a.asLong());
        tag.putLong(KEY_B, b.asLong());
        return tag;
    }

    public static boolean hasLink(CompoundTag tag) {
        return tag != null && tag.contains(KEY_A) && tag.contains(KEY_B);
    }

    public static BlockPos readA(CompoundTag tag) {
        return BlockPos.of(tag.getLong(KEY_A));
    }

    public static BlockPos readB(CompoundTag tag) {
        return BlockPos.of(tag.getLong(KEY_B));
    }
}
