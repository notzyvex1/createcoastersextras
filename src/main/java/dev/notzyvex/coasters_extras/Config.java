package dev.notzyvex.coasters_extras;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side tuning, exposed in {@code config/coasters_extras-server.toml}.
 *
 * <p>Server rather than common: these values decide how a coaster actually moves, so the
 * server has to be the one that owns them. On a common config a client with different numbers
 * would predict a different launch to the one the server performs, and the ride would visibly
 * stutter as the two corrected each other.
 *
 * <p>Values are read through the getters rather than cached in a static, because NeoForge
 * loads the config after mod construction and reloads it when the file changes on disk.
 * Every getter here calls {@code ConfigValue.get()} on the spot for that reason -- assigning
 * one of these to a {@code static final} anywhere would freeze it at whatever it was during
 * mod construction, which is before the file has been read at all.
 *
 * <p>Nothing here is written to the world save. These are session tuning numbers; the values
 * that DO persist are the per-anchorpoint dials ({@code StationBoostBehaviour},
 * {@code SendDirectionBehaviour}), which are not configured from this file. So no setting in
 * here can corrupt a save -- the worst a bad number does is make a ride behave badly until it
 * is changed back.
 *
 * <p>Each dial on an anchorpoint still overrides the matching value here. These are the
 * defaults every curve nobody has dialled in falls back to.
 */
public final class Config {

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // ------------------------------------------------------------------ boost track

    private static final ModConfigSpec.DoubleValue BOOST_SPEED = B
            .comment("",
                     "How fast a Boost Track drives carts, in blocks per second.",
                     "Used on any boost curve whose Boost Speed dial has never been set. 0 disables it.")
            .defineInRange("boost.speed", 22.0, 0.0, 1000.0);

    private static final ModConfigSpec.DoubleValue BOOST_ACCELERATION = B
            .comment("",
                     "How hard a boost pushes up to that speed, in blocks per second squared.",
                     "Low is a gentle shove over several blocks; high is an instant kick.")
            .defineInRange("boost.acceleration", 40.0, 0.1, 2000.0);

    // ------------------------------------------------------------------ rainbow track

    private static final ModConfigSpec.DoubleValue RAINBOW_COLOUR_SCALE = B
            .comment("",
                     "How quickly the Rainbow Track's colour and chime move along the track.",
                     "This is cycles per block, so 0.06 runs a full rainbow every ~17 blocks",
                     "and 0.20 every ~5. Higher means more colour and more notes per second of",
                     "ride; lower means long sweeps of one colour. Drives both, so they always",
                     "stay in step with each other.")
            .defineInRange("rainbow.colourScale", 0.16, 0.001, 5.0);

    // ---------------------------------------------------------------- bobsled track

    private static final ModConfigSpec.DoubleValue BOBSLED_MIN_SPEED = B
            .comment("",
                     "Below this speed, in blocks per second, a bobsled cart does not lean.",
                     "A parked cart has no corner to lean into, so it sits upright.")
            .defineInRange("bobsled.minSpeed", 2.0, 0.0, 100.0);

    private static final ModConfigSpec.DoubleValue BOBSLED_MAX_BANK = B
            .comment("",
                     "The furthest a bobsled cart will lean, in degrees.",
                     "Real bobsleds reach vertical on a hard corner; 35 reads well in game",
                     "without the rider losing track of which way is up. 0 disables leaning.")
            .defineInRange("bobsled.maxBankDegrees", 35.0, 0.0, 89.0);

    private static final ModConfigSpec.DoubleValue BOBSLED_STIFFNESS = B
            .comment("",
                     "How hard the cart is pulled toward the lean the corner calls for.",
                     "Higher snaps into the bank; lower rolls into it lazily.")
            .defineInRange("bobsled.bankStiffness", 6.0, 0.1, 100.0);

