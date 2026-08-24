package dev.notzyvex.coasters_extras.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.notzyvex.coasters_extras.CoastersExtras;
import dev.notzyvex.coasters_extras.control.CoasterControlsBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the Coaster Controls handle, and only the handle, at a continuously eased angle.
 *
 * <p>The console body is a normal baked block model and renders itself. The handle is a
 * separate model this class rotates every frame, which is what a block state alone could not
 * do: block-state model swaps are limited to Minecraft's 22.5-degree steps and snap between
 * them, so the lever jumped. Rendered here it turns by any angle and glides, the way Create's
 * Train Controls lever does.
 */
public class CoasterControlsRenderer implements BlockEntityRenderer<CoasterControlsBlockEntity> {

    /** The handle-only model, registered as a standalone so it bakes without a block state. */
    public static final ModelResourceLocation HANDLE = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID,
                    "block/coaster_controls_handle"));

    /**
     * Where the handle hinges, in block units (0-1). Taken from the source model, not judged.
     *
     * <p>The lever's rest pose is a 45-degree rotation baked into both of its elements about
     * {@code origin [3, 12, 11]} -- the origin of the {@code lever} group in the Blockbench file.
     * The runtime swing has to turn about that same line, because two rotations about different
     * lines do not compose into a hinge: the lever would shear away from the console as it moved.
     *
     * <p>So Y and Z are the baked origin. X is free -- a rotation about the X axis leaves the X
     * coordinate untouched -- and is left at the block's centre.
     *
     * <p>Both numbers previously came from the arm's raw {@code from}/{@code to} box instead,
     * which was only ever right while the model was flat. It was flat because the importer was
     * discarding every rotation in the file, and a hinge derived from a broken model is a hinge
     * derived twice from the same bug.
     */
    private static final float PIVOT_X = 8f / 16f;
    private static final float PIVOT_Y = 12f / 16f;
    private static final float PIVOT_Z = 11f / 16f;

    public CoasterControlsRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CoasterControlsBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        float angle = be.handleAngle(partialTick);

        BakedModel model = Minecraft.getInstance().getModelManager().getModel(HANDLE);

        ps.pushPose();

        // Face the same way the console does. The block state rotates the console model by
        // y = 0/90/180/270 for north/east/south/west; the handle has to turn with it.
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facingDegrees(facing)));
        ps.translate(-0.5, -0.5, -0.5);

        // Swing the handle about its hinge.
        ps.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        ps.mulPose(Axis.XP.rotationDegrees(angle));
        ps.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);

        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                ps.last(), buffer.getBuffer(RenderType.cutout()), state, model,
                1f, 1f, 1f, light, overlay);

        ps.popPose();
    }

    /**
     * The lever leaves the block, so the box the game culls against has to as well.
     *
     * <p>With its 45-degree rest pose the grab bar already reaches about 1.33 blocks up, and full
     * forward throw takes it higher still. The default is the single block the entity sits in, so
     * at grazing camera angles the lever was frustum-culled -- it vanished while the console
     * stayed drawn, which reads as the handle falling off rather than as a culling artefact.
     *
     * <p>On this version this hook is on the RENDERER, via NeoForge's
     * {@code IBlockEntityRendererExtension}, not on the block entity.
     */
    @Override
    public AABB getRenderBoundingBox(CoasterControlsBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.0);
    }

    /** Matches the block state's y rotation: north 0, east 90, south 180, west 270. */
    private static float facingDegrees(Direction facing) {
        return switch (facing) {
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
    }
}
