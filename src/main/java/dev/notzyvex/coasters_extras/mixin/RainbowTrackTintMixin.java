package dev.notzyvex.coasters_extras.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.lib.instance.ColoredLitOverlayInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.silvergold.simulatedcoasters.client.track.bent.BentRailInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.silvergold.simulatedcoasters.client.track.CoasterCurveRailInstancing",
       remap = false)
public class RainbowTrackTintMixin {

    private static final double SWEEP = 0.85;

    @Inject(method = "apply", at = @At("TAIL"))
    private static void coasters_extras$rainbow(Vec3 visualPos,
                                                BezierConnection bc,
                                                TransformedInstance[] ties,
                                                @Nullable BentRailInstance[] bentLeft,
                                                @Nullable BentRailInstance[] bentRight,
                                                @Nullable BentRailInstance[] bentCenterBeam,
                                                int railDiffuseRgbPacked,
                                                int beamDiffuseRgbPacked,
                                                @Nullable Level level,
                                                CallbackInfo ci) {
        try {
            if (bc == null || bc.getMaterial() == null) return;
            var id = bc.getMaterial().id;
            if (!"coasters_extras".equals(id.getNamespace())
                    || !"rainbow_track".equals(id.getPath())) {
                return;
            }
            tint(bentLeft);
            tint(bentRight);
            tint(bentCenterBeam);
            tint(ties);
        } catch (Throwable ignored) {
        }
    }

    private static void tint(@Nullable Object[] arr) {
        if (arr == null || arr.length == 0) return;
        for (int i = 0; i < arr.length; i++) {
            if (!(arr[i] instanceof ColoredLitOverlayInstance inst)) continue;
            float hue = (float) ((double) i / arr.length * SWEEP);
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f);
            inst.red   = (byte) ((rgb >> 16) & 0xFF);
            inst.green = (byte) ((rgb >> 8) & 0xFF);
            inst.blue  = (byte) (rgb & 0xFF);
            if (inst instanceof Instance flywheel) {
                flywheel.setChanged();
            }
        }
    }
}
