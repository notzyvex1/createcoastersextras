package dev.notzyvex.coasters_extras;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Tags used by the mixins to decide "is this a balloon?".
 *
 * <p>Tag-based rather than a hard-coded block list so that new balloons -- ours or another
 * addon's -- start working by adding a datapack entry, with no code change.
 */
public final class ModTags {

    public static final class Blocks {
        public static final TagKey<Block> BALLOONS = TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "balloons"));
        private Blocks() {}
    }

    public static final class Items {
        public static final TagKey<Item> BALLOONS = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(CoastersExtras.MOD_ID, "balloons"));
        private Items() {}
    }

    private ModTags() {}
}
