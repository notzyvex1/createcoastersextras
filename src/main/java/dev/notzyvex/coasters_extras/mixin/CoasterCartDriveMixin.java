package dev.notzyvex.coasters_extras.mixin;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLiftDrive;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.notzyvex.coasters_extras.Config;
import dev.notzyvex.coasters_extras.control.CoasterControls;
import dev.notzyvex.coasters_extras.control.CoasterControlsBlock;
import dev.notzyvex.coasters_extras.net.BrakingTracker;
import dev.notzyvex.coasters_extras.net.StationPayload;
import dev.notzyvex.coasters_extras.net.RideTelemetry;
import dev.notzyvex.coasters_extras.net.SensorTracker;
import dev.notzyvex.coasters_extras.net.StationTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.notzyvex.coasters_extras.StationBoostBehaviour;

@Mixin(targets = "dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrackSnap",
       remap = false)
public class CoasterCartDriveMixin {

    private static final java.util.Map<Integer, Double> STATION_DIR = new java.util.HashMap<>();

    private static final java.util.Map<String, Long> STATION_ARRESTED = new java.util.HashMap<>();
    private static final java.util.Map<String, Long> STATION_DISPATCH = new java.util.HashMap<>();
    private static final java.util.Map<String, Long> STATION_SEEN = new java.util.HashMap<>();

    private static final java.util.Map<String, java.util.Map<Integer, Long>> STATION_ENTERED =
            new java.util.HashMap<>();

    private static final java.util.Map<Integer, Long> SPLASH_SOUND_LAST = new java.util.HashMap<>();

    private static final java.util.Map<Integer, Vector3d> BOBSLED_PREV_FWD =
            new java.util.HashMap<>();

    private static final java.util.Map<Integer, Long> REVERSE_LAST = new java.util.HashMap<>();

