package dev.notzyvex.coasters_extras.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;

public final class TrackRepaint {

    public static boolean repaint(ServerLevel level, BlockPos anchor, BlockPos peer,
                                  TrackMaterial material) {
        boolean any = repaintOneEnd(level, anchor, peer, material);
        any |= repaintOneEnd(level, peer, anchor, material);
        return any;
    }

    private static boolean repaintOneEnd(ServerLevel level, BlockPos from, BlockPos to,
                                         TrackMaterial material) {
        BlockEntity be = level.getBlockEntity(from);
        Map<BlockPos, BezierConnection> curves = curves(be);
        if (curves == null) {
            return false;
        }
        BezierConnection curve = curves.get(to);
        if (curve == null) {
            return false;
        }

        curve.setMaterial(material);
        be.setChanged();
        level.sendBlockUpdated(from, be.getBlockState(), be.getBlockState(), 3);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static Map<BlockPos, BezierConnection> curves(BlockEntity be) {
        if (be == null) {
            return null;
        }
        try {
            Object view = be.getClass().getMethod("getAnchorPeerCurvesView").invoke(be);
            return view instanceof Map ? (Map<BlockPos, BezierConnection>) view : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private TrackRepaint() {}
}
