package dev.notzyvex.coasters_extras.track;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackMaterialFactory;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registers the 16 cosmetic track variants through Create's public
 * {@link TrackMaterialFactory} -- the same entry point Create: Coasters Simulated uses for
 * its coaster track, and Create: Steam 'n' Rails uses for its ~20 variants.
 *
 * <p>{@code standardModels()} makes Create look for models under
 * {@code coasters_extras:block/track/<name>_track/}, which is exactly where the generated
 * model sets live.
 *
 * <p>These use Create's own {@link TrackBlockItem}, so they place and connect like any
 * Create track. They are deliberately NOT wired into the base mod's anchorpoint system:
 * that item is a connector operating on existing track, not a placer, so pointing it at a
 * different material needs separate work on their connection logic.
 */
public final class ModTrackVariants {

    public static final Map<TrackVariant, TrackMaterial> MATERIALS =
            new EnumMap<>(TrackVariant.class);
    public static final Map<TrackVariant, DeferredBlock<TrackBlock>> BLOCKS =
            new EnumMap<>(TrackVariant.class);
    public static final Map<TrackVariant, DeferredItem<Item>> ITEMS =
            new EnumMap<>(TrackVariant.class);

    static {
        for (TrackVariant variant : TrackVariant.values()) {
            MATERIALS.put(variant, TrackMaterialFactory
                    .make(ResourceLocation.fromNamespaceAndPath(
                            CoastersExtras.MOD_ID, variant.trackName()))
                    .lang(variant.displayName())
                    .block(NonNullSupplier.lazy(() -> () -> BLOCKS.get(variant).get()))
                    .particle(ResourceLocation.fromNamespaceAndPath(
                            "create", "block/industrial_iron_block"))
                    .standardModels()
                    .noRecipeGen()
                    .build());
        }
    }

    public static void register() {
        for (TrackVariant variant : TrackVariant.values()) {
            DeferredBlock<TrackBlock> block = ModTracks.BLOCKS.register(
                    variant.blockName(),
                    () -> new VariantTrackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PODZOL)
                                    .strength(0.8F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(),
                            MATERIALS.get(variant)));
            BLOCKS.put(variant, block);

            ITEMS.put(variant, ModTracks.ITEMS.register(
                    variant.trackName(),
                    () -> new VariantTrackItem(new Item.Properties(), variant)));
        }
    }

    private ModTrackVariants() {}
}