    @Redirect(
            method = "onPrePhysicsTick(Ldev/ryanhcode/sable/neoforge/event/ForgeSablePrePhysicsTickEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/silvergold/simulatedcoasters/track/cart/CoasterCartChainLiftDrive;"
                           + "applySpeedMatch(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;"
                           + "Lnet/minecraft/server/level/ServerLevel;"
                           + "Ldev/ryanhcode/sable/sublevel/system/SubLevelPhysicsSystem;"
                           + "Ldev/silvergold/simulatedcoasters/track/graph/CoasterPathTrackFrame$GraphHit;"
                           + "Ldev/silvergold/simulatedcoasters/track/graph/CoasterPathTrackFrame$TrackBasis;D)V"
            ),
            require = 1
    )
    private static void coasters_extras$drive(ServerSubLevel sub, ServerLevel level,
                                              SubLevelPhysicsSystem physics,
                                              CoasterPathTrackFrame.GraphHit graphHit,
                                              CoasterPathTrackFrame.TrackBasis basis,
                                              double dt) {
        CoasterCartChainLiftDrive.applySpeedMatch(sub, level, physics, graphHit, basis, dt);

        try {
            coasters_extras$applyTrackEffect(sub, level, physics, graphHit, basis, dt);
        } catch (Throwable t) {
        }
    }

    private static double coasters_extras$speedFromAnchors(ServerLevel level,
                                                           CoasterPathTrackFrame.GraphHit hit) {
        for (BlockPos pos : new BlockPos[]{ hit.edge().from(), hit.edge().to() }) {
            if (pos == null) continue;
            StationBoostBehaviour dial =
                    BlockEntityBehaviour.get(level, pos, StationBoostBehaviour.TYPE);
            if (dial != null) return dial.value;
        }
        return -1;
    }

    private static double coasters_extras$boostFromAnchors(ServerLevel level,
                                                           CoasterPathTrackFrame.GraphHit hit) {
        int best = 0;
        for (BlockPos pos : new BlockPos[]{ hit.edge().from(), hit.edge().to() }) {
            if (pos == null) continue;
            StationBoostBehaviour dial =
                    BlockEntityBehaviour.get(level, pos, StationBoostBehaviour.TYPE);
            if (dial != null && dial.launch > best) best = dial.launch;
        }
        return best;
    }

    private static int coasters_extras$directionFromAnchors(ServerLevel level,
                                                            CoasterPathTrackFrame.GraphHit hit) {
        for (BlockPos pos : new BlockPos[]{ hit.edge().from(), hit.edge().to() }) {
            if (pos == null) continue;
            dev.notzyvex.coasters_extras.SendDirectionBehaviour send =
                    BlockEntityBehaviour.get(level, pos,
                            dev.notzyvex.coasters_extras.SendDirectionBehaviour.TYPE);
            if (send == null) continue;
            int sign = send.sign();
            if (sign != 0) return sign;
        }
        return 0;
    }

    private static boolean coasters_extras$requiresSignal(
            ServerLevel level, CoasterPathTrackFrame.GraphHit hit) {
        for (BlockPos pos : new BlockPos[]{ hit.edge().from(), hit.edge().to() }) {
            if (pos == null) continue;
            dev.notzyvex.coasters_extras.LaunchTriggerBehaviour trigger =
                    BlockEntityBehaviour.get(level, pos,
                            dev.notzyvex.coasters_extras.LaunchTriggerBehaviour.TYPE);
            if (trigger != null && trigger.needsSignal()) return true;
        }
        return false;
    }

    private static BlockPos coasters_extras$anchorAlong(CoasterPathTrackFrame.GraphHit hit,
                                                        Vector3d tangent, int sign) {
        BlockPos a = hit.edge().from();
        BlockPos b = hit.edge().to();
        if (a == null) return b;
        if (b == null) return a;

        Vec3 heading = new Vec3(tangent.x * sign, tangent.y * sign, tangent.z * sign);
        Vec3 mid = Vec3.atCenterOf(a).add(Vec3.atCenterOf(b)).scale(0.5);
        return Vec3.atCenterOf(a).subtract(mid).dot(heading)
             > Vec3.atCenterOf(b).subtract(mid).dot(heading) ? a : b;
    }

    private static BlockPos coasters_extras$fartherAnchor(CoasterPathTrackFrame.GraphHit hit,
                                                          Vec3 cart) {
        BlockPos a = hit.edge().from();
        BlockPos b = hit.edge().to();
        if (a == null) return b;
        if (b == null) return a;
        return cart.distanceToSqr(Vec3.atCenterOf(a)) >= cart.distanceToSqr(Vec3.atCenterOf(b))
                ? a : b;
    }

    private static BlockPos coasters_extras$exitAnchor(CoasterPathTrackFrame.GraphHit hit,
                                                       Vec3 cart, Vector3d tangent, double dir) {
        Vec3 heading = new Vec3(tangent.x * dir, tangent.y * dir, tangent.z * dir);
        BlockPos best = null;
        double bestDot = 0.0;
        for (BlockPos pos : new BlockPos[]{ hit.edge().from(), hit.edge().to() }) {
            if (pos == null) continue;
            double d = Vec3.atCenterOf(pos).subtract(cart).dot(heading);
            if (d > bestDot) {
                bestDot = d;
                best = pos;
            }
        }
        return best;
    }

    private static final java.util.Map<java.util.UUID, Vec3> DRIVER_HEADING =
            new java.util.HashMap<>();

    private static void coasters_extras$applyDriver(ServerLevel level, PhysicsPipeline pipeline,
                                                    ServerSubLevel sub,
                                                    CoasterPathTrackFrame.GraphHit hit,
                                                    Vector3d tangent, double current, double dt) {
        Vec3 at = hit.point();
        net.minecraft.server.level.ServerPlayer driver = null;
        double nearest = Config.driverRangeSq();

        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            CoasterControls.dropIfNotRiding(p);
            if (!p.isPassenger()) continue;
            if (!CoasterControls.isDriving(p)) continue;
            double d = p.position().distanceToSqr(at);
            if (d < nearest) {
                nearest = d;
                driver = p;
            }
        }
        if (driver == null) return;

        float input = driver.zza;

        if (((LivingEntityJumpAccessor) driver).coasters_extras$isJumping()) {
            coasters_extras$throwLever(level, driver, 0F);
            if (Math.abs(current) > 1.0E-4) {
                double step = Math.min(Math.abs(current), Config.driverHandbrake() * dt)
                        * -Math.signum(current);
                pipeline.addLinearAndAngularVelocity(sub, new Vector3d(tangent).mul(step),
                        new Vector3d());
            }
            return;
        }

        if (Math.abs(input) < 0.05F) {
            coasters_extras$throwLever(level, driver, 0F);
            return;
        }

        Vec3 remembered = DRIVER_HEADING.get(driver.getUUID());
        double facing;
        if (Math.abs(current) > Config.driverStoppedSpeed()) {
            facing = Math.signum(current);
            DRIVER_HEADING.put(driver.getUUID(),
                    new Vec3(tangent.x * facing, tangent.y * facing, tangent.z * facing));
        } else if (remembered != null) {
            double along = remembered.x * tangent.x + remembered.y * tangent.y
                    + remembered.z * tangent.z;
            facing = along >= 0 ? 1.0 : -1.0;
        } else {
            Vec3 look = driver.getLookAngle();
            facing = look.x * tangent.x + look.y * tangent.y + look.z * tangent.z >= 0
                    ? 1.0 : -1.0;
        }

        double desired = input * facing * Config.driverMaxSpeed();
        double delta = desired - current;
        if (Math.abs(delta) < 1.0E-4) return;

        boolean slowing = Math.abs(desired) < Math.abs(current)
                || Math.signum(desired) != Math.signum(current);
        double rate = slowing ? Config.driverBrake() : Config.driverAcceleration();

        double step = Math.min(Math.abs(delta), rate * dt) * Math.signum(delta);
        pipeline.addLinearAndAngularVelocity(sub, new Vector3d(tangent).mul(step),
                new Vector3d());

        coasters_extras$throwLever(level, driver, input);
    }

    private static void coasters_extras$throwLever(ServerLevel level,
                                                   net.minecraft.server.level.ServerPlayer driver,
                                                   float input) {
        BlockPos stand = CoasterControls.stand(driver);
        if (stand == null) return;
        int want = input > 0.05F ? 2 : input < -0.05F ? 0 : 1;
        var state = level.getBlockState(stand);
        if (!(state.getBlock() instanceof CoasterControlsBlock)) return;
        if (state.getValue(CoasterControlsBlock.THROTTLE) == want) return;
        level.setBlock(stand, state.setValue(CoasterControlsBlock.THROTTLE, want), 3);
    }

    private static String coasters_extras$stationKey(CoasterPathTrackFrame.GraphHit hit) {
        BlockPos a = hit.edge().from();
        BlockPos b = hit.edge().to();
        long x = a == null ? 0 : a.asLong();
        long y = b == null ? 0 : b.asLong();
        return Math.min(x, y) + "/" + Math.max(x, y);
    }

    private static void coasters_extras$forgetIdleStations(long now) {
        if (now % 200 != 0) return;
        final long forgetAfter = Config.stationForgetAfterTicks();
        STATION_SEEN.entrySet().removeIf(e -> {
            if (now - e.getValue() <= forgetAfter) return false;
            STATION_ARRESTED.remove(e.getKey());
            STATION_DISPATCH.remove(e.getKey());
            STATION_ENTERED.remove(e.getKey());
            return true;
        });
    }

    private static boolean coasters_extras$anchorPowered(ServerLevel level,
                                                         CoasterPathTrackFrame.GraphHit hit) {
        for (BlockPos pos : new BlockPos[]{ hit.edge().from(), hit.edge().to() }) {
            if (pos != null && level.hasNeighborSignal(pos)) return true;
        }
        return false;
    }

    private static void coasters_extras$applyTrackEffect(ServerSubLevel sub, ServerLevel level,
                                                         SubLevelPhysicsSystem physics,
                                                         CoasterPathTrackFrame.GraphHit graphHit,
                                                         CoasterPathTrackFrame.TrackBasis basis,
                                                         double dt) {
        if (dt <= 0 || graphHit == null || graphHit.edge() == null
                || graphHit.edge().bezier() == null) {
            return;
        }
        Vec3 fwd = basis.forward();
        if (fwd.lengthSqr() < 1.0E-12) {
            return;
        }
        Vector3d tangent = new Vector3d(fwd.x, fwd.y, fwd.z).normalize();

        PhysicsPipeline pipeline = physics.getPipeline();
        Vector3d vel = pipeline.getLinearVelocity(sub, new Vector3d());
        double current = vel.dot(tangent);
        double speed = Math.abs(current);

        RideTelemetry.report(graphHit.edge().from(), graphHit.edge().to(),
                sub.getRuntimeId(), speed, level.getGameTime());

        coasters_extras$applyDriver(level, pipeline, sub, graphHit, tangent, current, dt);

        ResourceLocation id = graphHit.edge().bezier().getMaterial().id;
        if (!"coasters_extras".equals(id.getNamespace())) {
            return;
        }
        String kind = id.getPath();

        double dialled = coasters_extras$speedFromAnchors(level, graphHit);

        switch (kind) {
            case "boost_track", "powered_boost_track" -> {
                boolean poweredKind = kind.equals("powered_boost_track");
                if ((poweredKind || coasters_extras$requiresSignal(level, graphHit))
                        && !coasters_extras$anchorPowered(level, graphHit)) {
                    return;
                }
                boolean gated = poweredKind;
                int boostDir = coasters_extras$directionFromAnchors(level, graphHit);
                double dir = boostDir != 0 ? boostDir : (current >= 0 ? 1 : -1);
                double target = dialled > 0 ? dialled
                        : (gated ? Config.poweredBoostSpeed() : Config.boostSpeed());
                if (speed >= target) return;
                double accel = gated ? Config.poweredBoostAcceleration()
                                     : Config.boostAcceleration();
                double delta = Math.min(accel * dt, target - speed) * dir;
                pipeline.addLinearAndAngularVelocity(sub, new Vector3d(tangent).mul(delta),
                        new Vector3d());

                double f = Math.min(1.0, (target - speed) / Math.max(target, 1.0));
                Vec3 p = graphHit.point();

                double ex = -tangent.x * dir, ey = -tangent.y * dir, ez = -tangent.z * dir;
                if (level.getGameTime() % 2 != 0) return;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        p.x, p.y + 0.22, p.z, 0, ex, ey + 0.25, ez, 0.22 + f * 0.3);
                if (f > 0.6) {
                    level.sendParticles(ParticleTypes.FLAME,
                            p.x, p.y + 0.18, p.z, 0, ex, ey + 0.1, ez, 0.15 + f * 0.2);
                }
            }
            case "station_track" -> {
                int cartId = sub.getRuntimeId();
                double dwell = dialled > 0 ? dialled : Config.stationDwellSeconds();
                int dwellTicks = Math.max(1, (int) (dwell * 20));
                boolean held = coasters_extras$anchorPowered(level, graphHit);
                long now = level.getGameTime();
                Vec3 p = graphHit.point();

                double dir;
                boolean knowsHeading = true;
                if (speed > Config.stationStoppedSpeed()) {
                    dir = current >= 0 ? 1 : -1;
                    STATION_DIR.put(cartId, dir);
                } else {
                    Double remembered = STATION_DIR.get(cartId);
                    knowsHeading = remembered != null;
                    dir = remembered != null ? remembered : 1.0;
                }

                int dialledDirection = coasters_extras$directionFromAnchors(level, graphHit);
                BlockPos exitAnchor;
                if (dialledDirection != 0) {
                    exitAnchor = coasters_extras$anchorAlong(graphHit, tangent, dialledDirection);
                } else if (knowsHeading) {
                    exitAnchor = coasters_extras$exitAnchor(graphHit, p, tangent, dir);
                } else {
                    exitAnchor = coasters_extras$fartherAnchor(graphHit, p);
                }
                BlockPos entryAnchor = exitAnchor == null ? graphHit.edge().from()
                        : (exitAnchor.equals(graphHit.edge().from()) ? graphHit.edge().to()
                                                                    : graphHit.edge().from());
                BlockPos reportB = exitAnchor == null ? graphHit.edge().to() : exitAnchor;
                double remaining = exitAnchor == null
                        ? 0.0 : p.distanceTo(Vec3.atCenterOf(exitAnchor));

                String station = coasters_extras$stationKey(graphHit);
                STATION_SEEN.put(station, now);
                java.util.Map<Integer, Long> entered =
                        STATION_ENTERED.computeIfAbsent(station, k -> new java.util.HashMap<>());
                Long firstSeen = entered.putIfAbsent(cartId, now);
                long myArrival = firstSeen == null ? now : firstSeen;
                coasters_extras$forgetIdleStations(now);

                Long dispatchedAt = STATION_DISPATCH.get(station);
                if (dispatchedAt != null) {
                    if (now - dispatchedAt > Config.stationDispatchWindowTicks()) {
                        STATION_DISPATCH.remove(station);
                    } else {
                        int dialledDir = coasters_extras$directionFromAnchors(level, graphHit);
                        double out = dialledDir != 0
                                ? dialledDir
                                : STATION_DIR.getOrDefault(cartId, dir);
                        double dialledBoost = coasters_extras$boostFromAnchors(level, graphHit);
                        double launch = dialledBoost > 0
                                ? dialledBoost
                                : Config.stationLaunchSpeed();
                        if (speed < launch) {
                            double push = Math.min(
                                    Config.stationLaunchRate() * dt,
                                    launch - speed);
                            pipeline.addLinearAndAngularVelocity(sub,
                                    new Vector3d(tangent).mul(push * out), new Vector3d());
                        }
                        StationTracker.report(entryAnchor, reportB,
                                StationPayload.STATE_LEAVING, 0, dwellTicks, now);
                        return;
                    }
                }

                double catchMax = Config.stationMaxDeceleration();
                double stoppable = Math.sqrt(2.0 * catchMax
                                             * Math.max(remaining, 0.05));
                if (speed > stoppable) {
                    pipeline.addLinearAndAngularVelocity(sub,
                            new Vector3d(tangent).mul(-(speed - stoppable) * dir),
                            new Vector3d());
                    speed = stoppable;
                    current = stoppable * dir;
                }

                Long arrestedAt = STATION_ARRESTED.get(station);

                boolean newArrival = arrestedAt != null && myArrival > arrestedAt;

                if ((arrestedAt == null || newArrival)
                        && remaining > Config.stationArriveDistance()) {
                    double approachDecel = Config.stationApproachDeceleration();
                    double creepSpeed = Config.stationCreepSpeed();
                    double allowed = Math.sqrt(2.0 * approachDecel * remaining);

                    if (speed > allowed) {
                        double rate = Math.min(Math.max(speed * speed / (2.0 * remaining),
                                                        approachDecel),
                                               catchMax);
                        double delta = -Math.min(rate * dt, speed) * dir;
                        pipeline.addLinearAndAngularVelocity(sub,
                                new Vector3d(tangent).mul(delta), new Vector3d());
                        if (speed > Config.stationBrakeEffectSpeed() && now % 3 == 0) {
                            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.25, p.z,
                                    2, 0.15, 0.05, 0.15, 0.01);
                            level.playSound(null, p.x, p.y, p.z, SoundEvents.PISTON_CONTRACT,
                                    SoundSource.BLOCKS, 0.35F, 1.4F);
                        }
                    } else if (speed < creepSpeed) {
                        double push = Math.min(Config.stationCreepAcceleration() * dt,
                                               creepSpeed - speed);
                        pipeline.addLinearAndAngularVelocity(sub,
                                new Vector3d(tangent).mul(push * dir), new Vector3d());
                    }
                    StationTracker.report(entryAnchor, reportB,
                            StationPayload.STATE_ARRIVING, 0, dwellTicks, now);
                    return;
                }

                if (arrestedAt == null) {
                    arrestedAt = now;
                    STATION_ARRESTED.put(station, now);
                    level.playSound(null, p.x, p.y, p.z, SoundEvents.IRON_DOOR_CLOSE,
                            SoundSource.BLOCKS, 0.45F, 1.6F);
                }

                if (Math.abs(current) > 1.0E-3) {
                    pipeline.addLinearAndAngularVelocity(sub,
                            new Vector3d(tangent).mul(-current), new Vector3d());
                }

                if (held) {
                    STATION_ARRESTED.put(station, now);
                    StationTracker.report(entryAnchor, reportB,
                            StationPayload.STATE_HELD, 0, dwellTicks, now);
                    return;
                }

                long waited = now - arrestedAt;
                if (waited < dwellTicks) {
                    StationTracker.report(entryAnchor, reportB, StationPayload.STATE_WAITING,
                            (int) (dwellTicks - waited), dwellTicks, now);
                    return;
                }

                STATION_ARRESTED.remove(station);
                STATION_ENTERED.remove(station);
                STATION_DISPATCH.put(station, now);
                StationTracker.report(entryAnchor, reportB,
                        StationPayload.STATE_LEAVING, 0, dwellTicks, now);
                level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.3, p.z,
                        6, 0.25, 0.1, 0.25, 0.02);
                level.playSound(null, p.x, p.y, p.z, SoundEvents.NOTE_BLOCK_BELL.value(),
                        SoundSource.BLOCKS, 0.6F, 1.2F);
            }
            case "brake_track" -> {
                if (coasters_extras$requiresSignal(level, graphHit)
                        && !coasters_extras$anchorPowered(level, graphHit)) {
                    return;
                }
                double target = dialled >= 0 ? dialled : Config.brakeTargetSpeed();
                if (speed <= target) return;
                double dir = current >= 0 ? 1 : -1;
                double over = speed - target;

                double needed = (over * over) / (2.0 * Config.brakeStopDistance());
                double rate = Math.min(Math.max(Config.brakeDeceleration(), needed),
                                       Config.brakeDecelerationMax());
                double delta = -Math.min(rate * dt, over) * dir;
                pipeline.addLinearAndAngularVelocity(sub, new Vector3d(tangent).mul(delta),
                        new Vector3d());

                if (target <= 0.01 && speed < Config.brakeDeadStopSpeed()) {
                    pipeline.addLinearAndAngularVelocity(sub,
                            new Vector3d(tangent).mul(-(current + delta)), new Vector3d());
                }

                if (over > Config.brakeGlowOverspeed()) {
                    Vec3 wp = graphHit.point();
                    BrakingTracker.report(wp.x, wp.y, wp.z);
                }

                double sparkSpeed = Config.brakeSparkSpeed();
                if (speed > sparkSpeed) {
                    double f = Math.min(1.0, (speed - sparkSpeed)
                                             / (Config.brakeFullEffectSpeed() - sparkSpeed));
                    Vec3 p = graphHit.point();
                    double spread = 0.08 + f * 0.22;
                    long now = level.getGameTime();

                    if (now % 4 == 0) {
                        int lava = 1 + (int) (f * 3);
                        level.sendParticles(ParticleTypes.LAVA, p.x, p.y + 0.2, p.z,
                                lava, spread, spread * 0.5, spread, 0.0);

                        if (f > 0.6) {
                            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.25, p.z,
                                    1 + (int) (f * 2), spread, spread * 0.5, spread, 0.02);
                        }
                        if (f > 0.9) {
                            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.35, p.z,
                                    1, spread, 0.1, spread, 0.01);
                        }
                    }

                    int every = f > 0.6 ? 2 : 4;
                    if (now % every == 0) {
                        level.playSound(null, p.x, p.y, p.z, SoundEvents.LAVA_EXTINGUISH,
                                SoundSource.BLOCKS,
                                (float) (0.30 + f * 0.55),
                                (float) (1.65 + f * 0.35));
                    }
                }
            }
            case "rainbow_track" -> {
                if (level.getGameTime() % 3 != 0) return;

                Vec3 p = graphHit.point();
                float hue = (float) (((p.x + p.z) * Config.rainbowColourScale()) % 1.0);
                if (hue < 0) hue += 1.0f;

                int rgb = java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f);
                DustParticleOptions dust = new DustParticleOptions(
                        new Vector3f(((rgb >> 16) & 0xFF) / 255f,
                                     ((rgb >> 8) & 0xFF) / 255f,
                                     (rgb & 0xFF) / 255f), 1.1f);
                level.sendParticles(dust, p.x, p.y + 0.25, p.z, 3, 0.16, 0.12, 0.16, 0.0);

                int[] pentatonic = { -12, -10, -8, -5, -3, 0, 2, 4, 7, 9, 12 };
                int step = pentatonic[Math.min(pentatonic.length - 1,
                                               (int) (hue * pentatonic.length))];
                float pitch = (float) Math.pow(2.0, step / 12.0);
                level.playSound(null, p.x, p.y, p.z, SoundEvents.NOTE_BLOCK_CHIME.value(),
                        SoundSource.BLOCKS, 0.5F, pitch);
            }
            case "bobsled_track" -> {
                if (speed < Config.bobsledMinSpeed()) return;

                Vec3 upV = basis.up();
                Vec3 rightV = basis.right();
                Vector3d up = new Vector3d(upV.x, upV.y, upV.z);
                Vector3d right = new Vector3d(rightV.x, rightV.y, rightV.z);
                if (up.lengthSquared() < 1.0E-12 || right.lengthSquared() < 1.0E-12) return;
                up.normalize();
                right.normalize();

                int bobCart = sub.getRuntimeId();
                Vector3d prevFwd = BOBSLED_PREV_FWD.put(bobCart, new Vector3d(tangent));
                if (prevFwd == null) return;

                double sweep = Math.atan2(new Vector3d(prevFwd).cross(tangent).dot(up),
                                          prevFwd.dot(tangent));
                double travelled = Math.max(1.0E-6, speed * dt);
                double curvature = sweep / travelled;

                double lateral = speed * speed * curvature;
                double target = Math.atan2(lateral, Config.bobsledGravity())
                        * Config.bobsledBankSign();
                double maxBank = Math.toRadians(Config.bobsledMaxBankDegrees());
                target = Math.max(-maxBank, Math.min(maxBank, target));

                dev.ryanhcode.sable.companion.math.Pose3d pose =
                        pipeline.readPose(sub, new dev.ryanhcode.sable.companion.math.Pose3d());
                Vector3d cartUp = pose.orientation().transform(new Vector3d(0, 1, 0));
                double roll = Math.atan2(cartUp.dot(right), cartUp.dot(up));

                Vector3d angNow = pipeline.getAngularVelocity(sub, new Vector3d());
                double rollRate = angNow.dot(tangent);
                double wantedRate = (target - roll) * Config.bobsledBankStiffness();
                double correction = (wantedRate - rollRate) * Config.bobsledBankDamping();
                pipeline.addLinearAndAngularVelocity(sub, new Vector3d(),
                        new Vector3d(tangent).mul(correction));

                if (BOBSLED_PREV_FWD.size() > 256) {
                    BOBSLED_PREV_FWD.clear();
                }
            }
            case "slippery_track" -> {
                double slipperyMax = Config.slipperyMaxSpeed();
                if (speed < Config.slipperyMinSpeed() || speed >= slipperyMax) return;
                double dir = current >= 0 ? 1 : -1;
                double give = Math.min(Config.slipperyRecoverFraction() * speed * dt,
                                       slipperyMax - speed);
                pipeline.addLinearAndAngularVelocity(sub,
                        new Vector3d(tangent).mul(give * dir), new Vector3d());

                if (speed > 4.0 && level.getGameTime() % 4 == 0) {
                    Vec3 p = graphHit.point();
                    level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 0.25, p.z,
                            1, 0.12, 0.06, 0.12, 0.01);
                }
            }
            case "sensor_track" -> {
                SensorTracker.report(graphHit.edge().from(), graphHit.edge().to(),
                                     level.getGameTime());
                if (level.getGameTime() % 2 == 0) {
                    Vec3 p = graphHit.point();
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.25, p.z,
                            1, 0.05, 0.05, 0.05, 0.0);
                }
            }
            case "splash_track" -> {
                if (speed < Config.splashMinSpeed()) return;

                double dir;
                int splashDirDial = coasters_extras$directionFromAnchors(level, graphHit);
                if (dialled > 0) {
                    dir = splashDirDial != 0 ? splashDirDial : (current >= 0 ? 1 : -1);
                    if (speed < dialled) {
                        double push = Math.min(Config.boostAcceleration() * dt, dialled - speed)
                                * dir;
                        pipeline.addLinearAndAngularVelocity(sub,
                                new Vector3d(tangent).mul(push), new Vector3d());
                    }
                } else {
                    dir = current >= 0 ? 1 : -1;
                    double shed = Math.min(Config.splashDrag() * speed * dt, speed) * dir;
                    pipeline.addLinearAndAngularVelocity(sub,
                            new Vector3d(tangent).mul(-shed), new Vector3d());
                }

                Vector3d right = new Vector3d(tangent).cross(new Vector3d(0, 1, 0));
                if (right.lengthSquared() < 1.0E-6) {
                    right.set(1, 0, 0);
                }
                right.normalize();

                double f = Math.min(1.0, speed / Config.splashFullEffectSpeed());
                long now = level.getGameTime();
                if (now % 2 != 0) return;

                Vec3 p = graphHit.point();

                double[] lateral = { Config.splashSideOffset(), Config.splashRailOffset() };
                double along = Config.splashLengthSpread();
                double under = Config.splashUnderDepth();
                double[] stations = { -along, 0.0, along };
                DustParticleOptions foam = new DustParticleOptions(
                        new Vector3f(0.90f, 0.98f, 1.0f), 1.2f);

                for (int side = -1; side <= 1; side += 2) {
                    for (int lane = 0; lane < lateral.length; lane++) {
                        double lat = lateral[lane] * side;
                        boolean outer = lane == 1;

                        for (double s : stations) {
                            double sx = p.x + right.x * lat + tangent.x * s;
                            double sy = p.y + 0.12 + tangent.y * s;
                            double sz = p.z + right.z * lat + tangent.z * s;

                            level.sendParticles(
                                    dev.notzyvex.coasters_extras.particle.ModParticles
                                            .SPLASH.get(),
                                    sx, sy, sz,
                                    3 + (int) (f * 5), 0.12, 0.14 + f * 0.1, 0.12, 0.0);
                            level.sendParticles(ParticleTypes.FALLING_WATER, sx, sy + 0.1, sz,
                                    1 + (int) (f * 2), 0.16, 0.08, 0.16, 0.0);
                            level.sendParticles(foam, sx, sy + 0.05, sz,
                                    1 + (int) (f * 2), 0.14, 0.05, 0.14, 0.02);

                            if (outer) {
                                level.sendParticles(ParticleTypes.FALLING_WATER,
                                        sx, sy - under, sz,
                                        2 + (int) (f * 4), 0.10, under * 0.5, 0.10, 0.0);
                                level.sendParticles(
                                        dev.notzyvex.coasters_extras.particle.ModParticles
                                                .SPLASH.get(),
                                        sx, sy - under * 0.55, sz,
                                        2 + (int) (f * 3), 0.14, 0.10, 0.14, 0.0);
                            }
                        }
                    }
                }

                int splashCart = sub.getRuntimeId();
                Long lastSound = SPLASH_SOUND_LAST.get(splashCart);
                if (lastSound == null || now - lastSound >= Config.splashSoundCooldownTicks()) {
                    SPLASH_SOUND_LAST.put(splashCart, now);
                    level.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_SPLASH,
                            SoundSource.BLOCKS,
                            (float) (0.45 + f * 0.55),
                            (float) (0.95 + f * 0.25));
                }
                if (now % 200 == 0) {
                    SPLASH_SOUND_LAST.entrySet().removeIf(e -> now - e.getValue() > 200);
                }
            }
            case "launch_track" -> {
                if (coasters_extras$requiresSignal(level, graphHit)
                        && !coasters_extras$anchorPowered(level, graphHit)) {
                    return;
                }
                int launchDir = coasters_extras$directionFromAnchors(level, graphHit);
                double dir = launchDir != 0 ? launchDir : (current >= 0 ? 1 : -1);
                double target = dialled > 0 ? dialled : Config.launchSpeed();
                if (speed >= target) return;
                double delta = Math.min(Config.launchAcceleration() * dt, target - speed) * dir;
                pipeline.addLinearAndAngularVelocity(sub, new Vector3d(tangent).mul(delta),
                        new Vector3d());

                double f = Math.min(1.0, (target - speed) / Math.max(target, 1.0));
                Vec3 p = graphHit.point();
                double ex = -tangent.x * dir, ey = -tangent.y * dir, ez = -tangent.z * dir;
                if (level.getGameTime() % 2 != 0) return;
                level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.2, p.z,
                        0, ex, ey + 0.2, ez, 0.35 + f * 0.45);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.28, p.z,
                        1 + (int) (f * 2), 0.12, 0.06, 0.12, 0.01);
                if (f > 0.5) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.22, p.z,
                            0, ex, ey + 0.25, ez, 0.3 + f * 0.35);
                }
                if (level.getGameTime() % 4 == 0) {
                    level.playSound(null, p.x, p.y, p.z, SoundEvents.PISTON_EXTEND,
                            SoundSource.BLOCKS, (float) (0.35 + f * 0.4), (float) (0.6 + f * 0.2));
                }
            }
            case "reverse_track" -> {
                if (speed < Config.reverseMinSpeed()) return;
                int cartId = sub.getRuntimeId();
                long now = level.getGameTime();
                Long last = REVERSE_LAST.get(cartId);
                if (last != null && now - last < Config.reverseCooldownTicks()) return;
                int wantDir = coasters_extras$directionFromAnchors(level, graphHit);
                double outSign = current >= 0 ? 1 : -1;
                if (wantDir != 0) {
                    if (outSign == wantDir) {
                        return;
                    }
                    outSign = wantDir;
                }

                REVERSE_LAST.put(cartId, now);
                if (now % 200 == 0) {
                    REVERSE_LAST.entrySet().removeIf(e -> now - e.getValue() > 200);
                }

                pipeline.addLinearAndAngularVelocity(sub,
                        new Vector3d(tangent).mul(-2.0 * current), new Vector3d());

                if (dialled > 0 && speed < dialled) {
                    double push = Math.min(Config.boostAcceleration() * dt, dialled - speed)
                            * outSign;
                    pipeline.addLinearAndAngularVelocity(sub,
                            new Vector3d(tangent).mul(push), new Vector3d());
                }

                Vec3 p = graphHit.point();
                level.sendParticles(ParticleTypes.PORTAL, p.x, p.y + 0.3, p.z,
                        14, 0.25, 0.25, 0.25, 0.4);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 0.3, p.z,
                        8, 0.18, 0.18, 0.18, 0.12);
                level.playSound(null, p.x, p.y, p.z, SoundEvents.PISTON_CONTRACT,
                        SoundSource.BLOCKS, 0.6F, 0.8F);
            }
            default -> { }
        }
    }
}
