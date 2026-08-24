package dev.notzyvex.coasters_extras.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;

/**
 * Changes the material of a curve that is already in the world.
 *
 * <p>{@code BezierConnection} keeps its material in a non-final protected field with a public
 * {@code setMaterial}, so a placed curve genuinely can be repainted -- no rebuilding, no
 * breaking and replacing, and the geometry is untouched.
 *
 * <p>Both ends have to be done. Each anchorpoint stores its own copy of the connection to its
 * peer, and repainting only the one you clicked leaves the ride two different colours
 * depending on which end you look from.
 */
public final class TrackRepaint {

    /** @return true if a curve was found and repainted. */
    public static boolean repaint(ServerLevel level, BlockPos anchor, BlockPos peer,
                                  TrackMaterial material) {
        boolean any = repaintOneEnd(level, anchor, peer, material);
        // Not `&&` -- the second end must be attempted even if the first had nothing, or a
        // curve stored on only one side would half-repaint and stay mismatched forever.
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
        // setChanged alone only marks it for saving. Clients are already rendering the old
        // material and will keep doing so until the block entity is re-sent.
        level.sendBlockUpdated(from, be.getBlockState(), be.getBlockState(), 3);
        return true;
    }

    /** The anchorpoint's curve map, or null if this is not an anchorpoint. */
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
