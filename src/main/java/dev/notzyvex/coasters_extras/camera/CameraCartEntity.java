package dev.notzyvex.coasters_extras.camera;

import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphLookup;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A camera that rides the track, for filming.
 *
 * <p><b>Deliberately not a coaster cart.</b> A real cart is a Sable sublevel with the base mod's
 * {@code COASTER_CART} block attached, and twenty-four classes in that mod compare against that
 * exact block by identity rather than by type -- resolve, track snap, placement collision, link
 * placement, the world renderer, the bearing cache. Anything that is not literally their block
 * places fine and is then invisible to every system that makes a cart a cart.
 *
 * <p>Which turns out to be a gift rather than a limitation. <b>A camera should not follow a cart
 * anyway.</b> A cart is a physics body, and physics jitters -- unnoticeable when you are sitting in
 * one, motion sickness through a lens. This follows the track SPLINE instead, sampling the same
 * Bezier the carts are constrained to. Smooth by construction rather than smoothed afterwards, and
 * with no dependency on the cart system at all.
 */
public class CameraCartEntity extends Entity {

    /** Metres per second along the track. Slow by default -- a dolly shot, not a ride. */
    public static final float DEFAULT_SPEED = 4.0f;

    /** How far from a track a camera may be placed and still find it. */
    private static final double SNAP_RANGE = 3.0;

    private static final EntityDataAccessor<Float> SPEED =
            SynchedEntityData.defineId(CameraCartEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RUNNING =
            SynchedEntityData.defineId(CameraCartEntity.class, EntityDataSerializers.BOOLEAN);

    /** Where along the current edge we are, 0..1. */
    private double t;
    private CoasterPathEdge edge;
    private int direction = 1;

    public CameraCartEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPEED, DEFAULT_SPEED);
        builder.define(RUNNING, false);
    }

    public float speed() {
        return entityData.get(SPEED);
    }

    public void setSpeed(float blocksPerSecond) {
        entityData.set(SPEED, Math.max(0.1f, Math.min(40f, blocksPerSecond)));
    }

    public boolean running() {
        return entityData.get(RUNNING);
    }

    public void setRunning(boolean running) {
        entityData.set(RUNNING, running);
    }

    /**
     * Put the camera on the nearest track.
     *
     * @return false if there is no track within reach, so the caller can say so rather than
     *         leaving a camera hanging in the air doing nothing.
     */
    public boolean snapToTrack() {
        CoasterPathGraph graph = CoasterPathGraphLookup.get(level());
        if (graph == null) {
            return false;
        }
        CoasterPathTrackFrame.GraphHit hit =
                CoasterPathTrackFrame.nearestGraphHit(level(), graph, position(), SNAP_RANGE);
        if (hit == null || hit.edge() == null) {
            return false;
        }
        edge = hit.edge();
        t = hit.t();
        setPos(hit.point().x, hit.point().y, hit.point().z);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !running() || edge == null) {
            return;
        }

        double length = edgeLength(edge);
        if (length <= 1.0e-4) {
            return;
        }
        t += (speed() / 20.0 / length) * direction;

        if (t > 1.0 || t < 0.0) {
            // Rather than walking the graph to find the next edge -- which needs the base mod's
            // node topology and would couple this to it -- the camera re-snaps from the point just
            // past the end of the current edge. The graph lookup finds whatever edge is there,
            // including across a junction, and it costs one query per edge rather than per tick.
            Vec3 ahead = pointOnEdge(edge, t > 1.0 ? 1.0 : 0.0)
                    .add(headingAt(t > 1.0 ? 0.98 : 0.02).scale(direction * 0.35));
            setPos(ahead.x, ahead.y, ahead.z);
            if (!snapToTrack()) {
                // End of the line. Stop rather than reverse: a camera that turns round mid-shot
                // has ruined the take, whereas one that stops has simply finished it.
                setRunning(false);
                return;
            }
            return;
        }

        Vec3 at = pointOnEdge(edge, t);
        setPos(at.x, at.y, at.z);

        Vec3 heading = headingAt(t).scale(direction);
        if (heading.lengthSqr() > 1.0e-9) {
            setYRot((float) (Math.atan2(heading.z, heading.x) * (180 / Math.PI)) - 90f);
            setXRot((float) -(Math.asin(Math.max(-1, Math.min(1, heading.normalize().y)))
                    * (180 / Math.PI)));
        }
    }

    /** A point on an edge. Curves sample their Bezier; straights interpolate their two ends. */
    private Vec3 pointOnEdge(CoasterPathEdge e, double at) {
        if (e.bezier() != null) {
            return e.bezier().getPosition((float) Math.max(0, Math.min(1, at)));
        }
        return e.straightFromWorld().lerp(e.straightToWorld(), Math.max(0, Math.min(1, at)));
    }

    /** Direction of travel at `at`, by sampling a short way either side. */
    private Vec3 headingAt(double at) {
        Vec3 back = pointOnEdge(edge, Math.max(0, at - 0.02));
        Vec3 fwd = pointOnEdge(edge, Math.min(1, at + 0.02));
        Vec3 delta = fwd.subtract(back);
        return delta.lengthSqr() < 1.0e-9 ? Vec3.ZERO : delta.normalize();
    }

    private double edgeLength(CoasterPathEdge e) {
        // Sampled rather than analytic: a Bezier has no closed-form arc length, and sixteen
        // samples is well inside a pixel over the length an edge can be.
        double total = 0;
        Vec3 previous = pointOnEdge(e, 0);
        for (int i = 1; i <= 16; i++) {
            Vec3 next = pointOnEdge(e, i / 16.0);
            total += next.distanceTo(previous);
            previous = next;
        }
        return total;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("T", t);
        tag.putInt("Direction", direction);
        tag.putFloat("Speed", speed());
        tag.putBoolean("Running", running());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        t = tag.getDouble("T");
        direction = tag.getInt("Direction") >= 0 ? 1 : -1;
        setSpeed(tag.getFloat("Speed"));
        setRunning(tag.getBoolean("Running"));
        // The edge itself is NOT saved -- it is a runtime object owned by the base mod's graph,
        // and the graph is rebuilt on load. Re-snapping from the saved position finds the
        // equivalent edge without having to serialise theirs.
        snapToTrack();
    }
}
