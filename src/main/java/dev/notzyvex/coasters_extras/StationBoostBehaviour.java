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

/**
 * The anchorpoint dial: one value box, two bars.
 *
 * <p>Create's value board already supports this -- {@link ValueSettingsBoard} takes a LIST of
 * rows, and every setting arrives as a {@code ValueSettings(row, value)} pair. So a single box
 * can carry both numbers a station needs, which is far better than bolting a second box onto
 * the block: two boxes have to be positioned so they do not overlap, they each need their own
 * hit test, and the player has to discover that the second one exists at all.
 *
 * <p>Row 0 is what the old single-value dial did, and it still writes to the inherited
 * {@code value} field under the same {@code ScrollValue} NBT key -- so stations and boost
 * tracks built before this change keep their setting instead of silently resetting to zero.
 *
 * <p>Row 1 is new: how fast a station launches a ride back out. It persists separately, and
 * zero means "use the config default", matching how row 0 treats zero. The dial
 * itself just says "Default" -- the box is narrow and "config default" clipped.
 *
 * <p>There used to be a third row, Send, choosing which way a station dispatched. It is gone:
 * a board's {@code maxValue} is shared by every row, so putting a three-way choice next to a
 * 0..200 speed forced it onto a two-hundred-notch bar. It now lives on its own
 * {@link SendDirectionBehaviour}, which is a {@code ScrollOptionBehaviour} and therefore gets
 * the small icon picker Create uses for choices. Nothing here reads or writes a direction any
 * more -- {@code SendDirectionBehaviour} owns the old {@code StationDirection} key outright,
 * because two behaviours writing one key into one shared tag is last-writer-wins.
 *
 * <p>Registered under its own {@link BehaviourType} because {@code SmartBlockEntity} keeps
 * behaviours in a {@code Map<BehaviourType<?>, ...>} filled by {@code put(b.getType(), b)}.
 * Inheriting {@code ScrollValueBehaviour.TYPE} would make this and any other scroll dial on the
 * same block evict one another.
 */
public class StationBoostBehaviour extends ScrollValueBehaviour {

    public static final BehaviourType<StationBoostBehaviour> TYPE = new BehaviourType<>();

    /** Row indices, matching the order of the labels handed to the board. */
    public static final int ROW_PRIMARY = 0;
    public static final int ROW_LAUNCH = 1;
    /** Send direction, mirrored from {@link SendDirectionBehaviour} onto this board. */
    public static final int ROW_SEND = 2;
    /** Powered toggle, mirrored from {@link LaunchTriggerBehaviour} onto this board. */
    public static final int ROW_POWERED = 3;

    /** Dial ceiling. The config permits 500, but a drag bar is a poor way to enter 500. */
    public static final int MAX_LAUNCH = 200;

    /**
     * Ceiling for row 0 -- dwell seconds at a station, blocks per second everywhere else.
     *
     * <p>Lower than {@link #MAX_LAUNCH}, and that gap is a trap worth naming. Create's board
     * has ONE maxValue shared by every row, so all three bars are drawn 0..200 while row 0
     * only ever means 0..60. A value arriving from the wide bar therefore has to be clamped
     * here; without it, a number that only makes sense as a direction bar position could be
     * stored as a dwell time, and a station would announce a hold of over a hundred seconds.
     *
     * <p>The mixin builds the dial with {@code between(0, MAX_PRIMARY)} so the scroll path and
     * the board path agree on the same ceiling rather than each carrying their own copy.
     */
    public static final int MAX_PRIMARY = 60;

    /** Row 1: launch speed in blocks per second. 0 means "use the config value". */
    public int launch = 0;

    /**
     * Row 2: which way a station sends the ride out, stored as the raw bar position.
     *
     * <p>Create's board has one control -- an integer drag bar -- and one {@code maxValue} shared
     * by every row. There is no enum row, so a three-way choice has to be expressed on a 0..200
     * bar whether or not that is the natural instrument for it.
     *
     * <p>Rather than ask the player to land the handle on 0, 1 or 2 at the extreme left, the bar
     * is cut into three equal zones and any position within a zone selects it. The whole bar is
     * live, the formatter prints the name rather than the number, so what the player actually
     * does is drag between three labelled thirds.
     *
     * <p>Auto occupies the FIRST zone, so it contains zero. That is not cosmetic: {@code getInt}
     * returns 0 for a key that is not in the NBT, so every station built before this row existed
     * loads as auto. Putting auto in the middle would silently reverse every station in every
     * existing world.
     */
    public int direction = 0;

