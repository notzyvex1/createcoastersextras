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

public class CoasterControlsRenderer implements BlockEntityRenderer<CoasterControlsBlockEntity> {

    public static final ModelResourceLocation HANDLE = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID,
                    "block/coaster_controls_handle"));

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

        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facingDegrees(facing)));
        ps.translate(-0.5, -0.5, -0.5);

        ps.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        ps.mulPose(Axis.XP.rotationDegrees(angle));
        ps.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);

        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                ps.last(), buffer.getBuffer(RenderType.cutout()), state, model,
                1f, 1f, 1f, light, overlay);

        ps.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CoasterControlsBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.0);
    }

    private static float facingDegrees(Direction facing) {
        return switch (facing) {
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
    }
}