    private static final ModConfigSpec.DoubleValue BOBSLED_DAMPING = B
            .comment("",
                     "How strongly the roll is settled toward that target each tick.",
                     "Too low and the cart wallows; too high and it snaps and jitters.")
            .defineInRange("bobsled.bankDamping", 0.35, 0.01, 1.0);

    private static final ModConfigSpec.DoubleValue BOBSLED_GRAVITY = B
            .comment("",
                     "The gravity used to work out the lean angle, in blocks per second squared.",
                     "This is the reference the sideways force is compared against, not the",
                     "world's actual gravity. Lower it to make every corner lean harder.")
            .defineInRange("bobsled.gravity", 9.81, 0.1, 200.0);

    private static final ModConfigSpec.BooleanValue BOBSLED_INVERT = B
            .comment("",
                     "Flip which way the cart leans in a corner.",
                     "Which sign is correct depends on the base mod handedness, so this is a",
                     "config toggle rather than a constant: if your carts lean OUT of corners",
                     "instead of into them, set this true and they will lean the other way.")
            .define("bobsled.invertBank", false);

    // ---------------------------------------------------------- powered boost track

    private static final ModConfigSpec.DoubleValue POWERED_BOOST_SPEED = B
            .comment("",
                     "How fast a Powered Boost Track drives carts while it has redstone,",
                     "in blocks per second. Used on any powered boost curve whose dial has",
                     "never been set. 0 disables it.")
            .defineInRange("powered_boost.speed", 22.0, 0.0, 1000.0);

    private static final ModConfigSpec.DoubleValue POWERED_BOOST_ACCELERATION = B
            .comment("",
                     "How hard a powered boost pushes up to that speed, in blocks per",
                     "second squared. Kept separate from the plain boost so a signal-driven",
                     "launch can be violent without making every boost on the map violent.")
            .defineInRange("powered_boost.acceleration", 55.0, 0.1, 2000.0);

    // ------------------------------------------------------------------ brake track

    private static final ModConfigSpec.DoubleValue BRAKE_TARGET_SPEED = B
            .comment("",
                     "The speed a Brake Track slows carts down to, in blocks per second.",
                     "Used when the Braking Sensitivity dial has never been set. 0 means a dead stop.")
            .defineInRange("brake.targetSpeed", 4.0, 0.0, 1000.0);

    private static final ModConfigSpec.DoubleValue BRAKE_DECELERATION = B
            .comment("",
                     "The gentlest a brake ever grabs, in blocks per second squared.",
                     "A cart only barely over the target loses speed at least this fast.")
            .defineInRange("brake.deceleration", 30.0, 0.1, 5000.0);

    private static final ModConfigSpec.DoubleValue BRAKE_STOP_DISTANCE = B
            .comment("",
                     "How many blocks of brake run it takes to reach the target speed, at any arrival speed.",
                     "Shorter means faster carts get gripped harder. This is the number to change if",
                     "rides sail through your brake runs.")
            .defineInRange("brake.stopDistance", 6.0, 0.1, 128.0);

    private static final ModConfigSpec.DoubleValue BRAKE_DECELERATION_MAX = B
            .comment("",
                     "Hardest a brake is ever allowed to grab, in blocks per second squared.",
                     "A safety ceiling so a ridiculous arrival speed still slows down instead of",
                     "stopping dead in one tick.")
            .defineInRange("brake.decelerationMax", 1200.0, 1.0, 100000.0);

    private static final ModConfigSpec.DoubleValue BRAKE_SPARK_SPEED = B
            .comment("",
                     "Above this arrival speed a brake starts throwing sparks, smoke and hiss.",
                     "Purely the show -- it does not change how hard the brake grips.")
            .defineInRange("brake.sparkSpeed", 14.0, 0.0, 1000.0);