    /** What the zones mean, in bar order. */
    /**
     * How long an untouched station holds a ride. Mirrors STATION_DWELL_DEFAULT in
     * CoasterCartDriveMixin, which lives in a mixin this class cannot import -- so the
     * value is duplicated on purpose and named here so the duplication is at least
     * visible rather than a bare 3 in three files.
     */
    public static final int STATION_DWELL_DEFAULT = 3;

    public static final int DIR_AUTO = 0;
    public static final int DIR_FORWARD = 1;
    public static final int DIR_REVERSE = 2;

    private static final int ZONES = 3;

    /** The zone a bar position falls in. */
    public static int zoneOf(int raw) {
        int clamped = Math.max(0, Math.min(MAX_LAUNCH, raw));
        // The +1 keeps the very top of the bar inside the last zone rather than one past it.
        return Math.min(ZONES - 1, clamped * ZONES / (MAX_LAUNCH + 1));
    }

    /** This anchorpoint's chosen departure direction, as one of the DIR_ constants. */
    /**
     * Board index to semantic row.
     *
     * <p>The station board has three rows and a brake or boost board has two, because a
     * brake has nothing to launch -- and a row that does nothing is worse than a missing
     * one, since the player sets it and waits for an effect that never arrives.
     *
     * <p>Which means Send is index 2 on one board and index 1 on the other, while the rest
     * of this class -- the NBT keys, the callbacks, {@code lastRow} -- speaks in ROW_
     * constants. Everything crossing that boundary is translated here, in one place. The
     * alternative, deleting the row and leaving the indices alone, silently routes the
     * direction value into the launch speed, which is a data-loss bug that looks like a
     * cosmetic one.
     */
    /**
     * Which semantic rows this track's board shows, in display order.
     *
     * <p>One list drives the row labels, the board-index mapping and the write-back, so those
     * three cannot fall out of step. They previously could: the row index is what gets
     * persisted and what setValueSettings switches on, so a board that grew a row in one place
     * and not the others silently reinterpreted saved values.
     */
    private java.util.List<Integer> rowPlan() {
        java.util.List<Integer> plan = new java.util.ArrayList<>();
        plan.add(ROW_PRIMARY);
        if (station()) {
            plan.add(ROW_LAUNCH);
        }
        // Send and Powered are NOT rows here. They have their own icon dials on the
        // anchorpoint, and mirroring them onto this board as well put the same setting in
        // two places -- two ways to change one value, and a 0..200 drag bar standing in for
        // a two-way toggle. The dials are the control; this board is the numbers.
        return plan;
    }

    /** The three tracks that carry a Powered dial. Matches AnchorpointSpeedControlMixin. */
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

    /** Semantic row back to the index this block's board actually shows it at. */
    private int boardRow(int semantic) {
        int i = rowPlan().indexOf(semantic);
        return i < 0 ? 0 : i;
    }

    /** The option rows are choices spread over the shared 0..200 bar; this snaps a raw drag. */
    private static int zoneOfRaw(int raw, int zones) {
        int clamped = Math.max(0, Math.min(MAX_LAUNCH, raw));
        return Math.min(zones - 1, clamped * zones / (MAX_LAUNCH + 1));
    }

    /** A bar position that lands in the middle of the given zone, for reading back. */
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

    /** Which row the player last touched, so the box shows that one when reopened. */
    private int lastRow = ROW_PRIMARY;

    /**
     * Called when row 1 changes, so the far anchorpoint of the curve can be kept in step.
     *
     * <p>Row 0 already gets this from {@code withCallback}, but that only fires for the
     * inherited value. Without the same treatment here, setting the launch speed on one end of
     * a station and then reading it from the other returns zero -- which is the exact bug the
     * row-0 sync exists to prevent.
     */
    private java.util.function.Consumer<Integer> launchCallback = v -> {};

    public StationBoostBehaviour onLaunchChanged(java.util.function.Consumer<Integer> callback) {
        this.launchCallback = callback;
        return this;
    }

