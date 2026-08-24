package dev.notzyvex.coasters_extras.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.notzyvex.coasters_extras.client.BrakingCarts;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "dev.silvergold.simulatedcoasters.client.cart.CoasterCartWheelAxisRenderer",
       remap = false)
public class BrakingWheelTintMixin {

    @Unique private static final float COASTERS_EXTRAS$TINT_R = 1.00F;
    @Unique private static final float COASTERS_EXTRAS$TINT_G = 0.22F;
    @Unique private static final float COASTERS_EXTRAS$TINT_B = 0.18F;

    @ModifyArgs(
            method = "renderOneAxis",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;"
                           + "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"
                           + "Lcom/mojang/blaze3d/vertex/VertexConsumer;"
                           + "Lnet/minecraft/world/level/block/state/BlockState;"
                           + "Lnet/minecraft/client/resources/model/BakedModel;"
                           + "FFFII"
                           + "Lnet/neoforged/neoforge/client/model/data/ModelData;"
                           + "Lnet/minecraft/client/renderer/RenderType;)V"
            ),
            require = 1
    )
    private static void coasters_extras$tintBrakingWheels(
            Args args,
            @Local(argsOnly = true) Vec3 worldOrigin) {
        try {
            if (worldOrigin == null || !BrakingCarts.isBraking(worldOrigin)) {
                return;
            }
            float r = args.<Float>get(4);
            float g = args.<Float>get(5);
            float b = args.<Float>get(6);
            args.set(4, r * COASTERS_EXTRAS$TINT_R);
            args.set(5, g * COASTERS_EXTRAS$TINT_G);
            args.set(6, b * COASTERS_EXTRAS$TINT_B);
        } catch (Throwable ignored) {
        }
    }
}
