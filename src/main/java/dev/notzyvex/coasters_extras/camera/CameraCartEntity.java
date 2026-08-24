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

public class CameraCartEntity extends Entity {

    public static final float DEFAULT_SPEED = 4.0f;

    private static final double SNAP_RANGE = 3.0;

    private static final EntityDataAccessor<Float> SPEED =
            SynchedEntityData.defineId(CameraCartEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RUNNING =
            SynchedEntityData.defineId(CameraCartEntity.class, EntityDataSerializers.BOOLEAN);

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
            Vec3 ahead = pointOnEdge(edge, t > 1.0 ? 1.0 : 0.0)
                    .add(headingAt(t > 1.0 ? 0.98 : 0.02).scale(direction * 0.35));
            setPos(ahead.x, ahead.y, ahead.z);
            if (!snapToTrack()) {
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

    private Vec3 pointOnEdge(CoasterPathEdge e, double at) {
        if (e.bezier() != null) {
            return e.bezier().getPosition((float) Math.max(0, Math.min(1, at)));
        }
        return e.straightFromWorld().lerp(e.straightToWorld(), Math.max(0, Math.min(1, at)));
    }

    private Vec3 headingAt(double at) {
        Vec3 back = pointOnEdge(edge, Math.max(0, at - 0.02));
        Vec3 fwd = pointOnEdge(edge, Math.min(1, at + 0.02));
        Vec3 delta = fwd.subtract(back);
        return delta.lengthSqr() < 1.0e-9 ? Vec3.ZERO : delta.normalize();
    }

    private double edgeLength(CoasterPathEdge e) {
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
        snapToTrack();
    }
}