    /** Same again for row 2, and for the same reason: both ends of a curve must agree. */
    private java.util.function.Consumer<Integer> directionCallback = v -> {};

    public StationBoostBehaviour onDirectionChanged(java.util.function.Consumer<Integer> callback) {
        this.directionCallback = callback;
        return this;
    }

    /**
     * Whether this anchorpoint carries a station curve, decided when the board is opened.
     *
     * <p>The same dial sits on boost, brake and station anchorpoints, so its rows cannot be
     * fixed at construction: a boost track showed "Hold (seconds)" and a launch bar that did
     * nothing, because both were written for the station. It also cannot be cached -- the
     * curve attached to an anchorpoint changes as the player builds, so what the block is for
     * is only knowable at the moment someone opens it.
     */
    private java.util.function.BooleanSupplier isStation = () -> false;

    public StationBoostBehaviour whenStation(java.util.function.BooleanSupplier test) {
        this.isStation = test;
        return this;
    }

    /**
     * Whether this anchorpoint carries a BRAKE curve, decided when the board is opened.
     *
     * <p>A brake sets one number -- how slow to go -- and has no direction to choose, so its
     * board is a single row and drops the Send bar the boost and launch tracks show.
     */
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

    /**
     * Whether this anchorpoint carries a REVERSE curve, decided when the board is opened.
     *
     * <p>A reverse track has no speed of its own -- it only decides which way the ride leaves --
     * so its board is a single row and that row is the DIRECTION one, not the speed one. That
     * makes it the one board whose row 0 is not {@link #ROW_PRIMARY}, which is why
     * {@link #semanticRow} and {@link #boardRow} both special-case it: without that, dragging
     * the only bar on the board would be stored as a speed and the direction would never change.
     */
    private java.util.function.BooleanSupplier isReverse = () -> false;

    public StationBoostBehaviour whenReverse(java.util.function.BooleanSupplier test) {
        this.isReverse = test;
        return this;
    }

    /**
     * Whether this anchorpoint carries a LAUNCH curve, decided when the board is opened.
     *
     * <p>Only changes the wording. A boost eases a ride up to a cruising speed and a launch
     * throws it there from a standstill, so calling both of them the same thing on the dial
     * made the two tracks look interchangeable when they are not.
     */
    private java.util.function.BooleanSupplier isLaunch = () -> false;

    public StationBoostBehaviour whenLaunch(java.util.function.BooleanSupplier test) {
        this.isLaunch = test;
        return this;
    }

    /**
     * Whether this anchorpoint carries a SPLASH curve, decided when the board is opened.
     *
     * <p>Water drags a ride down, so a splash section long enough to look good is long enough
     * to strand a coaster in it. The dial is how you pay that back: it sets the speed the water
     * section drives the ride at, so a flume can actually run.
     */
    private java.util.function.BooleanSupplier isSplash = () -> false;

    public StationBoostBehaviour whenSplash(java.util.function.BooleanSupplier test) {
        this.isSplash = test;
        return this;
    }

    /**
     * True when this anchorpoint carries a Powered Boost curve.
     *
     * <p>Only used to title the board. Boost and Powered Boost share every row and every
     * setting, so nothing else needs to tell them apart -- but a player looking at two
     * identical dials on two adjacent anchorpoints does.
     */
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

    /** Row 0's label depends on the track: seconds at a station, speed everywhere else. */
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

    /**
     * What the board is titled: the name of the track this anchorpoint actually carries.
     *
     * <p>Every board used to open with the same literal "Track Speed", which was fine when
     * there was one functional track and is not fine with ten. An anchorpoint can also join
     * more than one curve, so "which track am I about to change" is a real question rather
     * than a cosmetic one, and the row label underneath only names the SETTING.
     *
     * <p>Checked in the same order as the board branches below, so the title can never
     * disagree with the rows it is sitting above.
     */
    private Component boardTitle() {
        if (reverse())      return Component.literal("Reverse Track");
        if (brake())        return Component.literal("Brake Track");
        if (station())      return Component.literal("Station Track");
        if (splash())       return Component.literal("Splash Track");
        if (launchTrack())  return Component.literal("Launch Track");
        if (poweredBoost()) return Component.literal("Powered Boost Track");
        return this.label;
    }