    private static final ModConfigSpec.DoubleValue BRAKE_FULL_EFFECT_SPEED = B
            .comment("",
                     "The speed at which the brake's effects are at full blast -- most particles, loudest hiss.",
                     "Must be above brake.sparkSpeed; it is forced above it at runtime if you set it lower.")
            .defineInRange("brake.fullEffectSpeed", 50.0, 0.1, 2000.0);

    private static final ModConfigSpec.DoubleValue BRAKE_GLOW_OVERSPEED = B
            .comment("",
                     "How far over the target speed a cart has to be before its wheels glow for nearby players.")
            .defineInRange("brake.glowOverspeed", 2.0, 0.0, 100.0);

    private static final ModConfigSpec.DoubleValue BRAKE_DEAD_STOP_SPEED = B
            .comment("",
                     "When a brake is set to stop carts completely, this last crawl is cancelled outright",
                     "instead of being braked, so the cart parks rather than creeping on forever.")
            .defineInRange("brake.deadStopSpeed", 0.8, 0.0, 10.0);

    // ------------------------------------------------------------------ station track

    private static final ModConfigSpec.DoubleValue STATION_LAUNCH_SPEED = B
            .comment("",
                     "How fast a station dispatches a coaster, in blocks per second.",
                     "This is the speed the ride leaves the platform at -- enough to reach the",
                     "first drop or the next boost track. Raise it if your rides stall out",
                     "leaving the station, lower it for a gentler departure.")
            .defineInRange("station.launchSpeed", 120.0, 0.5, 500.0);

    private static final ModConfigSpec.DoubleValue STATION_LAUNCH_RATE = B
            .comment("",
                     "How hard it accelerates up to that speed, in blocks per second squared.",
                     "Low values ease out of the station; high values shove. This is what makes",
                     "a dispatch feel like a real ride rather than a teleport.")
            .defineInRange("station.launchAcceleration", 60.0, 0.5, 500.0);

    private static final ModConfigSpec.DoubleValue STATION_APPROACH_DECELERATION = B
            .comment("",
                     "The comfortable slowdown a station aims for on arrival, in blocks per second squared.",
                     "Low is a long unhurried glide in. Carts arriving too fast for this brake harder",
                     "anyway, because they still have to be stopped by the end of the platform.")
            .defineInRange("station.approachDeceleration", 5.0, 0.1, 1000.0);

    private static final ModConfigSpec.DoubleValue STATION_MAX_DECELERATION = B
            .comment("",
                     "Hardest a station will ever brake, in blocks per second squared.",
                     "This also decides the fastest arrival it can catch at all: roughly",
                     "sqrt(2 x this x platform length), so about 155 b/s across eight blocks at 1500.",
                     "Lower it and fast coasters start sailing straight through the station.")
            .defineInRange("station.maxDeceleration", 1500.0, 1.0, 100000.0);

    private static final ModConfigSpec.DoubleValue STATION_ARRIVE_DISTANCE = B
            .comment("",
                     "How close to the end of the platform, in blocks, counts as having arrived.",
                     "Raising it parks trains further short of the end; it must stay above zero.")
            .defineInRange("station.arriveDistance", 0.45, 0.05, 8.0);

    private static final ModConfigSpec.DoubleValue STATION_CREEP_SPEED = B
            .comment("",
                     "A cart that stalls partway down the platform is nudged along at this speed",
                     "until it reaches the end, in blocks per second.")
            .defineInRange("station.creepSpeed", 1.6, 0.0, 20.0);

    private static final ModConfigSpec.DoubleValue STATION_CREEP_ACCELERATION = B
            .comment("",
                     "How briskly that nudge comes on, in blocks per second squared.")
            .defineInRange("station.creepAcceleration", 4.0, 0.1, 200.0);

    private static final ModConfigSpec.DoubleValue STATION_STOPPED_SPEED = B
            .comment("",
                     "Below this speed a cart in a station counts as parked, so the station remembers",
                     "which way it was going instead of reading a heading that is no longer there.")
            .defineInRange("station.stoppedSpeed", 0.35, 0.01, 5.0);

