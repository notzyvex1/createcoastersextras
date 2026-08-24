package dev.notzyvex.coasters_extras.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes our track ids compare equal to {@code simulatedcoasters:coaster_track}.
 *
 * <p><b>Why this exists.</b> Create: Coasters Simulated decides "is this coaster track?"
 * in roughly forty places across thirty files, always the same way:
 *
 * <pre>bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)</pre>
 *
 * Curve shape, banking, rail gauge, the centre beam, picking, bezier handles, cart
 * placement and drops all gate on it. A track material that is not literally theirs is
 * invisible to every one of those, which is why our tracks rendered with Create's plain
 * geometry no matter how faithfully their models and textures were copied -- verified
 * byte-identical, and it made no difference.
 *
 * <p>Patching forty call sites individually would also lose: the same {@code getMaterial()}
 * chooses which models to draw, so spoofing it wholesale gives their texture on all our
 * variants. Aliasing only the <em>id comparison</em> threads that needle -- their checks
 * pass, while our material still supplies its own models.
 *
 * <p>{@link ResourceLocation} is final, so an id subclass is impossible and this has to
 * live on {@code equals} itself. That method is extremely hot, so the guard is ordered to
 * fail in one string comparison for everything that is not ours.
 *
 * <p>Symmetric on purpose: without the reverse case {@code a.equals(b)} and
 * {@code b.equals(a)} would disagree, which breaks collections in ways that are miserable
 * to debug. {@code hashCode} is deliberately untouched, so map lookups still resolve to the
 * real entry and our materials remain distinct in {@code TrackMaterial.ALL}.
 */
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
        // Fast bail: neither side is one of the two namespaces we care about.
        boolean weAreOurs   = OURS.equals(this.namespace);
        boolean weAreTheirs = !weAreOurs && THEIRS.equals(this.namespace);
        if (!weAreOurs && !weAreTheirs) {
            return;
        }

        if (weAreOurs) {
            // our track id  ==  their coaster track
            if (this.path.endsWith("_track")
                    && THEIRS.equals(o.getNamespace())
                    && COASTER_TRACK.equals(o.getPath())) {
                cir.setReturnValue(true);
            }
            return;
        }

        // their coaster track  ==  our track id   (keeps equals symmetric)
        if (COASTER_TRACK.equals(this.path)
                && OURS.equals(o.getNamespace())
                && o.getPath().endsWith("_track")) {
            cir.setReturnValue(true);
        }
    }
}