    /** The label for one semantic row on this track. */
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
        // ONE board per track, built from rowPlan(): the primary value, a Launch row on a
        // station, then Send, then Powered where the track has it.
        //
        // Send and Powered are MIRRORS of their own icon dials, not second copies -- writing a
        // row here writes straight through to the behaviour that owns it. The icon dials are
        // still on the anchorpoint for precise setting; this board is for seeing and changing
        // everything about a track in one place, which is the thing three separate boxes made
        // impossible.
        //
        // maxValue is shared across every row -- that is the Create API constraint that split
        // these out originally -- so the option rows snap the raw bar position into zones and
        // print the option name instead of the number.
        ImmutableList.Builder<Component> rows = ImmutableList.builder();
        for (int semantic : rowPlan()) {
            rows.add(rowLabel(semantic));
        }
        return new ValueSettingsBoard(boardTitle(), MAX_LAUNCH, 10, rows.build(),
                new ValueSettingsFormatter(this::format));
    }

    /**
     * The text in the value box.
     *
     * <p>Two rules, both learned from this reading badly in game:
     *
     * <p>ONE UNIT PER ROW, ALWAYS SHOWN. The old version printed a bare {@code "default"} for
     * an untouched row, which answered the wrong question -- the player is not asking whether
     * they have touched it, they are asking what it is going to do. So an untouched row now
     * prints the value that will actually be used, marked as the default. This is also what
     * the goggle tooltip prints, and the two disagreeing (dial said "default", goggles said
     * "3s") was its own small bug: the same station described two ways in two places.
     *
     * <p>THE ARROW POINTS THE WAY THE RIDE GOES. {@code >>} and {@code <<} were ASCII stand-ins
     * that read as a debug log. A single real arrow glyph, on the side of the word the ride
     * travels, is the whole label -- you can read it without reading it.
     */
    private net.minecraft.network.chat.MutableComponent format(ValueSettings settings) {
        int row = semanticRow(settings.row());
        // The option rows print the chosen option, not the bar position underneath it --
        // "Forward" rather than "133".
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
        // Row 0 is seconds at a station and blocks per second everywhere else: the same
        // number means two different things depending on what the curve is.
        if (station()) {
            // Must stay in step with STATION_DWELL_DEFAULT in CoasterCartDriveMixin, which
            // this class cannot import -- and with StationGoggles, which prints the same
            // fallback. Three places, one number.
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

        // The two mirrored rows do not live here -- they are written straight through to the
        // behaviour that owns them, so the icon dial and this row are always the same value
        // rather than two copies that can disagree.
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
        // Row 0 keeps the inherited path, so its callback -- which syncs the far anchorpoint
        // of the curve -- still fires exactly as before. Its ceiling depends on the track:
        //
        //   station  -> dwell SECONDS, capped at MAX_PRIMARY (60). Nobody holds a ride longer.
        //   anything -> track SPEED in b/s, capped at MAX_LAUNCH (200), the full bar.
        //
        // Clamping row 0 to MAX_PRIMARY unconditionally was a real bug: on a boost or brake
        // track row 0 IS the speed, so a player setting anything above 60 saw it snap back to
        // 60 (reported on CurseForge). The cap has to follow what the row means, not a fixed 60.
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
        // StationDialRow is intentionally no longer written -- see read(). Leaving a key that
        // nothing reads invites a later "fix" that restores it and brings the bug back.
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        // getInt returns 0 for a missing key, which is exactly the "use the default" value --
        // so a station saved before this field existed loads as untouched rather than broken.
        launch = nbt.getInt("StationLaunch");
        // Missing key -> 0 -> the first zone -> auto, which is why auto is the first zone.
        direction = nbt.getInt("StationDirection");
        // lastRow is deliberately NOT restored, and this is the fix for "the station timer
        // resets after leaving the world".
        //
        // getValueSettings() reports ONE (row, value) pair, chosen by lastRow, and Create
        // seeds the drag handle from it. Persisting lastRow meant a world could load with the
        // dial pointed at Send, whose value is a raw 0..200 bar position -- so the next drag
        // started from that position and, released on the Hold row, wrote it in as seconds.
        // A direction sitting mid-bar is ~100, which is exactly the "100s" that was reported.
        //
        // Remembering the row is only a convenience within one session; carrying it across a
        // reload buys nothing and lets one row's number seed another's.
        lastRow = ROW_PRIMARY;
    }
}