    private static final ModConfigSpec.DoubleValue STATION_DWELL_SECONDS = B
            .comment("",
                     "How long a train waits in a station before it is dispatched, in seconds.",
                     "Used on any station whose dwell dial has never been set.")
            .defineInRange("station.dwellSeconds", 3.0, 0.05, 600.0);

    private static final ModConfigSpec.DoubleValue STATION_BRAKE_EFFECT_SPEED = B
            .comment("",
                     "Above this arrival speed a station puffs steam and clanks as it brakes. Show only.")
            .defineInRange("station.brakeEffectSpeed", 6.0, 0.0, 500.0);

    private static final ModConfigSpec.IntValue STATION_DISPATCH_WINDOW_TICKS = B
            .comment("",
                     "How long a departing train stays under station power after the bell, in ticks (20 = 1 second).",
                     "It has to be long enough for the whole train to clear the platform, or the station",
                     "catches the back of it and stops the ride it just sent out.")
            .defineInRange("station.dispatchWindowTicks", 60, 1, 1200);

    private static final ModConfigSpec.IntValue STATION_FORGET_AFTER_TICKS = B
            .comment("",
                     "A station nothing has touched for this many ticks is forgotten to save memory.",
                     "Housekeeping only -- leave it alone unless you build and delete stations constantly.",
                     "Never allowed below station.dispatchWindowTicks, or a train would be forgotten mid-dispatch.")
            .defineInRange("station.forgetAfterTicks", 200, 60, 24000);

    // ------------------------------------------------------------------ slippery track

    private static final ModConfigSpec.DoubleValue SLIPPERY_RECOVER = B
            .comment("",
                     "Fraction of its own speed a cart gets handed back per second on Slippery Track,",
                     "to cancel out friction. 0.55 roughly matches ordinary drag; higher actively speeds",
                     "rides up, 0 makes the track do nothing.")
            .defineInRange("slippery.recoverFraction", 0.55, 0.0, 10.0);

    private static final ModConfigSpec.DoubleValue SLIPPERY_MAX_SPEED = B
            .comment("",
                     "Slippery Track stops helping once a cart is going this fast, in blocks per second.")
            .defineInRange("slippery.maxSpeed", 60.0, 0.5, 1000.0);

    private static final ModConfigSpec.DoubleValue SLIPPERY_MIN_SPEED = B
            .comment("",
                     "And it does nothing at all below this speed, so a parked cart is not flung off.")
            .defineInRange("slippery.minSpeed", 0.5, 0.0, 100.0);

    // ------------------------------------------------------------------ splash track

    private static final ModConfigSpec.DoubleValue SPLASH_DRAG = B
            .comment("",
                     "Fraction of its speed a cart loses per second to the water, when the Water Boost",
                     "dial is left untouched. Deliberately light -- water slows a ride, it does not stop one.")
            .defineInRange("splash.drag", 0.12, 0.0, 10.0);

    private static final ModConfigSpec.DoubleValue SPLASH_MIN_SPEED = B
            .comment("",
                     "Below this speed a cart makes no splash at all, so a parked one does not fountain.")
            .defineInRange("splash.minSpeed", 0.6, 0.0, 100.0);

    private static final ModConfigSpec.DoubleValue SPLASH_FULL_EFFECT_SPEED = B
            .comment("",
                     "The speed at which a splash is at its biggest and loudest, in blocks per second.")
            .defineInRange("splash.fullEffectSpeed", 24.0, 0.1, 500.0);

    private static final ModConfigSpec.DoubleValue SPLASH_SIDE_OFFSET = B
            .comment("",
                     "How far out to each side of the rail the spray erupts, in blocks. Look only.")
            .defineInRange("splash.sideOffset", 0.45, 0.0, 4.0);

