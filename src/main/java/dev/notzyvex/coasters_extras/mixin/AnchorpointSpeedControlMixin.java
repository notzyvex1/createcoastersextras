package dev.notzyvex.coasters_extras.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import dev.notzyvex.coasters_extras.LaunchTriggerBehaviour;
import dev.notzyvex.coasters_extras.SendDirectionBehaviour;
import dev.notzyvex.coasters_extras.StationBoostBehaviour;

/**
 * Puts a Create-style speed dial on anchorpoints that carry boost or brake track.
 *
 * <p>Boost and brake curves need somewhere for their target speed to live, and a curve is
 * a bezier connection rather than a block -- there is no block entity to hang a value on
 * and nothing to persist it. The anchorpoints at each end <em>are</em> real blocks, and
 * {@code CoasterAnchorpointBlockEntity} already extends Create's {@code KineticBlockEntity},
 * so Create's own scroll behaviour drops straight on. Their {@code addBehaviours} is empty,
 * so nothing is displaced.
 *
 * <p>Gives the Creative Motor interaction: look at the anchor, click and hold, scroll.
 * Persistence, syncing and rendering all come from Create.
 *
 * <p>The dial only appears when a boost or brake curve is actually attached. Showing a
 * "Track Speed" control on every anchorpoint -- including ones carrying plain coaster
 * track, where it would do nothing -- is just noise.
 */
@Mixin(targets = "dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity",
       remap = false)
public class AnchorpointSpeedControlMixin {

