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
 * The Send dial: a small three-icon picker, the same control Create puts on a Mechanical
 * Bearing.
 *
 * <p>All the small-dial behaviour is inherited. {@code ScrollOptionBehaviour}'s constructor
 * calls {@code between(0, options.length - 1)} and its {@code createBoard} returns a board with
 * {@code maxValue = max}, a single "Select" row and a {@code ScrollOptionSettingsFormatter} --
 * which is exactly what makes the board render as three icons instead of a two-hundred-notch
 * slider. {@code ScrollValueRenderer} then draws the block-side box as an
 * {@code IconValueBox} rather than a text one, because it checks
 * {@code instanceof ScrollOptionBehaviour} and this class still is one.
 *
 * <p>Three things had to be overridden, and each of them is a real bug if it is not:
 *
 * <p>ONE: {@link #getType()}. {@code SmartBlockEntity} keeps behaviours in a
 * {@code Map<BehaviourType<?>, ...>} filled by {@code put(b.getType(), b)}, and
 * {@code ScrollOptionBehaviour} does NOT override {@code getType} -- it inherits
 * {@code ScrollValueBehaviour.TYPE}. Leaving it would not clash with
 * {@link StationBoostBehaviour} (which has its own type) but it would clash with any other
 * plain scroll dial, and {@code BlockEntityBehaviour.get} needs a type of our own to look this
 * one up by anyway.
 *
 * <p>TWO: {@link #netId()}. This is the one that is easy to miss. Every behaviour on a block
 * entity is offered the same {@code ValueSettingsPacket}, and the server picks the one to apply
 * it to by matching {@code packet.behaviourIndex == behaviour.netId()} -- and
 * {@code ValueSettingsBehaviour.netId()} defaults to 0 for everything. Two dials on one
 * anchorpoint both answering 0 means every edit lands on whichever comes first in insertion
 * order, so the Send dial would look like it did nothing while quietly rewriting the speed.
 *
 * <p>THREE: {@link #write}/{@link #read}. {@code ScrollValueBehaviour} persists under the fixed
 * key {@code "ScrollValue"}, and {@code SmartBlockEntity.write} hands every behaviour the SAME
 * {@code CompoundTag}. Two behaviours writing {@code "ScrollValue"} is last-writer-wins on save
 * and both-read-the-same-number on load -- the speed dial and the Send dial would overwrite
 * each other every time the chunk saved.
 */
public class SendDirectionBehaviour extends ScrollOptionBehaviour<SendDirection> {

    public static final BehaviourType<SendDirectionBehaviour> TYPE = new BehaviourType<>();

    /**
     * Distinguishes this dial from the speed dial in {@code ValueSettingsPacket}.
     *
     * <p>Any value other than 0 works; 0 is taken by everything that has not overridden
     * {@code netId()}, which is every behaviour Create ships.
     */
    public static final int NET_ID = 1;

    /** Our own NBT key, because the tag is shared with every other behaviour on the block. */
    private static final String KEY = "SendDirection";

    /** The pre-2.2 key: a raw 0..200 bar position from the old shared board. */
    private static final String LEGACY_KEY = "StationDirection";

    public SendDirectionBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(SendDirection.class, label, be, slot);
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
     * two ends that disagree means editing the "wrong" one silently does nothing -- exactly the
     * bug the speed dial's sync exists to prevent.
     */
    private java.util.function.Consumer<Integer> changed = v -> {};

    public SendDirectionBehaviour onChanged(java.util.function.Consumer<Integer> callback) {
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
    public SendDirection direction() {
        return SendDirection.byIndex(value);
    }

    /** 0 auto, +1 forward, -1 reverse. */
    public int sign() {
        return direction().sign();
    }

    /** The index actually stored, clamped so a corrupt tag cannot index past the enum. */
    private int index() {
        return Mth.clamp(value, 0, SendDirection.COUNT - 1);
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // Deliberately NOT super.write: that writes the shared "ScrollValue" key. See the class
        // comment -- every behaviour on this block entity writes into this same CompoundTag.
        // BlockEntityBehaviour.write, the only thing further up the chain, is a no-op.
        int i = index();
        nbt.putInt(KEY, i);
        // Kept in step so an older build of the mod still reads the right direction. It costs
        // one int and it is the difference between a downgrade being a downgrade and a
        // downgrade silently reversing every station on the server.
        nbt.putInt(LEGACY_KEY, SendDirection.byIndex(i).toLegacyBar());
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // Same reason as write: super.read would take "ScrollValue", which belongs to the
        // speed dial on this block.
        if (nbt.contains(KEY)) {
            // Clamped rather than trusted. get() indexes the options array directly, so an
            // out-of-range value here is an ArrayIndexOutOfBoundsException on the next render.
            value = Mth.clamp(nbt.getInt(KEY), 0, SendDirection.COUNT - 1);
            return;
        }
        if (nbt.contains(LEGACY_KEY)) {
            // A world saved before the Send row moved off the shared board. The old value is a
            // raw 0..200 handle position, so it has to be decoded through the same three zones
            // the old dial read it with -- storing it as-is would be an ordinal of 97.
            value = SendDirection.fromLegacyBar(nbt.getInt(LEGACY_KEY)).ordinal();
            return;
        }
        // Neither key: an anchorpoint that predates the whole feature. Auto, which is what it
        // has been doing all along.
        value = SendDirection.AUTO.ordinal();
    }
}
