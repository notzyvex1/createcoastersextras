package dev.notzyvex.coasters_extras.mixin;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLiftDrive;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
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

/**
 * Makes the boost, brake and sensor tracks actually do something.
 *
 * <p>Hooks the one place a cart is driven along track. Their chain lift calls
 * {@code applySpeedMatch(sub, level, physics, graphHit, basis, dt)} from
 * {@code CoasterCartTrackSnap}, and that call site already carries everything needed:
 * the cart, the physics system, where it sits on the graph, and the track tangent.
 * Redirecting it lets the original run untouched and then applies our own effect.
 *
 * <p>The drive maths mirrors theirs deliberately -- project velocity onto the tangent,
 * move it toward a target by at most {@code rate * dt}, then feed it back through
 * {@code addLinearAndAngularVelocity}. Setting velocity directly would fight the physics
 * solver and make carts jitter.
 */
@Mixin(targets = "dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrackSnap",
       remap = false)
public class CoasterCartDriveMixin {

    /*
     * Every tuning number that used to live here is now in Config, and every one of them is
     * read at the point of USE rather than held in a field.
     *
     * A static final was wrong twice over. NeoForge loads the server config AFTER mod
     * construction, so a field initialised from it would have been reading a config that was
     * not loaded yet; and it reloads the file whenever it changes on disk, so even a correct
     * first read would go stale the moment an operator edited it. On top of that javac inlines
     * static final primitives at every use site, so these numbers were baked into the bytecode
     * as literals and could never have been changed at runtime at all -- which is exactly the
     * trap the stationLaunchSpeed comment further down already warned about.
     *
     * ModConfigSpec.ConfigValue.get() caches internally, and NeoForge clears that cache on
     * every reload (ModConfigSpec.afterReload -> resetCaches), so calling a Config getter per
     * physics tick is both cheap and always current. That only holds because nothing in Config
     * calls Builder.worldRestart() or gameRestart(), which would deliberately keep the stale
     * value; do not add either.
     *
     * What is left below is per-cart and per-station runtime state. It is not tuning and has
     * no business in a config file.
     */

    /** Cart runtime id -> which way it was heading, kept for when it is stopped. */
    private static final java.util.Map<Integer, Double> STATION_DIR = new java.util.HashMap<>();

    /*
     * A station is held per CURVE, not per cart.
     *
     * A coaster is a train. Keying the hold on the cart meant every cart decided for itself
     * when to stop, so the moment the leader parked, the ones behind were still rolling and
     * shunted it forward. The whole train has to arrest together: the first cart to reach the
     * far anchorpoint freezes the station, and from then on every cart on that curve is
     * pinned until the station lets go of all of them at once.
     */
    /** Curve -> tick the leading cart reached the far anchorpoint. */
    private static final java.util.Map<String, Long> STATION_ARRESTED = new java.util.HashMap<>();
    /** Curve -> tick it dispatched; every cart on it is under power until the window closes. */
    private static final java.util.Map<String, Long> STATION_DISPATCH = new java.util.HashMap<>();
    /** Curve -> last tick any cart was on it, so abandoned stations are forgotten. */
    private static final java.util.Map<String, Long> STATION_SEEN = new java.util.HashMap<>();

    /**
     * Curve -> cart -> the tick that cart first appeared on it since the last dispatch.
     *
     * <p>Exists to tell a TRAIN apart from a NEW ARRIVAL. The station pins every cart on the
     * curve the moment the leader reaches the platform end, which is right for the carts behind
     * the leader and wrong for a coaster that shows up afterwards -- that one gets frozen on
     * contact wherever it happens to be. Comparing a cart's arrival against the arrest tick is
     * what separates the two.
     */
    private static final java.util.Map<String, java.util.Map<Integer, Long>> STATION_ENTERED =
            new java.util.HashMap<>();

    /** Cart runtime id -> game tick it last played a splash sound, so it is one-shot per pass. */
    private static final java.util.Map<Integer, Long> SPLASH_SOUND_LAST = new java.util.HashMap<>();

    /**
     * Cart runtime id -> the track tangent it saw last tick, so a corner can be measured.
     *
     * <p>A bezier carries no curvature we can ask it for, and the drive hook only ever sees one
     * point on it. Comparing this tick's tangent with last tick's is how tight the corner is.
     */
    private static final java.util.Map<Integer, Vector3d> BOBSLED_PREV_FWD =
            new java.util.HashMap<>();

    /** Cart runtime id -> game tick it was last reversed, so a flip happens once per pass. */
    private static final java.util.Map<Integer, Long> REVERSE_LAST = new java.util.HashMap<>();

    // Named target, not method = "*". The wildcard paired with require = 0 meant that if
    // Mixin failed to match the call site it did nothing AND reported nothing, so the
    // tracks silently had no behaviour. require = 1 makes a bad target a startup error.
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
        // chain lifts still work exactly as before
        CoasterCartChainLiftDrive.applySpeedMatch(sub, level, physics, graphHit, basis, dt);

