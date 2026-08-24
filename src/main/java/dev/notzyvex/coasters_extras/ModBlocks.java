package dev.notzyvex.coasters_extras;

import dev.silvergold.simulatedcoasters.balloon.RedBalloonBlock;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CoastersExtras.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CoastersExtras.MOD_ID);

    public static final Map<BalloonColor, DeferredBlock<ExtraBalloonBlock>> BALLOONS =
            new EnumMap<>(BalloonColor.class);
    public static final Map<BalloonColor, DeferredItem<Item>> BALLOON_ITEMS =
            new EnumMap<>(BalloonColor.class);

    static {
        for (BalloonColor color : BalloonColor.values()) {
            DeferredBlock<ExtraBalloonBlock> block = BLOCKS.register(color.blockName(),
                    () -> new ExtraBalloonBlock(color, RedBalloonBlock.defaultProperties()));
            BALLOONS.put(color, block);
            BALLOON_ITEMS.put(color,
                    ITEMS.register(color.blockName(),
                            () -> new BalloonBlockItem(block.get(), new Item.Properties(), color)));
        }
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private ModBlocks() {}
}
