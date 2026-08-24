package dev.notzyvex.coasters_extras.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.notzyvex.coasters_extras.client.BrakingCarts;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Red wheels while a cart is braking -- the render half of the feature.
 *
 * <h2>The three pieces, and which one this is</h2>
 *
 * <ol>
 *   <li>{@code CoasterCartDriveMixin} knows, server-side, when a cart is gripping a Brake Track
 *       (its {@code brake_track} case calls {@code BrakingTracker.report} once the overspeed
 *       passes 2.0 b/s), </li>
 *   <li>{@code BrakingTracker} / {@code BrakingPayload} / {@code BrakingCarts} carry those
 *       positions to the client and cache them there, and</li>
 *   <li><b>this mixin</b> is the last piece: it tints the wheel model as it is drawn.</li>
 * </ol>
 *
 * <h2>Where the tint actually goes in (verified against 0.1.4 bytecode)</h2>
 *
 * The whole class funnels through one private helper:
 *
 * <pre>
 *   private static void renderOneAxis(
 *       PoseStack, ModelBlockRenderer, MultiBufferSource, BlockState, BakedModel,
 *       float cr, float cg, float cb, int light, RandomSource,
 *       Vec3 worldOrigin, float camX, float camY, float camZ,
 *       Quaternionf, boolean, double, float, float, float)
 * </pre>
 *
 * and there is exactly <b>one</b> {@code ModelBlockRenderer.renderModel} call in the entire
 * class -- at offset 191 inside {@code renderOneAxis}, in the loop over
 * {@code BakedModel.getRenderTypes()}. It is the <i>NeoForge</i> overload, the one that takes
 * {@code ModelData} and {@code RenderType} on the end, not vanilla's nine-argument form:
 *
 * <pre>
 *   renderModel(PoseStack$Pose, VertexConsumer, BlockState, BakedModel,
 *               float r, float g, float b, int light, int overlay,
 *               ModelData, RenderType)
 * </pre>
 *
 * Both callers reach it here: {@code renderCell} calls {@code renderOneAxis} eight times
 * (four axes x two wheel rows) for a cart running on track, and {@code renderStandingAt}
 * reaches it via {@code lambda$renderStandingAt$0} for a cart block sitting on the ground.
 * Tinting the one call site therefore covers every wheel the mod draws.
 *
 * <p><b>Note for the next compatibility pass:</b> the deferred-work note in {@code PROJECT.md}
 * guessed that this {@code renderModel} call lived inside a lambda -- an
 * {@code invokedynamic} building a {@code WheelPlacementConsumer}. It does not. That
 * {@code invokedynamic} is in {@code renderStandingAt}, and the lambda it builds
 * ({@code lambda$renderStandingAt$0}) only forwards to {@code renderOneAxis}; the draw call
 * itself is in {@code renderOneAxis} proper. Nothing here has to target synthetic code.
 *
 * <h2>Why {@code @ModifyArgs} rather than {@code @Redirect}</h2>
 *
 * A redirect would have to swallow the call and re-issue all eleven arguments by hand, and it
 * claims the call site exclusively -- two mods redirecting the same {@code INVOKE} is a hard
 * conflict. {@code @ModifyArgs} edits the colour floats in place, leaves the base mod's call
 * exactly as written, and stacks with anything else touching the same instruction.
 *
 * <h2>How the wheel finds out whose cart it is on</h2>
 *
 * It does not, and it does not need to. {@code renderOneAxis} is handed no cart identity --
 * only a {@code Vec3 worldOrigin}, which is the world-space anchor it translates the pose to.
 * That parameter is in scope at the draw call and is reachable with MixinExtras'
 * {@code @Local(argsOnly = true)}: it is the only {@code Vec3} argument the method takes, so
 * the match is unambiguous. {@code BrakingCarts.isBraking} then answers with a distance check
 * against the handful of positions the server last published -- a short linear scan over a
 * list that is usually empty, not the {@code nearestGraphHit} spatial search that made this
 * feature look expensive when it was first sketched.
 *
 * <p>{@code BrakingCarts} expires its own entries after 400 ms, so if the packets stop for any
 * reason -- cart left the brake, dimension change, connection hiccup -- the glow fades on its
 * own instead of sticking to a wheel forever.
 *
 * <h2>{@code require = 1} is deliberate</h2>
 *
 * A cosmetic injector is the obvious candidate for {@code require = 0}, but the mod's
 * dependency is pinned to {@code [0.1.2,0.2.0)}, so nobody can be running a base mod where
 * this call site has moved. Inside that range a failure to inject can only mean we read the
 * bytecode wrong, and a startup crash naming this class is a far better way to learn that than
 * wheels that quietly never turn red. Flip it to 0 if the pin is ever widened.
 */
@Mixin(targets = "dev.silvergold.simulatedcoasters.client.cart.CoasterCartWheelAxisRenderer",
       remap = false)
public class BrakingWheelTintMixin {

    /**
     * Multipliers applied to the incoming {@code cr, cg, cb}. Red is left alone and the other
     * two are pulled most of the way down, so the result reads as hot metal against the plain
     * grey wheel rather than a flat red repaint. Tuning knob -- this is the dial to turn if it
     * looks wrong in game.
     */
    @Unique private static final float COASTERS_EXTRAS$TINT_R = 1.00F;
    @Unique private static final float COASTERS_EXTRAS$TINT_G = 0.22F;
    @Unique private static final float COASTERS_EXTRAS$TINT_B = 0.18F;

    /**
     * Tints the wheel model red if this axis belongs to a cart that is currently braking.
     *
     * <p>Argument indices are into the redirected call, receiver excluded:
     * 0 {@code Pose}, 1 {@code VertexConsumer}, 2 {@code BlockState}, 3 {@code BakedModel},
     * <b>4 r, 5 g, 6 b</b>, 7 light, 8 overlay, 9 {@code ModelData}, 10 {@code RenderType}.
     *
     * <p>The colours are multiplied rather than replaced so that whatever tint the base mod
     * (or another addon) already chose still composes -- a wheel drawn dark stays dark, it
     * just goes dark red.
     */
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
            // This runs per wheel axis, per cart, per frame. Anything unexpected in here would
            // be a log line sixty times a second and a dead client, so it is swallowed: the
            // wheel simply draws in its normal colour.
        }
    }
}
