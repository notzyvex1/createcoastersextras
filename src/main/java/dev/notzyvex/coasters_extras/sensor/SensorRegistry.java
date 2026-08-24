package dev.notzyvex.coasters_extras.sensor;

import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the Sensor Block, its item and its block entity.
 *
 * <p>Its own registry rather than a line in {@code ModTracks}: this is the first thing in the
 * mod with a block entity of its own, and the balloons reach theirs by borrowing the base
 * mod's type through a mixin, which is not a pattern to extend.
 */
public final class SensorRegistry {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CoastersExtras.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CoastersExtras.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CoastersExtras.MOD_ID);

    public static final DeferredBlock<SensorBlock> SENSOR_BLOCK =
            BLOCKS.register("sensor_block", () -> new SensorBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(1.5F)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()));

    public static final DeferredItem<SensorBlockItem> SENSOR_BLOCK_ITEM =
            ITEMS.register("sensor_block", () -> new SensorBlockItem(
                    SENSOR_BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SensorBlockEntity>>
            SENSOR_BLOCK_ENTITY = BLOCK_ENTITIES.register("sensor_block",
                    () -> BlockEntityType.Builder
                            .of(SensorBlockEntity::new, SENSOR_BLOCK.get())
                            .build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    private SensorRegistry() {}
}
