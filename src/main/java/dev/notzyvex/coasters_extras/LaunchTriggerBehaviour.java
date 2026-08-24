package dev.notzyvex.coasters_extras;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The "Launch On" dial: a two-icon picker on a Launch Track's anchorpoint.
 *
 * <p>Built the same way as {@link SendDirectionBehaviour}, and for the same reason -- see that
 * class for why a small dial has to be its own {@code ScrollOptionBehaviour} rather than another
 * row on the shared value board.
 *
 * <p>The three overrides below are each a real bug if omitted, and this is now the THIRD dial on
 * an anchorpoint, so the collisions are no longer hypothetical:
 *
 * <p>ONE: {@link #getType()}. {@code SmartBlockEntity} stores behaviours in a
 * {@code Map<BehaviourType<?>, ...>}. {@code ScrollOptionBehaviour} inherits
 * {@code ScrollValueBehaviour.TYPE}, so without our own type this dial and the Send dial would
 * be the same map key and one would evict the other outright.
 *
 * <p>TWO: {@link #netId()}. Every behaviour on the block is offered the same
 * {@code ValueSettingsPacket} and the server applies it to whichever one's {@code netId()}
 * matches. 0 is the default for everything Create ships and 1 is taken by the Send dial, so this
 * has to be 2 -- otherwise editing this dial would quietly rewrite the Send direction instead.
 *
 * <p>THREE: {@link #write}/{@link #read}. Every behaviour is handed the SAME {@code CompoundTag},
 * and {@code ScrollValueBehaviour} persists under the fixed key {@code "ScrollValue"}. Without
 * our own key this would be last-writer-wins with the speed dial on every chunk save.
 */
public class LaunchTriggerBehaviour extends ScrollOptionBehaviour<LaunchTrigger> {

    public static final BehaviourType<LaunchTriggerBehaviour> TYPE = new BehaviourType<>();

    /**
     * Distinguishes this dial in {@code ValueSettingsPacket}.
     *
     * <p>0 is every behaviour that has not overridden {@code netId()}; 1 is
     * {@link SendDirectionBehaviour#NET_ID}. Anything added after this must not reuse 2.
     */
    public static final int NET_ID = 2;

    /** Our own NBT key, because the tag is shared with every other behaviour on the block. */
    private static final String KEY = "LaunchTrigger";

    public LaunchTriggerBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(LaunchTrigger.class, label, be, slot);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public int netId() {
        return NET_ID;
    }

    /**
     * Fires when the player changes this dial, so the anchorpoint at the far end of the curve
     * can be kept in step.
     *
     * <p>Both ends describe the same curve and the drive code reads whichever answers first, so
     * two ends that disagree means editing the "wrong" one silently does nothing.
     */
    private java.util.function.Consumer<Integer> changed = v -> {};

    public LaunchTriggerBehaviour onChanged(java.util.function.Consumer<Integer> callback) {
        this.changed = callback;
        return this;
    }

    @Override
    public void setValueSettings(net.minecraft.world.entity.player.Player player,
                                 ValueSettings settings, boolean ctrlDown) {
        int before = value;
        super.setValueSettings(player, settings, ctrlDown);
        // Only on a real change: re-syncing the peer for a no-op edit would bounce a block
        // update back and forth for nothing.
        if (value != before) {
            changed.accept(value);
        }
    }

    /** The chosen option. Never null and never out of range, whatever is in the NBT. */
    public LaunchTrigger trigger() {
        return LaunchTrigger.byIndex(value);
    }

    /** True if this launch should wait for a redstone signal. */
    public boolean needsSignal() {
        return trigger().needsSignal();
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // Deliberately NOT super.write: that writes the shared "ScrollValue" key, which belongs
        // to the speed dial on this same block entity.
        nbt.putInt(KEY, Mth.clamp(value, 0, LaunchTrigger.COUNT - 1));
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // Same reason as write. Clamped rather than trusted: get() indexes the options array
        // directly, so an out-of-range value is an ArrayIndexOutOfBoundsException on the next
        // render rather than a wrong setting.
        value = nbt.contains(KEY)
                ? Mth.clamp(nbt.getInt(KEY), 0, LaunchTrigger.COUNT - 1)
                : LaunchTrigger.ALWAYS.ordinal();
    }
}