    /**
     * What an untouched dial reads.
     *
     * <p>Zero, because each track reads this differently and zero is the only value that is
     * sensible for all of them. It was 22, which is a fine boost speed but meant a brake
     * "stopped" a coaster at 22 b/s and a station held it for 22 seconds -- both of which
     * look like the track is broken. Each track now supplies its own default when the dial
     * is left at zero, and zero itself still means what you would expect: no boost, a dead
     * stop on a brake, no wait at a station.
     */
    private static final int DEFAULT_SPEED = 0;

    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void coasters_extras$addSpeedDial(List<BlockEntityBehaviour> behaviours,
                                              CallbackInfo ci) {
        SmartBlockEntity self = (SmartBlockEntity) (Object) this;

        // Anonymous so it lives inside this mixin rather than in a class of ours that the
        // target's module would have to resolve.
        CenteredSideValueBoxTransform slot =
                new CenteredSideValueBoxTransform((state, dir) -> dir.getAxis().isHorizontal()) {
                    @Override
                    public boolean shouldRender(LevelAccessor level, BlockPos pos, BlockState state) {
                        return super.shouldRender(level, pos, state)
                                && coasters_extras$hasControllableCurve(level, pos);
                    }

                    @Override
                    public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 hit) {
                        return super.testHit(level, pos, state, hit)
                                && coasters_extras$hasControllableCurve(level, pos);
                    }

                    /**
                     * The anchorpoint only fills half its block -- {@code Shapes.box(0,0,0,1,.5,1)}
                     * for a given facing. Create's centred transform aims at the middle of the
                     * full cube, which lands the dial in the empty half and reads as badly
                     * misaligned. Pull it back onto the solid side.
                     */
                    @Override
                    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
                        Vec3 base = super.getLocalOffset(level, pos, state);
                        if (base == null) return null;

                        Direction facing = coasters_extras$facingOf(state);
                        if (facing == null) return base;

                        // Solid half sits opposite the facing, so step a quarter block that way.
                        // Also raised slightly: the Send dial sits the same distance below, so
                        // the pair straddles the middle of the face instead of one covering the
                        // other.
                        Direction solid = facing.getOpposite();
                        // Three boxes ACROSS the face, not stacked down it: speed on the left,
                        // Send in the middle, Powered on the right. Stacking them vertically
                        // ran out of block -- the anchorpoint is only half a block tall, so the
                        // top box sat in the empty half and the bottom one crowded the floor.
                        // The face is a full block wide, so sideways is where the room is.
                        // "Across the face" comes from the SIDE this box is being drawn on,
                        // not from the block's facing. The block can face up or down -- and
                        // when it did, deriving the axis from facing collapsed all three boxes
                        // onto the same point (and before that, threw outright, since
                        // getClockWise() has no answer for a vertical axis).
                        //
                        // getSide() is the face Create is rendering, and our predicate only
                        // ever allows horizontal ones, so it always has a clockwise.
                        Direction face = getSide();
                        if (face == null || face.getAxis().isVertical()) {
                            return base.add(solid.getStepX() * 0.25,
                                            solid.getStepY() * 0.25,
                                            solid.getStepZ() * 0.25);
                        }
                        Direction across = face.getClockWise();
                        return base.add(solid.getStepX() * 0.25 + across.getStepX() * 0.26,
                                        solid.getStepY() * 0.25,
                                        solid.getStepZ() * 0.25 + across.getStepZ() * 0.26);
                    }
                };

        // ONE value box carrying both numbers, as two rows of the same board -- not two boxes.
        // Create's ValueSettingsBoard takes a list of rows and reports each edit as
        // (row, value), so a second bar costs nothing but a label. Two separate boxes would
        // have to be offset to avoid overlapping, would each need their own hit test, and the
        // player would have to discover the second one existed.
        StationBoostBehaviour dial = new StationBoostBehaviour(
                Component.literal("Track Speed"), self, slot);
        // The scroll/drag RANGE is the full bar (0..200) so a boost or brake SPEED can reach
        // 200. Row 0 on a station means dwell seconds instead, and that is clamped to 60 in
        // StationBoostBehaviour.setValueSettings -- the range here is the wide one, the per-row
        // meaning narrows it. Capping the range at 60 was why boost speed could not exceed 60.
        dial.between(0, StationBoostBehaviour.MAX_LAUNCH)
                .withFormatter(v -> v + " b/s")
                // A curve has an anchorpoint at each end and each carries its own dial.
                // The drive hook reads whichever answers first, so editing the "wrong" end
                // silently did nothing. Setting either now writes both.
                .withCallback(v -> coasters_extras$syncPeers(self, v));
        dial.onLaunchChanged(v -> coasters_extras$syncBoostPeers(self, v));
        // Checked when the board is opened, not now: addBehaviours runs before any curve is
        // attached, and the curve changes as the player builds. Asking at open time is the
        // only way the rows can match what the anchor is actually carrying.
        dial.whenStation(() -> {
            LevelAccessor level = self.getLevel();
            return level != null
                    && coasters_extras$curveMatches(level, self.getBlockPos(), "station_track");
        });
        dial.whenBrake(() -> {
            LevelAccessor level = self.getLevel();
            return level != null
                    && coasters_extras$curveMatches(level, self.getBlockPos(), "brake_track");
        });
        dial.whenSplash(() -> {
            LevelAccessor level = self.getLevel();
            return level != null
                    && coasters_extras$curveMatches(level, self.getBlockPos(), "splash_track");
        });
        dial.whenLaunch(() -> {
            LevelAccessor level = self.getLevel();
            return level != null
                    && coasters_extras$curveMatches(level, self.getBlockPos(), "launch_track");
        });
        dial.whenReverse(() -> {
            LevelAccessor level = self.getLevel();
            return level != null
                    && coasters_extras$curveMatches(level, self.getBlockPos(), "reverse_track");
        });
        // Only titles the board -- a Powered Boost behaves exactly like a Boost otherwise.
        dial.whenPoweredBoost(() -> {
            LevelAccessor level = self.getLevel();
            return level != null
                    && coasters_extras$curveMatches(level, self.getBlockPos(),
                                                    "powered_boost_track");
        });
        dial.value = DEFAULT_SPEED;
        behaviours.add(dial);

        // The Send dial gets its OWN box, on the TOP face.
        //
        // It has to be a separate behaviour to be small: ValueSettingsBoard has one maxValue
        // for every row on it, so while Send was a row on the speed board it was forced to be
        // a 0..200 drag bar. As its own ScrollOptionBehaviour it draws as Create's three-icon
        // picker -- the Mechanical Bearing control.
        //
        // On the SAME horizontal faces as the speed box, sitting BELOW it.
        //
        // It was on the top face first, on the reasoning that a different face cannot overlap.
        // That was true and still wrong: nobody looks at the top of an anchorpoint, so the dial
        // was invisible in practice -- reported as "there is no dial". A control you cannot
        // find is worse than one that is slightly cramped, so both now live on the face you are
        // already looking at when you aim at the block, separated vertically.
        CenteredSideValueBoxTransform sendSlot =
                new CenteredSideValueBoxTransform((state, dir) -> dir.getAxis().isHorizontal()) {
                    @Override
                    public boolean shouldRender(LevelAccessor level, BlockPos pos, BlockState state) {
                        return super.shouldRender(level, pos, state)
                                && coasters_extras$hasControllableCurve(level, pos);
                    }

                    @Override
                    public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 hit) {
                        return super.testHit(level, pos, state, hit)
                                && coasters_extras$hasControllableCurve(level, pos);
                    }

                    @Override
                    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
                        Vec3 base = super.getLocalOffset(level, pos, state);
                        if (base == null) return null;
                        Direction facing = coasters_extras$facingOf(state);
                        if (facing == null) return base;
                        Direction solid = facing.getOpposite();
                        // The middle of the three. Speed sits to one side of it and
                        // Powered to the other.
                        return base.add(solid.getStepX() * 0.25,
                                        solid.getStepY() * 0.25,
                                        solid.getStepZ() * 0.25);
                    }
                };

        SendDirectionBehaviour send =
                new SendDirectionBehaviour(Component.literal("Send"), self, sendSlot);
        send.onChanged(v -> coasters_extras$syncSendPeers(self, v));
        behaviours.add(send);

        // The third dial: "Powered", on the three tracks that DO something continuously.
        //
        // Deliberately not gated on hasControllableCurve like the other two. That predicate is
        // "does this anchor carry any curve of ours", and a third box on every anchorpoint in
        // the world would be clutter. A station already has its own redstone hold and a
        // Powered Boost is gated by definition, so neither needs it; the three here are the
        // ones that otherwise run whenever a cart touches them.
        //
        // Sits below the Send box, at the same 0.16 step. The anchorpoint's solid half is the
        // bottom half of the block, so stepping down twice lands at roughly its middle -- still
        // on the face, and further from the empty top half than the speed box is.
        CenteredSideValueBoxTransform launchSlot =
                new CenteredSideValueBoxTransform((state, dir) -> dir.getAxis().isHorizontal()) {
                    @Override
                    public boolean shouldRender(LevelAccessor level, BlockPos pos, BlockState state) {
                        return super.shouldRender(level, pos, state)
                                && coasters_extras$curveMatches(level, pos, "launch_track", "brake_track",
                                                         "boost_track");
                    }

                    @Override
                    public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 hit) {
                        return super.testHit(level, pos, state, hit)
                                && coasters_extras$curveMatches(level, pos, "launch_track", "brake_track",
                                                         "boost_track");
                    }

                    @Override
                    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
                        Vec3 base = super.getLocalOffset(level, pos, state);
                        if (base == null) return null;
                        Direction facing = coasters_extras$facingOf(state);
                        if (facing == null) return base;
                        Direction solid = facing.getOpposite();
                        // "Across the face" comes from the SIDE this box is being drawn on,
                        // not from the block's facing. The block can face up or down -- and
                        // when it did, deriving the axis from facing collapsed all three boxes
                        // onto the same point (and before that, threw outright, since
                        // getClockWise() has no answer for a vertical axis).
                        //
                        // getSide() is the face Create is rendering, and our predicate only
                        // ever allows horizontal ones, so it always has a clockwise.
                        Direction face = getSide();
                        if (face == null || face.getAxis().isVertical()) {
                            return base.add(solid.getStepX() * 0.25,
                                            solid.getStepY() * 0.25,
                                            solid.getStepZ() * 0.25);
                        }
                        Direction across = face.getClockWise();
                        return base.add(solid.getStepX() * 0.25 - across.getStepX() * 0.26,
                                        solid.getStepY() * 0.25,
                                        solid.getStepZ() * 0.25 - across.getStepZ() * 0.26);
                    }
                };

        LaunchTriggerBehaviour launchOn =
                new LaunchTriggerBehaviour(Component.literal("Powered"), self, launchSlot);
        launchOn.onChanged(v -> coasters_extras$syncLaunchTriggerPeers(self, v));
        behaviours.add(launchOn);
    }

    /** Copies this anchor's Launch On setting to the anchorpoint at the far end of each curve. */
    private static void coasters_extras$syncLaunchTriggerPeers(SmartBlockEntity self, int value) {
        if (coasters_extras$syncing) return;
        coasters_extras$syncing = true;
        try {
            LevelAccessor level = self.getLevel();
            if (level == null) return;
            Object view = self.getClass().getMethod("getAnchorPeerCurvesView").invoke(self);
            if (!(view instanceof Map<?, ?> curves)) return;

            for (Object key : curves.keySet()) {
                if (!(key instanceof BlockPos peer)) continue;
                LaunchTriggerBehaviour other =
                        BlockEntityBehaviour.get(level, peer, LaunchTriggerBehaviour.TYPE);
                if (other == null || other.value == value) continue;
                other.value = value;
                other.blockEntity.setChanged();
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendBlockUpdated(peer, sl.getBlockState(peer), sl.getBlockState(peer), 3);
                }
            }
        } catch (Throwable ignored) {
            // internal API; a failed sync should not break the dial itself
        } finally {
            coasters_extras$syncing = false;
        }
    }

    /** Guards against the two ends setting each other back and forth forever. */
    private static boolean coasters_extras$syncing = false;

    /** Copies this anchor's dial to the anchorpoint at the far end of each curve. */
    private static void coasters_extras$syncPeers(SmartBlockEntity self, int value) {
        if (coasters_extras$syncing) return;
        coasters_extras$syncing = true;
        try {
            LevelAccessor level = self.getLevel();
            if (level == null) return;
            Object view = self.getClass().getMethod("getAnchorPeerCurvesView").invoke(self);
            if (!(view instanceof Map<?, ?> curves)) return;

            for (Object key : curves.keySet()) {
                if (!(key instanceof BlockPos peer)) continue;
                StationBoostBehaviour other =
                        BlockEntityBehaviour.get(level, peer, StationBoostBehaviour.TYPE);
                if (other == null || other.value == value) continue;
                other.value = value;
                other.blockEntity.setChanged();
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendBlockUpdated(peer, sl.getBlockState(peer), sl.getBlockState(peer), 3);
                }
            }
        } catch (Throwable ignored) {
            // internal API; a failed sync should not break the dial itself
        } finally {
            coasters_extras$syncing = false;
        }
    }

    /** The anchorpoint's FACING, read by name so we do not link against their block class. */
    private static Direction coasters_extras$facingOf(BlockState state) {
        for (net.minecraft.world.level.block.state.properties.Property<?> p
                : state.getProperties()) {
            if ("facing".equals(p.getName()) && state.getValue(p) instanceof Direction d) {
                return d;
            }
        }
        return null;
    }

    /**
     * True if any curve on this anchor is one of ours that has something to configure.
     *
     * <p>Both value boxes -- speed and Send -- render only when this is true, so a track missing
     * from this list has NO dial at all, however carefully its label and behaviour were written.
     * Launch, Reverse and Splash were exactly that: each had a dial label, a board and drive code
     * reading the setting, and none of them rendered a box to set it with. That is what "the
     * Send dial does not show up" was.
     *
     * <p>Add a track here the moment it starts reading a dial, or its setting is unreachable.
     */
    private static boolean coasters_extras$hasControllableCurve(LevelAccessor level, BlockPos pos) {
        return coasters_extras$curveMatches(level, pos,
                "boost_track", "brake_track", "station_track",
                "launch_track", "reverse_track", "splash_track",
                "powered_boost_track");
    }

    /** True if any curve on this anchor is one of ours with one of the given paths. */
    private static boolean coasters_extras$curveMatches(LevelAccessor level, BlockPos pos,
                                                        String... paths) {
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) return false;

            Object view = be.getClass().getMethod("getAnchorPeerCurvesView").invoke(be);
            if (!(view instanceof Map<?, ?> curves)) return false;

            for (Object c : curves.values()) {
                if (!(c instanceof BezierConnection bc) || bc.getMaterial() == null) continue;
                var id = bc.getMaterial().id;
                if (!"coasters_extras".equals(id.getNamespace())) continue;
                String p = id.getPath();
                for (String want : paths) {
                    if (want.equals(p)) return true;
                }
            }
        } catch (Throwable ignored) {
            // internal API; a failure here should hide the dial, not crash rendering
        }
        return false;
    }

    /** Guards the boost dial's peer sync, same as the speed dial's. */
    private static boolean coasters_extras$syncingBoost = false;

    /**
     * Copies this anchor's boost dial to the anchorpoint at the far end of each curve.
     *
     * <p>Same reason as the speed dial: a station curve has an anchorpoint at each end, the
     * drive code reads whichever answers first, and without this, setting the dial on the
     * "wrong" end of your own station does nothing at all.
     */
    private static void coasters_extras$syncBoostPeers(SmartBlockEntity self, int value) {
        if (coasters_extras$syncingBoost) return;
        coasters_extras$syncingBoost = true;
        try {
            LevelAccessor level = self.getLevel();
            if (level == null) return;
            Object view = self.getClass().getMethod("getAnchorPeerCurvesView").invoke(self);
            if (!(view instanceof Map<?, ?> curves)) return;

            for (Object key : curves.keySet()) {
                if (!(key instanceof BlockPos peer)) continue;
                StationBoostBehaviour other =
                        BlockEntityBehaviour.get(level, peer, StationBoostBehaviour.TYPE);
                // launch, not value -- this syncs row 1. Copying `value` here would overwrite
                // the peer's hold time with a launch speed.
                if (other == null || other.launch == value) continue;
                other.launch = value;
                other.blockEntity.setChanged();
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendBlockUpdated(peer, sl.getBlockState(peer), sl.getBlockState(peer), 3);
                }
            }
        } catch (Throwable ignored) {
            // internal API; a failed sync should not break the dial itself
        } finally {
            coasters_extras$syncingBoost = false;
        }
    }

    /** Guards the direction dial's peer sync. */
    private static boolean coasters_extras$syncingDirection = false;

    /**
     * Copies this anchor's departure direction to the anchorpoint at the far end of each curve.
     *
     * <p>Copied straight, NOT mirrored. Forward means "along the curve's tangent", and a curve
     * has one tangent no matter which end you stand at -- so both anchorpoints holding the same
     * number describe the same world direction. Mirroring here would make the two ends disagree
     * about which way the ride leaves, which is the opposite of what this sync is for.
     *
     * <p>The value written is the RAW bar position, so the far anchor's board reopens with its
     * handle in the same place rather than snapped to the bottom of the matching zone.
     */
    /**
     * Copies this anchorpoint's Send choice to the anchorpoint at the far end of each curve.
     *
     * <p>Both ends describe the same curve and the drive code reads whichever answers first, so
     * letting them disagree means editing the "wrong" end silently does nothing -- the same bug
     * the speed sync exists to prevent.
     */
    private static void coasters_extras$syncSendPeers(SmartBlockEntity self, int index) {
        if (coasters_extras$syncingDirection) return;
        coasters_extras$syncingDirection = true;
        try {
            LevelAccessor level = self.getLevel();
            if (level == null) return;

            Object view = self.getClass().getMethod("getAnchorPeerCurvesView").invoke(self);
            if (!(view instanceof Map<?, ?> curves)) return;

            for (Object key : curves.keySet()) {
                if (!(key instanceof BlockPos peer)) continue;
                SendDirectionBehaviour other =
                        BlockEntityBehaviour.get(level, peer, SendDirectionBehaviour.TYPE);
                if (other == null || other.value == index) continue;
                other.value = index;
                other.blockEntity.setChanged();
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendBlockUpdated(peer, sl.getBlockState(peer), sl.getBlockState(peer), 3);
                }
            }
        } catch (Throwable ignored) {
            // internal API; a failed sync should not break the dial itself
        } finally {
            coasters_extras$syncingDirection = false;
        }
    }
}