    private static final ModConfigSpec.DoubleValue SPLASH_RAIL_OFFSET = B
            .comment("",
                     "How far out from the centre the OUTER splash sits, in blocks.",
                     "This is the one that lands past the side rails and sheets off them.",
                     "Set it equal to splash.sideOffset for a single narrow spout.")
            .defineInRange("splash.railOffset", 0.78, 0.0, 6.0);

    private static final ModConfigSpec.DoubleValue SPLASH_UNDER_DEPTH = B
            .comment("",
                     "How far below the rail the water curtain falls, in blocks.",
                     "0 keeps the splash on top of the track; higher drapes it underneath.")
            .defineInRange("splash.underDepth", 0.38, 0.0, 6.0);

    private static final ModConfigSpec.DoubleValue SPLASH_LENGTH_SPREAD = B
            .comment("",
                     "How far along the track the splash spreads either way, in blocks.",
                     "Turns a splash from a point into a sheet the length of the cart.",
                     "0 collapses every emitter back onto the contact point.")
            .defineInRange("splash.lengthSpread", 0.45, 0.0, 6.0);

    private static final ModConfigSpec.IntValue SPLASH_SOUND_COOLDOWN_TICKS = B
            .comment("",
                     "A cart plays one splash sound, then stays quiet for this many ticks (20 = 1 second),",
                     "so a long water section is a splash rather than a continuous roar.")
            .defineInRange("splash.soundCooldownTicks", 22, 1, 200);

    // ------------------------------------------------------------------ launch track

    private static final ModConfigSpec.DoubleValue LAUNCH_SPEED = B
            .comment("",
                     "How fast a Launch Track flings carts, in blocks per second.",
                     "Used when the Launch Speed dial has never been set. Unlike a boost, a launch",
                     "will start a cart that is standing still.")
            .defineInRange("launch.speed", 45.0, 0.0, 2000.0);

    private static final ModConfigSpec.DoubleValue LAUNCH_ACCELERATION = B
            .comment("",
                     "How violently it gets there, in blocks per second squared. This is what makes a",
                     "launch feel like a catapult instead of a strong boost.")
            .defineInRange("launch.acceleration", 130.0, 0.1, 5000.0);

    // ------------------------------------------------------------------ reverse track

    private static final ModConfigSpec.DoubleValue REVERSE_MIN_SPEED = B
            .comment("",
                     "Below this speed a cart on a Reverse Track is treated as parked and left alone,",
                     "rather than being flipped where it stands.")
            .defineInRange("reverse.minSpeed", 0.5, 0.0, 100.0);

    private static final ModConfigSpec.IntValue REVERSE_COOLDOWN_TICKS = B
            .comment("",
                     "After being flipped, a cart cannot be flipped again for this many ticks (20 = 1 second),",
                     "which is what stops it juddering back and forth on the same piece of track.",
                     "Raise it if long trains bounce on a reverse; it cannot be set to 0 for that reason.")
            .defineInRange("reverse.cooldownTicks", 15, 1, 200);

    // ------------------------------------------------------------------ driving a coaster

    private static final ModConfigSpec.DoubleValue DRIVER_RANGE = B
            .comment("",
                     "How far from a cart a seated player can be and still count as its driver, in blocks.",
                     "Keep it small -- a large value lets someone drive a coaster they are not actually on.")
            .defineInRange("driver.range", 3.5, 0.5, 16.0);

    private static final ModConfigSpec.DoubleValue DRIVER_MAX_SPEED = B
            .comment("",
                     "Top speed a coaster reaches under a driver's own throttle, in blocks per second.")
            .defineInRange("driver.maxSpeed", 28.0, 0.5, 1000.0);

    private static final ModConfigSpec.DoubleValue DRIVER_ACCELERATION = B
            .comment("",
                     "How quickly the throttle answers when a driver speeds up, in blocks per second squared.")
            .defineInRange("driver.acceleration", 14.0, 0.1, 1000.0);

