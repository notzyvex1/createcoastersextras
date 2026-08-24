package dev.notzyvex.coasters_extras.cart;

import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * The chassis parts, and the eight cart items built from them.
 *
 * <p>Parts are real blocks so they can live inside the cart's sublevel like any other block the
 * player might place there -- which means a player can also take one and build their own cart by
 * hand, rather than only using the eight presets.
 */
public final class ModCartParts {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CoastersExtras.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CoastersExtras.MOD_ID);

    public static final Map<CartPart, DeferredBlock<Block>> PARTS =
            new EnumMap<>(CartPart.class);
    public static final Map<String, DeferredItem<CartItem>> CARTS = new java.util.LinkedHashMap<>();

    /** Metal, but not stone-hard: a cart panel should come off quickly with a pickaxe. */
    private static BlockBehaviour.Properties chassis() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5F, 3.0F)
                .sound(SoundType.NETHERITE_BLOCK)
                // Not solid: parts are open frames and seats, so light and sight lines must pass
                // through them or a cart becomes a black box with people inside it.
                .noOcclusion();
    }

    static {
        for (CartPart part : CartPart.values()) {
            DeferredBlock<Block> block = BLOCKS.register(
                    "cart_" + part.id(), () -> new Block(chassis()));
            PARTS.put(part, block);
            ITEMS.register("cart_" + part.id(),
                    () -> new BlockItem(block.get(), new Item.Properties()));
        }
        for (CartPrefab prefab : CartPrefab.ALL) {
            CARTS.put(prefab.id(), ITEMS.register(prefab.id() + "_cart",
                    () -> new CartItem(new Item.Properties().stacksTo(16), prefab)));
        }
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private ModCartParts() {}
}
