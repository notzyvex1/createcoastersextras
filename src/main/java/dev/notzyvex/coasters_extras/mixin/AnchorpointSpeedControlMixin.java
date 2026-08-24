package dev.notzyvex.coasters_extras.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
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

@Mixin(targets = "dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity",
       remap = false)
public class AnchorpointSpeedControlMixin {

    private static final int DEFAULT_SPEED = 0;

    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void coasters_extras$addSpeedDial(List<BlockEntityBehaviour> behaviours,
                                              CallbackInfo ci) {
        SmartBlockEntity self = (SmartBlockEntity) (Object) this;

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

                    @Override
                    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
                        Vec3 base = super.getLocalOffset(level, pos, state);
                        if (base == null) return null;

                        Direction facing = coasters_extras$facingOf(state);
                        if (facing == null) return base;

                        Direction solid = facing.getOpposite();
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

        StationBoostBehaviour dial = new StationBoostBehaviour(
                Component.literal("Track Speed"), self, slot);
        dial.between(0, StationBoostBehaviour.MAX_LAUNCH)
                .withFormatter(v -> v + " b/s")
                .withCallback(v -> coasters_extras$syncPeers(self, v));
        dial.onLaunchChanged(v -> coasters_extras$syncBoostPeers(self, v));
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
        dial.whenPoweredBoost(() -> {
            LevelAccessor level = self.getLevel();
            return level != null
                    && coasters_extras$curveMatches(level, self.getBlockPos(),
                                                    "powered_boost_track");
        });
        dial.value = DEFAULT_SPEED;
        behaviours.add(dial);

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
                        return base.add(solid.getStepX() * 0.25,
                                        solid.getStepY() * 0.25,
                                        solid.getStepZ() * 0.25);
                    }
                };

        SendDirectionBehaviour send =
                new SendDirectionBehaviour(Component.literal("Send"), self, sendSlot);
        send.onChanged(v -> coasters_extras$syncSendPeers(self, v));
        behaviours.add(send);

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
        } finally {
            coasters_extras$syncing = false;
        }
    }

    private static boolean coasters_extras$syncing = false;

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
        } finally {
            coasters_extras$syncing = false;
        }
    }

    private static Direction coasters_extras$facingOf(BlockState state) {
        for (net.minecraft.world.level.block.state.properties.Property<?> p
                : state.getProperties()) {
            if ("facing".equals(p.getName()) && state.getValue(p) instanceof Direction d) {
                return d;
            }
        }
        return null;
    }

    private static boolean coasters_extras$hasControllableCurve(LevelAccessor level, BlockPos pos) {
        return coasters_extras$curveMatches(level, pos,
                "boost_track", "brake_track", "station_track",
                "launch_track", "reverse_track", "splash_track",
                "powered_boost_track");
    }

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
        }
        return false;
    }

    private static boolean coasters_extras$syncingBoost = false;

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
                if (other == null || other.launch == value) continue;
                other.launch = value;
                other.blockEntity.setChanged();
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendBlockUpdated(peer, sl.getBlockState(peer), sl.getBlockState(peer), 3);
                }
            }
        } catch (Throwable ignored) {
        } finally {
            coasters_extras$syncingBoost = false;
        }
    }

    private static boolean coasters_extras$syncingDirection = false;

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
        } finally {
            coasters_extras$syncingDirection = false;
        }
    }
}
