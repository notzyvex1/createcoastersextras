package dev.notzyvex.coasters_extras.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResourceLocation.class)
public abstract class ResourceLocationCoasterAliasMixin {

    @Shadow @org.spongepowered.asm.mixin.Final private String namespace;
    @Shadow @org.spongepowered.asm.mixin.Final private String path;

    private static final String OURS   = "coasters_extras";
    private static final String THEIRS = "simulatedcoasters";
    private static final String COASTER_TRACK = "coaster_track";

    @Inject(method = "equals", at = @At("HEAD"), cancellable = true)
    private void coasters_extras$aliasCoasterTrack(Object other,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (!(other instanceof ResourceLocation o)) {
            return;
        }
        boolean weAreOurs   = OURS.equals(this.namespace);
        boolean weAreTheirs = !weAreOurs && THEIRS.equals(this.namespace);
        if (!weAreOurs && !weAreTheirs) {
            return;
        }

        if (weAreOurs) {
            if (this.path.endsWith("_track")
                    && THEIRS.equals(o.getNamespace())
                    && COASTER_TRACK.equals(o.getPath())) {
                cir.setReturnValue(true);
            }
            return;
        }

        if (COASTER_TRACK.equals(this.path)
                && OURS.equals(o.getNamespace())
                && o.getPath().endsWith("_track")) {
            cir.setReturnValue(true);
        }
    }
}