    private static final ModConfigSpec.DoubleValue DRIVER_BRAKE = B
            .comment("",
                     "How quickly it slows when they ease off or pull back. Firmer than accelerating on",
                     "purpose, so S brakes first and only then reverses.")
            .defineInRange("driver.brake", 26.0, 0.1, 2000.0);

    private static final ModConfigSpec.DoubleValue DRIVER_HANDBRAKE = B
            .comment("",
                     "How hard the spacebar handbrake bites, in blocks per second squared.",
                     "Firmer again, so reaching for the brake reads as a deliberate stop.")
            .defineInRange("driver.handbrake", 42.0, 0.1, 2000.0);

    private static final ModConfigSpec.DoubleValue DRIVER_STOPPED_SPEED = B
            .comment("",
                     "Below this speed the coaster counts as stopped, and a driver setting off picks their",
                     "direction by facing that way. Keep it well under a walking pace.")
            .defineInRange("driver.stoppedSpeed", 0.35, 0.01, 5.0);

    public static final ModConfigSpec SPEC = B.build();

    // ------------------------------------------------------------------ boost track

    public static double boostSpeed() {
        return BOOST_SPEED.get();
    }

    public static double boostAcceleration() {
        return BOOST_ACCELERATION.get();
    }

    // ------------------------------------------------------------------ rainbow track

    public static double rainbowColourScale() {
        return RAINBOW_COLOUR_SCALE.get();
    }

    // ---------------------------------------------------------------- bobsled track

    public static double bobsledMinSpeed() {
        return BOBSLED_MIN_SPEED.get();
    }

    public static double bobsledMaxBankDegrees() {
        return BOBSLED_MAX_BANK.get();
    }

    public static double bobsledBankStiffness() {
        return BOBSLED_STIFFNESS.get();
    }

    public static double bobsledBankDamping() {
        return BOBSLED_DAMPING.get();
    }

    public static double bobsledGravity() {
        return BOBSLED_GRAVITY.get();
    }

    /** +1 normally, -1 when the config asks for the lean to be flipped. */
    public static double bobsledBankSign() {
        return BOBSLED_INVERT.get() ? -1.0 : 1.0;
    }

    // ---------------------------------------------------------- powered boost track

    public static double poweredBoostSpeed() {
        return POWERED_BOOST_SPEED.get();
    }

    public static double poweredBoostAcceleration() {
        return POWERED_BOOST_ACCELERATION.get();
    }

    // ------------------------------------------------------------------ brake track

    public static double brakeTargetSpeed() {
        return BRAKE_TARGET_SPEED.get();
    }

    public static double brakeDeceleration() {
        return BRAKE_DECELERATION.get();
    }

    public static double brakeStopDistance() {
        return BRAKE_STOP_DISTANCE.get();
    }

    public static double brakeDecelerationMax() {
        return BRAKE_DECELERATION_MAX.get();
    }

    public static double brakeSparkSpeed() {
        return BRAKE_SPARK_SPEED.get();
    }

    /**
     * Forced above {@link #brakeSparkSpeed()} rather than trusted as written.
     *
     * <p>The brake's effect strength is {@code (speed - spark) / (full - spark)}. Setting the two
     * equal makes that 0/0, which is NaN, and a NaN particle spread is not something the rest of
     * the code checks for. Ranges alone cannot express "must be greater than that other setting",
     * so it is enforced here instead.
     */
    public static double brakeFullEffectSpeed() {
        return Math.max(BRAKE_FULL_EFFECT_SPEED.get(), brakeSparkSpeed() + 0.01);
    }

    public static double brakeGlowOverspeed() {
        return BRAKE_GLOW_OVERSPEED.get();
    }

    public static double brakeDeadStopSpeed() {
        return BRAKE_DEAD_STOP_SPEED.get();
    }

    // ------------------------------------------------------------------ station track

    public static double stationLaunchSpeed() {
        return STATION_LAUNCH_SPEED.get();
    }