        try {
            coasters_extras$applyTrackEffect(sub, level, physics, graphHit, basis, dt);
        } catch (Throwable t) {
            // never let our effect break their cart handling
        }
    }

    /** Speed dialled on either anchorpoint of this curve, or -1 if neither can be read. */
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

    /**
     * Station Boost dialled on either anchorpoint of this curve, or 0 if neither is set.
     *
     * <p>Highest of the two rather than first-found, unlike the speed dial above. The two ends
     * are kept in step by the dial's own callback, so they should already agree -- but a
     * station built before this dial existed has one end at zero until it is touched, and
     * taking the first answer would read that zero and silently ignore the end the player
     * actually set.
     */
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

    /**
     * The departure direction dialled on either anchorpoint of this curve.
     *
     * <p>Returns {@code 0} for auto, {@code +1} for forward and {@code -1} for reverse, where
     * forward means along the curve's own tangent. That is a property of the CURVE, not of the
     * anchorpoint you read it from, which is why the two ends store the same number rather than
     * mirrored ones -- "forward" is the same world direction whichever end you set it at.
     *
     * <p>Highest-wins does not apply here the way it does for boost, because reverse is a real
     * choice rather than a larger value: the first end with a non-auto setting is taken.
     */
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

    /**
     * Whether this curve is set to wait for redstone before it does anything.
     *
     * <p>Read by the Launch, Brake and Boost cases. Either anchorpoint answers, same as the
     * Send dial, and the two are kept in step by the dial's own peer sync. If NEITHER has the
     * behaviour -- an anchorpoint saved before the dial existed -- this is false, so every
     * track already built keeps working on contact exactly as it did.
     */
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

    /**
     * The anchorpoint this cart is heading toward, or null if neither is ahead of it.
     *
     * <p>A station curve runs between two anchorpoints and a cart enters at one of them; the
     * one it is travelling toward is the end of the platform, and that is where it has to
     * stop. Picked by projecting the offset to each anchor onto the direction of travel and
     * taking the one in front.
     *
     * <p>Uses block centres rather than the curve's own sampled endpoints, because a
     * {@code BlockPos} is unambiguously world space while a bezier's sampled points may be
     * relative to their owning block entity. A station is a platform rather than a hairpin,
     * so straight-line distance tracks arc length closely enough to brake against.
     */
    /**
     * The anchorpoint further along {@code tangent * sign}, whatever the cart is doing.
     *
     * <p>Unlike {@link #coasters_extras$exitAnchor} this does not look at the cart at all, which
     * is the entire point: it answers "which end of this platform is the forward one" rather
     * than "which end is this cart heading for". A station whose direction has been dialled
     * needs the first question, because a coaster placed on the platform has no heading to read
     * and one that has already rolled past the far anchorpoint must still come back to it
     * rather than settle wherever it stopped.
     *
     * <p>Compared against the curve's midpoint rather than each other, so the two anchorpoints
     * are measured on the same axis and a curve that doubles back cannot pick both.
     */
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

    /**
     * Whichever anchorpoint is further from the cart.
     *
     * <p>The fallback for a cart with no heading at all -- placed on the platform by hand, or
     * drifted on below the stopped-speed threshold. Picking the far end means it travels the
     * length of the station rather than arresting where it happens to sit, which is both what
     * a station is for and what the near-end bug looked like from the outside.
     */
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

    /**
     * Driver UUID to the world-space direction they were last driving in.
     *
     * <p>Keyed by player rather than by cart because the driver is who "forward" belongs to --
     * they keep it when they step off one coaster and onto another, which is what someone
     * shunting a train back and forth expects.
     */
    private static final java.util.Map<java.util.UUID, Vec3> DRIVER_HEADING =
            new java.util.HashMap<>();

    /**
     * Lets a player riding a coaster drive it with W and S.
     *
     * <p>No input plumbing of our own: a seated player is a passenger, and the vanilla client
     * already reports a passenger's movement to the server every tick, so {@code zza} is
     * simply there to be read. This is the same channel Create's Train Controls use, and the
     * reason driving requires a seat at all.
     *
     * <p>The rider is found by proximity rather than by asking which cart they are strapped
     * to. A cart is a physics sub-level and its seats are not addressable from here, whereas
     * "the player holding controls who is sitting within a few blocks of this cart" is both
     * cheap to answer and, in practice, exactly the same question.
     *
     * <p>Forward is the way the cart is already travelling, so W means "faster" and S means
     * "slower, then back" no matter where the driver is looking. Facing only chooses the
     * direction from a standstill, where there is nothing else to go on.
     */
    private static void coasters_extras$applyDriver(ServerLevel level, PhysicsPipeline pipeline,
                                                    ServerSubLevel sub,
                                                    CoasterPathTrackFrame.GraphHit hit,
                                                    Vector3d tangent, double current, double dt) {
        Vec3 at = hit.point();
        net.minecraft.server.level.ServerPlayer driver = null;
        // Read once per call, not per player, so every candidate is measured against the same
        // radius even if the config is reloaded halfway through the tick.
        double nearest = Config.driverRangeSq();

        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            // Standing up ends control, however it happened.
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

        // Space is the brake, and it outranks the throttle: someone reaching for the brake
        // wants to stop, not to have it argued with by whatever W or S is also doing. It also
        // has to be checked before the coast case below, or a driver holding nothing but the
        // brake would sail straight past.
        if (((LivingEntityJumpAccessor) driver).coasters_extras$isJumping()) {
            coasters_extras$throwLever(level, driver, 0F);
            if (Math.abs(current) > 1.0E-4) {
                // Toward zero, never through it -- clamping the step to the remaining speed
                // stops a hard brake from kicking the cart backwards on the tick it arrives.
                double step = Math.min(Math.abs(current), Config.driverHandbrake() * dt)
                        * -Math.signum(current);
                pipeline.addLinearAndAngularVelocity(sub, new Vector3d(tangent).mul(step),
                        new Vector3d());
            }
            return;
        }

        if (Math.abs(input) < 0.05F) {
            coasters_extras$throwLever(level, driver, 0F);   // hands off: centre the lever
            return;                                          // and let it coast
        }

        // Which way is "forward" for W. Once the cart is rolling this is the direction it is
        // already travelling, NOT where the driver happens to be looking.
        //
        // Looking was the whole rule before, and it is wrong for the obvious reason: riders
        // turn their heads. Glance back down the track at speed and the dot product flips, W
        // becomes reverse and S becomes forward, mid-ride, having touched nothing. Reported
        // from the server and reproducible just by looking over your shoulder.
        //
        // Facing still decides when the cart is stopped, which is the one moment it should:
        // a driver setting off from a standstill picks their direction by facing that way,
        // and there is no travel direction to inherit yet.
        // Stopping must not change what W means. Falling back to the driver's gaze the moment
        // the cart came to rest is why "W, brake, W again" drove off backwards: the curve's
        // tangent points whichever way it was drawn, so at a standstill a driver facing along
        // -tangent had W silently redefined under them.
        //
        // So the heading is remembered as a WORLD vector, not as a sign. A sign is only
        // meaningful against the tangent it was measured on, and the tangent flips between
        // curves -- remembering +1 and reapplying it one curve later points the cart the
        // opposite way. A world vector re-projects onto whatever tangent is current.
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
            // Never moved under power, so there is nothing to inherit and gaze is all there is.
            // This is a coaster fresh out of the placer, which is the one case where choosing
            // by facing is what a driver expects.
            Vec3 look = driver.getLookAngle();
            facing = look.x * tangent.x + look.y * tangent.y + look.z * tangent.z >= 0
                    ? 1.0 : -1.0;
        }

        double desired = input * facing * Config.driverMaxSpeed();
        double delta = desired - current;
        if (Math.abs(delta) < 1.0E-4) return;

        // Slowing down is firmer than speeding up, which also makes S brake first and only
        // then reverse -- the behaviour a driver expects without having to be told.
        boolean slowing = Math.abs(desired) < Math.abs(current)
                || Math.signum(desired) != Math.signum(current);
        double rate = slowing ? Config.driverBrake() : Config.driverAcceleration();

        double step = Math.min(Math.abs(delta), rate * dt) * Math.signum(delta);
        pipeline.addLinearAndAngularVelocity(sub, new Vector3d(tangent).mul(step),
                new Vector3d());

        coasters_extras$throwLever(level, driver, input);
    }

    /**
     * Moves the lever on the stand the driver took hold of.
     *
     * <p>Written to the block state rather than rendered per-viewer, so everyone watching the
     * ride sees the driver working the controls, not just the driver. Only three positions
     * and only written on a change, because every write is a block update -- a continuous
     * angle here would mean one per tick, forever.
     */
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

    /** Identifies a station curve, the same way round whichever end a cart came in from. */
    private static String coasters_extras$stationKey(CoasterPathTrackFrame.GraphHit hit) {
        BlockPos a = hit.edge().from();
        BlockPos b = hit.edge().to();
        long x = a == null ? 0 : a.asLong();
        long y = b == null ? 0 : b.asLong();
        return Math.min(x, y) + "/" + Math.max(x, y);
    }

    /**
     * Drops stations no cart has touched recently.
     *
     * <p>These maps are static and live for the session, so a world where someone builds and
     * removes stations all afternoon would otherwise accumulate an entry per curve forever.
     */
    private static void coasters_extras$forgetIdleStations(long now) {
        if (now % 200 != 0) return;
        // Read here rather than inside the lambda: one read for the whole sweep, and every
        // entry is judged against the same number.
        final long forgetAfter = Config.stationForgetAfterTicks();
        STATION_SEEN.entrySet().removeIf(e -> {
            if (now - e.getValue() <= forgetAfter) return false;
            STATION_ARRESTED.remove(e.getKey());
            STATION_DISPATCH.remove(e.getKey());
            STATION_ENTERED.remove(e.getKey());
            return true;
        });
    }

    /** True if either anchorpoint of this curve is receiving redstone. */
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

        // Publish before the per-track-type switch below, so a display board works on ANY curve
        // rather than only on the ones that happen to have a special behaviour. This number was
        // already being computed and discarded every physics tick; the only new cost is a map
        // write, and the map prunes itself.
        RideTelemetry.report(graphHit.edge().from(), graphHit.edge().to(),
                sub.getRuntimeId(), speed, level.getGameTime());

        // A driver's input applies on ANY track, not only ours -- otherwise you could only
        // steer along the parts of a ride we happen to have built. Runs before the material
        // check for that reason.
        coasters_extras$applyDriver(level, pipeline, sub, graphHit, tangent, current, dt);

        ResourceLocation id = graphHit.edge().bezier().getMaterial().id;
        if (!"coasters_extras".equals(id.getNamespace())) {
            return;
        }
        String kind = id.getPath();

        // The dial lives on the anchorpoints at each end of the curve; take whichever
        // we can read. Curves are bezier connections, not blocks, so they cannot hold
        // a value of their own.
        double dialled = coasters_extras$speedFromAnchors(level, graphHit);

        switch (kind) {
            case "boost_track", "powered_boost_track" -> {
                // The Powered Boost is the same drive on a switch. Gating it here rather
                // than in its own case keeps one implementation of "push the cart", so the
                // two can never drift apart as the boost is tuned.
                // A Powered Boost is gated by definition; a plain Boost is gated only if its
                // Powered dial asks for it. Kept as two variables because the first also picks
                // which config block the speed comes from, and a boost that a player chose to
                // gate is still a BOOST -- it should not silently adopt the powered boost's
                // separate speed and acceleration.
                boolean poweredKind = kind.equals("powered_boost_track");
                if ((poweredKind || coasters_extras$requiresSignal(level, graphHit))
                        && !coasters_extras$anchorPowered(level, graphHit)) {
                    return;
                }
                boolean gated = poweredKind;
                // The dial decides, if it has been set. Otherwise keep the cart's existing
                // direction, which is what a boost always did -- it never reversed a ride.
                //
                // "Never reverses" is right as a default and wrong as a rule: it means a boost
                // cannot start a ride that has come to a stop on it, because a stopped cart's
                // sign is whatever rounding left behind, and it cannot be used to force a
                // circuit to run one way round.
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

                // Effort, not speed: hardest from a standstill and fading to nothing as the
                // cart reaches its target. Scaling off speed instead would put the biggest
                // show at the moment the boost has finished doing anything.
                double f = Math.min(1.0, (target - speed) / Math.max(target, 1.0));
                Vec3 p = graphHit.point();

                // count 0 makes the offsets a velocity vector, so this fires backwards
                // along the track as exhaust rather than sitting in a static puff
                double ex = -tangent.x * dir, ey = -tangent.y * dir, ez = -tangent.z * dir;
                // Throttled to every other game tick. The physics tick can run more than
                // once per game tick, so an unthrottled burst here was several times denser
                // than it looked in the code -- enough to bury the track in sparks.
                if (level.getGameTime() % 2 != 0) return;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        p.x, p.y + 0.22, p.z, 0, ex, ey + 0.25, ez, 0.22 + f * 0.3);
                if (f > 0.6) {
                    level.sendParticles(ParticleTypes.FLAME,
                            p.x, p.y + 0.18, p.z, 0, ex, ey + 0.1, ez, 0.15 + f * 0.2);
                }
                // No sound. A firework blast at pitch 1.9 every eight ticks is a short, high
                // tick repeated for the whole length of the boost -- it reads as a bug rather
                // than as thrust, and there is nowhere to take it: quieter is still ticking,
                // and slower stops lining up with the exhaust. The sparks and flame carry it.
            }
            case "station_track" -> {
                int cartId = sub.getRuntimeId();
                // Dial is seconds here rather than blocks per second. Same control, and a
                // station has nothing meaningful to say about speed.
                double dwell = dialled > 0 ? dialled : Config.stationDwellSeconds();
                int dwellTicks = Math.max(1, (int) (dwell * 20));
                boolean held = coasters_extras$anchorPowered(level, graphHit);
                long now = level.getGameTime();
                Vec3 p = graphHit.point();

                // Which way is it going? Once stopped there is no heading left to read, so
                // it is remembered on the way in and reused for the stop target and the
                // launch direction alike.
                double dir;
                boolean knowsHeading = true;
                if (speed > Config.stationStoppedSpeed()) {
                    dir = current >= 0 ? 1 : -1;
                    STATION_DIR.put(cartId, dir);
                } else {
                    Double remembered = STATION_DIR.get(cartId);
                    // No remembered heading and not moving: this cart has never rolled on this
                    // station. The old code assumed +1 here, which is a coin flip -- get it
                    // wrong and exitAnchor resolves to the anchorpoint BEHIND the cart, so
                    // "remaining" is nearly zero and the station arrests it on the spot, at the
                    // end it came in from. Reported as "it stops at the wrong side".
                    knowsHeading = remembered != null;
                    dir = remembered != null ? remembered : 1.0;
                }

                // The dial names the platform end, when it has been set.
                //
                // On auto the station stops a ride at whichever end it was heading for, which
                // is right for a through station but useless for the case the dial exists for:
                // a coaster PLACED on the platform has no heading at all, so "wherever it was
                // going" is whatever it happened to be remembered as. Forward parks it at the
                // anchorpoint along the curve's tangent, reverse at the other one, and it does
                // not matter which way it arrived or whether it ever moved.
                // Not "dialled" -- that name is already the speed dial a few lines up, and this
                // is the direction one.
                int dialledDirection = coasters_extras$directionFromAnchors(level, graphHit);
                BlockPos exitAnchor;
                if (dialledDirection != 0) {
                    exitAnchor = coasters_extras$anchorAlong(graphHit, tangent, dialledDirection);
                } else if (knowsHeading) {
                    exitAnchor = coasters_extras$exitAnchor(graphHit, p, tangent, dir);
                } else {
                    // Nothing to go on: no dial, no heading. Send it to whichever anchorpoint is
                    // FURTHER away, so it crosses the platform instead of parking where it sits.
                    // That is what a station looks like it should do, and it makes a coaster
                    // placed by hand behave the same as one that rolled in.
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

                // Leaving: the whole train is under power until it is clear of the platform.
                // Without this a cart launches, is still on station track next tick, and the
                // arrival logic below arrests it again -- it left, then stopped on the bell.
                Long dispatchedAt = STATION_DISPATCH.get(station);
                if (dispatchedAt != null) {
                    if (now - dispatchedAt > Config.stationDispatchWindowTicks()) {
                        STATION_DISPATCH.remove(station);
                    } else {
                        // Auto (0) keeps what the cart arrived with, which is what every station
                        // did before this dial existed. A dialled direction overrides it, so a
                        // station can be made to always send rides the same way round a circuit
                        // regardless of which end the cart rolled in from.
                        int dialledDir = coasters_extras$directionFromAnchors(level, graphHit);
                        double out = dialledDir != 0
                                ? dialledDir
                                : STATION_DIR.getOrDefault(cartId, dir);
                        // Read per dispatch rather than cached in a static: NeoForge loads
                        // the config after mod construction and reloads it when the file
                        // changes, so a cached copy would be stale on both counts.
                        //
                        // The Station Boost dial wins when it has been set. Zero means
                        // untouched, which falls through to the config value -- so the global
                        // default still governs every station nobody has dialled in, and one
                        // station can differ without every other needing configuring first.
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

                // However fast it arrives, it has to be stoppable in the platform it has left.
                //
                // Braking is bounded by station.maxDeceleration, and v^2 = 2*a*d says the fastest
                // that bound can arrest within d is sqrt(2*a*d) -- about 80 b/s across a
                // typical eight-block platform. Anything above that was not slow-stopping,
                // it was physically impossible, and the cart sailed straight through. A
                // coaster doing 10000 needed 6,250,000 b/s^2 and got 400.
                //
                // So the speed is clamped to what can actually be stopped. Above that the
                // slowdown is abrupt, which is the honest outcome: there is no gentle way to
                // lose that much speed over eight blocks.
                // Read once and reused three lines down, so the clamp and the brake that
                // follows it cannot disagree if the file is reloaded between them.
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

                // A cart that turned up AFTER the station arrested is not part of the train
                // being held -- it is a fresh arrival, and it still has a platform to cross.
                //
                // This is the redstone-freeze bug. While a station is held by redstone the
                // arrest is refreshed every tick and never cleared, because the only place that
                // clears it is the dispatch at the bottom of this case, which a held station
                // never reaches. So STATION_ARRESTED stays set forever, and from then on EVERY
                // cart touching that curve skipped the braking branch below and went straight
                // to "pinned every tick" -- frozen on contact, partway along the track.
                boolean newArrival = arrestedAt != null && myArrival > arrestedAt;

                // Nothing has stopped here yet, or this cart is not part of what stopped:
                // either way it is still rolling in.
                if ((arrestedAt == null || newArrival)
                        && remaining > Config.stationArriveDistance()) {
                    // Brake by where the platform ENDS, not by a fixed rate.
                    //
                    // v^2 = 2*a*d rearranges to a = v^2 / 2d, so this is the exact
                    // deceleration that reaches zero at the far anchorpoint and nowhere
                    // else -- whatever speed the cart happened to arrive at. Recomputed
                    // every tick, so drag and gradient are absorbed automatically instead
                    // of accumulating into an overshoot.
                    // Same read-once-per-use rule: the comfortable rate is wanted twice in the
                    // next four lines and both uses have to be the same number.
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
                        // Too slow to coast to the end on its own. Without this a cart that
                        // trickles in parks halfway down the platform and waits there, which
                        // is not what "stops at the last anchorpoint" means.
                        //
                        // creepSpeed is the value read for the test above, deliberately: two
                        // separate reads could straddle a config reload and leave this
                        // subtraction negative, which would push the cart backwards.
                        double push = Math.min(Config.stationCreepAcceleration() * dt,
                                               creepSpeed - speed);
                        pipeline.addLinearAndAngularVelocity(sub,
                                new Vector3d(tangent).mul(push * dir), new Vector3d());
                    }
                    StationTracker.report(entryAnchor, reportB,
                            StationPayload.STATE_ARRIVING, 0, dwellTicks, now);
                    return;
                }

                // The leading cart has reached the far anchorpoint. Arrest the whole station
                // from this moment -- every cart on this curve, wherever it happens to be
                // along it, stops here.
                if (arrestedAt == null) {
                    arrestedAt = now;
                    STATION_ARRESTED.put(station, now);
                    level.playSound(null, p.x, p.y, p.z, SoundEvents.IRON_DOOR_CLOSE,
                            SoundSource.BLOCKS, 0.45F, 1.6F);
                }

                // Frozen. Not braked -- pinned, every tick, so a cart still rolling in behind
                // the leader cannot shunt the train forward off the platform.
                if (Math.abs(current) > 1.0E-3) {
                    pipeline.addLinearAndAngularVelocity(sub,
                            new Vector3d(tangent).mul(-current), new Vector3d());
                }

                if (held) {              // redstone on the anchor keeps it in the station
                    STATION_ARRESTED.put(station, now);   // countdown restarts when released
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

                // Dispatch the whole train at once. No impulse here on purpose -- the
                // departing branch above eases it up to speed over about a second, which
                // reads as a station letting a train go rather than a catapult.
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
                // A brake set to "On Redstone" is a brake you can switch off, which is how you
                // build a block section: hold a train with the signal off, release it with the
                // signal on. Unpowered it does nothing at all rather than braking weakly.
                if (coasters_extras$requiresSignal(level, graphHit)
                        && !coasters_extras$anchorPowered(level, graphHit)) {
                    return;
                }
                double target = dialled >= 0 ? dialled : Config.brakeTargetSpeed();
                if (speed <= target) return;
                double dir = current >= 0 ? 1 : -1;
                double over = speed - target;

                // Brake by stopping DISTANCE, not by a fixed rate.
                //
                // The old scaling capped out at 130 b/s^2, which sounds strong until you do
                // the arithmetic: a cart arriving at 100 b/s needs 0.74s to shed that, and
                // at 100 b/s it covers about 74 blocks in that time -- so it sailed straight
                // through any brake run you would actually build and came out the far end
                // barely slowed. That is why it "worked" at moderate speed and did nothing
                // when a cart was really moving.
                //
                // v^2 = 2*a*d rearranges to a = v^2 / 2d, so asking for a fixed stopping
                // distance makes the force scale with the square of the overspeed, which is
                // what actually keeps a fast cart on a short brake.
                double needed = (over * over) / (2.0 * Config.brakeStopDistance());
                double rate = Math.min(Math.max(Config.brakeDeceleration(), needed),
                                       Config.brakeDecelerationMax());
                double delta = -Math.min(rate * dt, over) * dir;
                pipeline.addLinearAndAngularVelocity(sub, new Vector3d(tangent).mul(delta),
                        new Vector3d());

                // Asked for a dead stop, and nearly there: cancel what is left outright.
                // The rate falls away with the square of the overspeed, so the last fraction
                // of a block per second takes forever to shed and the cart creeps on instead
                // of stopping.
                if (target <= 0.01 && speed < Config.brakeDeadStopSpeed()) {
                    pipeline.addLinearAndAngularVelocity(sub,
                            new Vector3d(tangent).mul(-(current + delta)), new Vector3d());
                }

                // tell clients so the wheels glow; only while it is really gripping
                if (over > Config.brakeGlowOverspeed()) {
                    Vec3 wp = graphHit.point();
                    BrakingTracker.report(wp.x, wp.y, wp.z);
                }

                double sparkSpeed = Config.brakeSparkSpeed();
                if (speed > sparkSpeed) {
                    // Everything below scales with speed, so the effect reads as
                    // "how hard is this thing fighting" rather than a fixed animation.
                    //
                    // Config.brakeFullEffectSpeed() is guaranteed to be greater than
                    // brakeSparkSpeed(), which is what keeps this divisor off zero -- a range
                    // cannot say "must exceed that other setting", so Config enforces it.
                    double f = Math.min(1.0, (speed - sparkSpeed)
                                             / (Config.brakeFullEffectSpeed() - sparkSpeed));
                    Vec3 p = graphHit.point();
                    double spread = 0.08 + f * 0.22;
                    long now = level.getGameTime();

                    // Particles stay on a fixed quarter-second cadence; their INTENSITY is what
                    // carries the speed, via the counts and spread below.
                    if (now % 4 == 0) {
                        int lava = 1 + (int) (f * 3);           // 1 -> 4 particles
                        level.sendParticles(ParticleTypes.LAVA, p.x, p.y + 0.2, p.z,
                                lava, spread, spread * 0.5, spread, 0.0);

                        // sparks only once it is genuinely screaming
                        if (f > 0.6) {
                            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.25, p.z,
                                    1 + (int) (f * 2), spread, spread * 0.5, spread, 0.02);
                        }
                        if (f > 0.9) {
                            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.35, p.z,
                                    1, spread, 0.1, spread, 0.01);
                        }
                    }

                    // Faster arrival -> more frequent, louder, higher hiss.
                    //
                    // This test used to sit INSIDE the `now % 4 == 0` block above, which made it
                    // dead: any tick divisible by 4 is also divisible by 2, so `% 2 == 0` was
                    // always true there and the hiss ran at a flat four-tick beat no matter how
                    // hard the brake was working. It has to be gated independently to have any
                    // effect at all, which is why it now sits outside.
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
                // Purely decorative -- no speed change. Colour and pitch both come from the
                // same hue, so the sparkles and the chime stay in step as the cart travels.
                //
                // Reverted to the original 0.9.x behaviour: a flat three-tick beat, three
                // sparkles, and a chime on every one of those ticks. The later versions added a
                // speed gate, a speed-scaled sparkle count and an ungated sound, which between
                // them changed what the track sounded like.
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

                // Quantised to a major pentatonic so consecutive segments sound like a
                // melody rather than a random sweep. Note block pitch is 2^(n/12).
                // Chimes on every beat, deliberately. A "one note per colour" version was
                // tried and reverted: gating the sound on a colour CHANGE made the track go
                // silent for long stretches whenever a cart sat inside one hue band, which
                // sounds broken rather than musical. The steady beat is the sound of this
                // track. Do not re-add the gate.
                // Two octaves of major pentatonic rather than one, centred on the base
                // note. Six notes inside a single octave meant a cart could cross most of a
                // rainbow section without the tune moving far enough to hear -- eleven notes
                // spanning -12 to +12 semitones gives it somewhere to actually go.
                //
                // -12 and +12 are the ends of the range on purpose: playSound clamps pitch to
                // 0.5..2.0, which IS exactly one octave either side, so anything wider would
                // be silently flattened against the limit and several notes would collapse
                // into the same sound.
                int[] pentatonic = { -12, -10, -8, -5, -3, 0, 2, 4, 7, 9, 12 };
                int step = pentatonic[Math.min(pentatonic.length - 1,
                                               (int) (hue * pentatonic.length))];
                float pitch = (float) Math.pow(2.0, step / 12.0);
                level.playSound(null, p.x, p.y, p.z, SoundEvents.NOTE_BLOCK_CHIME.value(),
                        SoundSource.BLOCKS, 0.5F, pitch);
            }
            case "bobsled_track" -> {
                // Leaning, done as ROLL rather than as sliding.
                //
                // The rail guide keeps the cart pinned to the line (see CoasterGuideBobsledMixin
                // for why letting it slide sideways was abandoned), and only roll is free. So the
                // lean a trough would have produced is computed and applied here instead: work
                // out the sideways force the corner is generating, turn that into the angle that
                // would cancel it, and drive the cart's roll toward it.
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
                if (prevFwd == null) return;   // first tick here: no corner to measure yet

                // Signed angle swept about the track's up axis since last tick. atan2 of the
                // cross and dot rather than acos of the dot alone, because acos loses the sign
                // and a left corner would lean the same way as a right one.
                double sweep = Math.atan2(new Vector3d(prevFwd).cross(tangent).dot(up),
                                          prevFwd.dot(tangent));
                double travelled = Math.max(1.0E-6, speed * dt);
                double curvature = sweep / travelled;

                // v^2 * curvature is the sideways acceleration; atan against gravity is the
                // angle at which that force points straight down through the floor of the cart,
                // which is exactly what a banked corner is for.
                double lateral = speed * speed * curvature;
                double target = Math.atan2(lateral, Config.bobsledGravity())
                        * Config.bobsledBankSign();
                double maxBank = Math.toRadians(Config.bobsledMaxBankDegrees());
                target = Math.max(-maxBank, Math.min(maxBank, target));

                // Current roll: the cart's own up vector measured against the track's.
                dev.ryanhcode.sable.companion.math.Pose3d pose =
                        pipeline.readPose(sub, new dev.ryanhcode.sable.companion.math.Pose3d());
                Vector3d cartUp = pose.orientation().transform(new Vector3d(0, 1, 0));
                double roll = Math.atan2(cartUp.dot(right), cartUp.dot(up));

                // Proportional-derivative, not a hard set: writing the orientation directly
                // would fight the solver and read as a jitter rather than as a lean.
                Vector3d angNow = pipeline.getAngularVelocity(sub, new Vector3d());
                double rollRate = angNow.dot(tangent);
                double wantedRate = (target - roll) * Config.bobsledBankStiffness();
                double correction = (wantedRate - rollRate) * Config.bobsledBankDamping();
                pipeline.addLinearAndAngularVelocity(sub, new Vector3d(),
                        new Vector3d(tangent).mul(correction));

                // Same bound as the other per-cart maps.
                if (BOBSLED_PREV_FWD.size() > 256) {
                    BOBSLED_PREV_FWD.clear();
                }
            }
            case "slippery_track" -> {
                // Cannot switch friction off in the solver, so give back a slice of what
                // drag is taking. Proportional to speed, which is roughly how drag scales,
                // so a fast cart coasts and a slow one is not flung.
                // Read once: the cap decides both whether to act and how much is left to give,
                // and those two have to agree or the clamp below could go negative.
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
                // The curve itself still cannot emit redstone -- it is a connection, not a
                // block. It reports the crossing here instead, and a linked Sensor Block
                // turns that into a signal.
                SensorTracker.report(graphHit.edge().from(), graphHit.edge().to(),
                                     level.getGameTime());
                if (level.getGameTime() % 2 == 0) {
                    Vec3 p = graphHit.point();
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.25, p.z,
                            1, 0.05, 0.05, 0.05, 0.0);
                }
            }
            case "splash_track" -> {
                // A parked cart should not fountain, so nothing happens below the threshold.
                if (speed < Config.splashMinSpeed()) return;

                // Water Boost, from the anchorpoint dial.
                //
                // Water drags a ride down, and a splash section long enough to look good is
                // long enough to strand a coaster in it. With a speed dialled, this section
                // DRIVES the ride at that speed instead of only taking from it -- so a flume
                // runs on its own. Untouched (0) it stays pure drag, which is what it did
                // before this dial existed.
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
                    // Gentle water drag -- proportional to speed, clamped so it can never kick
                    // the cart backwards on the tick it arrives. Water slows you a little; it
                    // does not stop a coaster, so this is deliberately light.
                    dir = current >= 0 ? 1 : -1;
                    double shed = Math.min(Config.splashDrag() * speed * dt, speed) * dir;
                    pipeline.addLinearAndAngularVelocity(sub,
                            new Vector3d(tangent).mul(-shed), new Vector3d());
                }

                // Sideways axis: tangent x up. On a vertical drop the tangent is parallel to up
                // and the cross collapses, so fall back to the world X axis there.
                Vector3d right = new Vector3d(tangent).cross(new Vector3d(0, 1, 0));
                if (right.lengthSquared() < 1.0E-6) {
                    right.set(1, 0, 0);
                }
                right.normalize();

                double f = Math.min(1.0, speed / Config.splashFullEffectSpeed());   // 0..1 show
                long now = level.getGameTime();
                if (now % 2 != 0) return;   // the physics tick can run several times a game tick

                Vec3 p = graphHit.point();

                // Two emitters, one per side, was a pair of fountains at a point. A real
                // splashdown is a SHEET: it runs the length of the cart, throws up at the
                // rail line AND out past the side rails, and drops a curtain underneath.
                // So the emitters are now a grid -- both sides, an inner and an outer
                // distance, and three stations along the track -- with each one emitting a
                // fraction of what a single emitter used to, which nets out heavier without
                // costing twelve times the particles.
                //
                // Read outside the loops so every emitter agrees on its geometry.
                double[] lateral = { Config.splashSideOffset(), Config.splashRailOffset() };
                double along = Config.splashLengthSpread();
                double under = Config.splashUnderDepth();
                double[] stations = { -along, 0.0, along };
                DustParticleOptions foam = new DustParticleOptions(
                        new Vector3f(0.90f, 0.98f, 1.0f), 1.2f);

                for (int side = -1; side <= 1; side += 2) {
                    for (int lane = 0; lane < lateral.length; lane++) {
                        double lat = lateral[lane] * side;
                        // The outer lane sits past the side rails, so it gets the falling
                        // water and the under-rail curtain; the inner lane carries the spray.
                        boolean outer = lane == 1;

                        for (double s : stations) {
                            double sx = p.x + right.x * lat + tangent.x * s;
                            double sy = p.y + 0.12 + tangent.y * s;
                            double sz = p.z + right.z * lat + tangent.z * s;

                            // droplets thrown up and outward from the rail
                            //
                            // Ours, not ParticleTypes.SPLASH: vanilla's is a single flat
                            // droplet picked at random from the water sheet, so a coaster
                            // hitting water looked like it was raining rather than throwing a
                            // wall of spray.
                            level.sendParticles(
                                    dev.notzyvex.coasters_extras.particle.ModParticles
                                            .SPLASH.get(),
                                    sx, sy, sz,
                                    3 + (int) (f * 5), 0.12, 0.14 + f * 0.1, 0.12, 0.0);
                            level.sendParticles(ParticleTypes.FALLING_WATER, sx, sy + 0.1, sz,
                                    1 + (int) (f * 2), 0.16, 0.08, 0.16, 0.0);
                            // white foam crest
                            level.sendParticles(foam, sx, sy + 0.05, sz,
                                    1 + (int) (f * 2), 0.14, 0.05, 0.14, 0.02);

                            if (outer) {
                                // The curtain: water sheeting off the side rails and falling
                                // away beneath the track. Without it the splash floated on
                                // top of the rail and the underside stayed bone dry.
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

                // ONE splash sound per pass, not a continuous rush. Debounced per cart: it
                // fires when the cart enters and stays quiet until it has left and come back.
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
                // Held until the circuit says go, if the Launch On dial asks for it. Checked
                // before anything else so a gated launch is completely inert -- no push, no
                // fire, no sound -- rather than a launch that fires quietly.
                if (coasters_extras$requiresSignal(level, graphHit)
                        && !coasters_extras$anchorPowered(level, graphHit)) {
                    return;
                }
                // Like a boost but far harder, and it WILL start a stopped cart -- a launch
                // whose whole job is to fling a ride out of a station cannot refuse to move a
                // stationary one. Direction from the dial if set, else current heading, else
                // forward along the tangent.
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
                if (level.getGameTime() % 2 != 0) return;   // physics ticks can double up
                // fire and smoke blasting out the back, like a real launch
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
                // Flip travel direction, but ONCE per pass. Reversing every physics tick would
                // trap a cart oscillating on the segment, so a per-cart latch holds it for a
                // few ticks -- long enough to roll off the other way before it can fire again.
                if (speed < Config.reverseMinSpeed()) return;
                int cartId = sub.getRuntimeId();
                long now = level.getGameTime();
                Long last = REVERSE_LAST.get(cartId);
                if (last != null && now - last < Config.reverseCooldownTicks()) return;
                // The Send dial decides what this track does, if it has been set.
                //
                //   Auto    -> flip whichever way the cart came in, the plain shuttle behaviour.
                //   Forward -> always leave along the curve's tangent.
                //   Reverse -> always leave against it.
                //
                // A forced direction is what makes this usable on a circuit: a cart that is
                // already going the wanted way is left alone rather than turned round, so the
                // track becomes "make sure rides head THIS way" instead of an unconditional flip.
                int wantDir = coasters_extras$directionFromAnchors(level, graphHit);
                double outSign = current >= 0 ? 1 : -1;
                if (wantDir != 0) {
                    if (outSign == wantDir) {
                        return;   // already heading the dialled way: nothing to do
                    }
                    outSign = wantDir;
                }

                REVERSE_LAST.put(cartId, now);
                if (now % 200 == 0) {
                    REVERSE_LAST.entrySet().removeIf(e -> now - e.getValue() > 200);
                }

                // v -> -v along the track: add -2*current so the tangential component flips.
                // Both the auto and the forced case are the same arithmetic, because a forced
                // direction only gets here when the cart is currently going the other way.
                pipeline.addLinearAndAngularVelocity(sub,
                        new Vector3d(tangent).mul(-2.0 * current), new Vector3d());

                // Reverse Boost: the speed dial says how fast to send it back out. Without it
                // a shuttle leaves at exactly the speed it arrived minus whatever the turn
                // cost, which on a long return leg means it never makes it home. Zero (the
                // untouched default) keeps the plain mirror-flip above.
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
            default -> { }   // a plain material variant: no behaviour
        }
    }
}
