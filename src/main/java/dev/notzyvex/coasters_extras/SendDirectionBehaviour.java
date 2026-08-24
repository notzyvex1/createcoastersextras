package dev.notzyvex.coasters_extras;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class SendDirectionBehaviour extends ScrollOptionBehaviour<SendDirection> {

    public static final BehaviourType<SendDirectionBehaviour> TYPE = new BehaviourType<>();

    public static final int NET_ID = 1;

    private static final String KEY = "SendDirection";

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
        if (value != before) {
            changed.accept(value);
        }
    }

    public SendDirection direction() {
        return SendDirection.byIndex(value);
    }

    public int sign() {
        return direction().sign();
    }

    private int index() {
        return Mth.clamp(value, 0, SendDirection.COUNT - 1);
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        int i = index();
        nbt.putInt(KEY, i);
        nbt.putInt(LEGACY_KEY, SendDirection.byIndex(i).toLegacyBar());
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        if (nbt.contains(KEY)) {
            value = Mth.clamp(nbt.getInt(KEY), 0, SendDirection.COUNT - 1);
            return;
        }
        if (nbt.contains(LEGACY_KEY)) {
            value = SendDirection.fromLegacyBar(nbt.getInt(LEGACY_KEY)).ordinal();
            return;
        }
        value = SendDirection.AUTO.ordinal();
    }
}
