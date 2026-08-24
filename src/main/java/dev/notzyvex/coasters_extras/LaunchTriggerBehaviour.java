package dev.notzyvex.coasters_extras;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class LaunchTriggerBehaviour extends ScrollOptionBehaviour<LaunchTrigger> {

    public static final BehaviourType<LaunchTriggerBehaviour> TYPE = new BehaviourType<>();

    public static final int NET_ID = 2;

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
        if (value != before) {
            changed.accept(value);
        }
    }

    public LaunchTrigger trigger() {
        return LaunchTrigger.byIndex(value);
    }

    public boolean needsSignal() {
        return trigger().needsSignal();
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        nbt.putInt(KEY, Mth.clamp(value, 0, LaunchTrigger.COUNT - 1));
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        value = nbt.contains(KEY)
                ? Mth.clamp(nbt.getInt(KEY), 0, LaunchTrigger.COUNT - 1)
                : LaunchTrigger.ALWAYS.ordinal();
    }
}