    public static double stationLaunchRate() {
        return STATION_LAUNCH_RATE.get();
    }

    public static double stationApproachDeceleration() {
        return STATION_APPROACH_DECELERATION.get();
    }

    public static double stationMaxDeceleration() {
        return STATION_MAX_DECELERATION.get();
    }

    public static double stationArriveDistance() {
        return STATION_ARRIVE_DISTANCE.get();
    }

    public static double stationCreepSpeed() {
        return STATION_CREEP_SPEED.get();
    }

    public static double stationCreepAcceleration() {
        return STATION_CREEP_ACCELERATION.get();
    }

    public static double stationStoppedSpeed() {
        return STATION_STOPPED_SPEED.get();
    }

    public static double stationDwellSeconds() {
        return STATION_DWELL_SECONDS.get();
    }

    public static double stationBrakeEffectSpeed() {
        return STATION_BRAKE_EFFECT_SPEED.get();
    }

    public static int stationDispatchWindowTicks() {
        return STATION_DISPATCH_WINDOW_TICKS.get();
    }

    /**
     * Never shorter than the dispatch window.
     *
     * <p>Forgetting a station drops the record of it having just dispatched, so a shorter value
     * than the window would let a station arrest the very train it had released.
     */
    public static int stationForgetAfterTicks() {
        return Math.max(STATION_FORGET_AFTER_TICKS.get(), stationDispatchWindowTicks());
    }

    // ------------------------------------------------------------------ slippery track

    public static double slipperyRecoverFraction() {
        return SLIPPERY_RECOVER.get();
    }

    public static double slipperyMaxSpeed() {
        return SLIPPERY_MAX_SPEED.get();
    }

    public static double slipperyMinSpeed() {
        return SLIPPERY_MIN_SPEED.get();
    }

    // ------------------------------------------------------------------ splash track

    public static double splashDrag() {
        return SPLASH_DRAG.get();
    }

    public static double splashMinSpeed() {
        return SPLASH_MIN_SPEED.get();
    }

    public static double splashFullEffectSpeed() {
        return SPLASH_FULL_EFFECT_SPEED.get();
    }

    public static double splashRailOffset() {
        return SPLASH_RAIL_OFFSET.get();
    }

    public static double splashUnderDepth() {
        return SPLASH_UNDER_DEPTH.get();
    }

    public static double splashLengthSpread() {
        return SPLASH_LENGTH_SPREAD.get();
    }

    public static double splashSideOffset() {
        return SPLASH_SIDE_OFFSET.get();
    }

    public static int splashSoundCooldownTicks() {
        return SPLASH_SOUND_COOLDOWN_TICKS.get();
    }

    // ------------------------------------------------------------------ launch track

    public static double launchSpeed() {
        return LAUNCH_SPEED.get();
    }

    public static double launchAcceleration() {
        return LAUNCH_ACCELERATION.get();
    }

    // ------------------------------------------------------------------ reverse track

    public static double reverseMinSpeed() {
        return REVERSE_MIN_SPEED.get();
    }

    public static int reverseCooldownTicks() {
        return REVERSE_COOLDOWN_TICKS.get();
    }

    // ------------------------------------------------------------------ driving a coaster

    public static double driverRange() {
        return DRIVER_RANGE.get();
    }

    /** Squared here so the caller can compare against a squared distance without a sqrt. */
    public static double driverRangeSq() {
        double r = DRIVER_RANGE.get();
        return r * r;
    }

    public static double driverMaxSpeed() {
        return DRIVER_MAX_SPEED.get();
    }

    public static double driverAcceleration() {
        return DRIVER_ACCELERATION.get();
    }

    public static double driverBrake() {
        return DRIVER_BRAKE.get();
    }

    public static double driverHandbrake() {
        return DRIVER_HANDBRAKE.get();
    }

    public static double driverStoppedSpeed() {
        return DRIVER_STOPPED_SPEED.get();
    }

    private Config() {}
}
