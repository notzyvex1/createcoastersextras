package dev.notzyvex.coasters_extras;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class StationBoostBehaviour extends ScrollValueBehaviour {

    public static final BehaviourType<StationBoostBehaviour> TYPE = new BehaviourType<>();

    public static final int ROW_PRIMARY = 0;
    public static final int ROW_LAUNCH = 1;
    public static final int ROW_SEND = 2;
    public static final int ROW_POWERED = 3;

    public static final int MAX_LAUNCH = 200;

    public static final int MAX_PRIMARY = 60;

    public int launch = 0;

    public int direction = 0;

    public static final int STATION_DWELL_DEFAULT = 3;

    public static final int DIR_AUTO = 0;
    public static final int DIR_FORWARD = 1;
    public static final int DIR_REVERSE = 2;

    private static final int ZONES = 3;

    public static int zoneOf(int raw) {
        int clamped = Math.max(0, Math.min(MAX_LAUNCH, raw));
        return Math.min(ZONES - 1, clamped * ZONES / (MAX_LAUNCH + 1));
    }

    private java.util.List<Integer> rowPlan() {
        java.util.List<Integer> plan = new java.util.ArrayList<>();
        plan.add(ROW_PRIMARY);
        if (station()) {
            plan.add(ROW_LAUNCH);
        }
        return plan;
    }

    private boolean hasPoweredRow() {
        return brake() || launchTrack() || (!station() && !reverse() && !splash()
                && !poweredBoost());
    }

    private int semanticRow(int boardRow) {
        java.util.List<Integer> plan = rowPlan();
        if (boardRow < 0 || boardRow >= plan.size()) {
            return ROW_PRIMARY;
        }
        return plan.get(boardRow);
    }

    private int boardRow(int semantic) {
        int i = rowPlan().indexOf(semantic);
        return i < 0 ? 0 : i;
    }

    private static int zoneOfRaw(int raw, int zones) {
        int clamped = Math.max(0, Math.min(MAX_LAUNCH, raw));
        return Math.min(zones - 1, clamped * zones / (MAX_LAUNCH + 1));
    }

    private static int rawOfZone(int zone, int zones) {
        return Math.min(MAX_LAUNCH, zone * (MAX_LAUNCH + 1) / zones
                                  + (MAX_LAUNCH + 1) / (zones * 2));
    }

    private SendDirectionBehaviour sendPeer() {
        return blockEntity == null || blockEntity.getLevel() == null ? null
                : com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(
                        blockEntity.getLevel(), blockEntity.getBlockPos(),
                        SendDirectionBehaviour.TYPE);
    }

    private LaunchTriggerBehaviour poweredPeer() {
        return blockEntity == null || blockEntity.getLevel() == null ? null
                : com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(
                        blockEntity.getLevel(), blockEntity.getBlockPos(),
                        LaunchTriggerBehaviour.TYPE);
    }

    public int directionZone() {
        return zoneOf(direction);
    }

    private int lastRow = ROW_PRIMARY;

    private java.util.function.Consumer<Integer> launchCallback = v -> {};

    public StationBoostBehaviour onLaunchChanged(java.util.function.Consumer<Integer> callback) {
        this.launchCallback = callback;
        return this;
    }

    private java.util.function.Consumer<Integer> directionCallback = v -> {};

    public StationBoostBehaviour onDirectionChanged(java.util.function.Consumer<Integer> callback) {
        this.directionCallback = callback;
        return this;
    }

    private java.util.function.BooleanSupplier isStation = () -> false;

    public StationBoostBehaviour whenStation(java.util.function.BooleanSupplier test) {
        this.isStation = test;
        return this;
    }

    private java.util.function.BooleanSupplier isBrake = () -> false;

    public StationBoostBehaviour whenBrake(java.util.function.BooleanSupplier test) {
        this.isBrake = test;
        return this;
    }

    private boolean brake() {
        try {
            return isBrake.getAsBoolean();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private java.util.function.BooleanSupplier isReverse = () -> false;

    public StationBoostBehaviour whenReverse(java.util.function.BooleanSupplier test) {
        this.isReverse = test;
        return this;
    }

    private java.util.function.BooleanSupplier isLaunch = () -> false;

    public StationBoostBehaviour whenLaunch(java.util.function.BooleanSupplier test) {
        this.isLaunch = test;
        return this;
    }

    private java.util.function.BooleanSupplier isSplash = () -> false;

    public StationBoostBehaviour whenSplash(java.util.function.BooleanSupplier test) {
        this.isSplash = test;
        return this;
    }

    public StationBoostBehaviour whenPoweredBoost(java.util.function.BooleanSupplier test) {
        this.poweredBoostTest = test;
        return this;
    }

    private java.util.function.BooleanSupplier poweredBoostTest = () -> false;

    private boolean poweredBoost() {
        return poweredBoostTest.getAsBoolean();
    }

    private boolean splash() {
        try {
            return isSplash.getAsBoolean();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean launchTrack() {
        try {
            return isLaunch.getAsBoolean();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean reverse() {
        try {
            return isReverse.getAsBoolean();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean station() {
        try {
            return isStation.getAsBoolean();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public StationBoostBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    private Component boardTitle() {
        if (reverse())      return Component.literal("Reverse Track");
        if (brake())        return Component.literal("Brake Track");
        if (station())      return Component.literal("Station Track");
        if (splash())       return Component.literal("Splash Track");
        if (launchTrack())  return Component.literal("Launch Track");
        if (poweredBoost()) return Component.literal("Powered Boost Track");
        return this.label;
    }

    private Component rowLabel(int semantic) {
        return switch (semantic) {
            case ROW_LAUNCH -> Component.literal("Launch");
            case ROW_SEND -> Component.literal("Send");
            case ROW_POWERED -> Component.literal("Powered");
            default -> Component.literal(
                    station() ? "Hold"
                              : reverse() ? "» Reverse Boost"
                              : brake() ? "» Brake Intensity"
                              : splash() ? "» Water Boost"
                              : launchTrack() ? "» Launch Intensity"
                                              : "» Boost Speed");
        };
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList.Builder<Component> rows = ImmutableList.builder();
        for (int semantic : rowPlan()) {
            rows.add(rowLabel(semantic));
        }
        return new ValueSettingsBoard(boardTitle(), MAX_LAUNCH, 10, rows.build(),
                new ValueSettingsFormatter(this::format));
    }

    private net.minecraft.network.chat.MutableComponent format(ValueSettings settings) {
        int row = semanticRow(settings.row());
        if (row == ROW_SEND) {
            return Component.translatable(
                    SendDirection.byIndex(zoneOfRaw(settings.value(), SendDirection.COUNT))
                            .getTranslationKey());
        }
        if (row == ROW_POWERED) {
            return Component.translatable(
                    LaunchTrigger.byIndex(zoneOfRaw(settings.value(), LaunchTrigger.COUNT))
                            .getTranslationKey());
        }
        if (row == ROW_LAUNCH) {
            return Component.literal(settings.value() == 0
                    ? "Default"
                    : settings.value() + " b/s");
        }
        if (station()) {
            return Component.literal(settings.value() > 0
                    ? settings.value() + "s"
                    : STATION_DWELL_DEFAULT + "s · Default");
        }
        return Component.literal(settings.value() == 0
                ? "Default"
                : settings.value() + " b/s");
    }

    @Override
    public void setValueSettings(Player player, ValueSettings setting, boolean ctrlDown) {
        lastRow = semanticRow(setting.row());

        if (lastRow == ROW_SEND) {
            SendDirectionBehaviour peer = sendPeer();
            if (peer != null) {
                int zone = zoneOfRaw(setting.value(), SendDirection.COUNT);
                if (zone != peer.value) {
                    peer.setValueSettings(player,
                            new ValueSettings(0, zone), ctrlDown);
                }
            }
            return;
        }
        if (lastRow == ROW_POWERED) {
            LaunchTriggerBehaviour peer = poweredPeer();
            if (peer != null) {
                int zone = zoneOfRaw(setting.value(), LaunchTrigger.COUNT);
                if (zone != peer.value) {
                    peer.setValueSettings(player,
                            new ValueSettings(0, zone), ctrlDown);
                }
            }
            return;
        }

        if (lastRow == ROW_LAUNCH) {
            int wanted = Math.max(0, Math.min(MAX_LAUNCH, setting.value()));
            if (wanted != launch) {
                launch = wanted;
                launchCallback.accept(wanted);
                blockEntity.setChanged();
                blockEntity.sendData();
                playFeedbackSound(this);
            }
            return;
        }
        int cap = station() ? MAX_PRIMARY : MAX_LAUNCH;
        int wanted = Math.max(0, Math.min(cap, setting.value()));
        super.setValueSettings(player, new ValueSettings(setting.row(), wanted), ctrlDown);
    }

    @Override
    public ValueSettings getValueSettings() {
        return switch (lastRow) {
            case ROW_LAUNCH -> new ValueSettings(boardRow(ROW_LAUNCH), launch);
            case ROW_SEND -> {
                SendDirectionBehaviour peer = sendPeer();
                yield new ValueSettings(boardRow(ROW_SEND),
                        rawOfZone(peer == null ? 0 : peer.value, SendDirection.COUNT));
            }
            case ROW_POWERED -> {
                LaunchTriggerBehaviour peer = poweredPeer();
                yield new ValueSettings(boardRow(ROW_POWERED),
                        rawOfZone(peer == null ? 0 : peer.value, LaunchTrigger.COUNT));
            }
            default -> new ValueSettings(boardRow(ROW_PRIMARY), value);
        };
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        nbt.putInt("StationLaunch", launch);
        nbt.putInt("StationDirection", direction);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        launch = nbt.getInt("StationLaunch");
        direction = nbt.getInt("StationDirection");
        lastRow = ROW_PRIMARY;
    }
}
