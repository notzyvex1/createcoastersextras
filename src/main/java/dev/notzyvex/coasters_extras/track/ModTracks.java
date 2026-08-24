package dev.notzyvex.coasters_extras.track;

import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTracks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CoastersExtras.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CoastersExtras.MOD_ID);

    /**
     * The placed-track block for the boost material.
     *
     * <p>Mirrors the base mod's {@code coaster_track_material}: it is the block Create
     * places when a track is laid, not the item you hold.
     */
    public static final DeferredBlock<BoostTrackBlock> BOOST_TRACK_MATERIAL =
            BLOCKS.register("boost_track_material",
                    () -> new BoostTrackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PODZOL)
                                    .strength(0.8F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(),
                            ModTrackMaterials.BOOST));

    /**
     * The item players hold to lay track.
     *
     * <p>Not a BlockItem: coaster track is laid between anchorpoints by the item's useOn,
     * so we extend their CoasterTrackItem to inherit the whole mechanic. Registering a
     * plain BlockItem here is why the boost track never appeared in the creative tab.
     */

    public static final DeferredItem<BoostTrackItem> BOOST_TRACK =
            ITEMS.register("boost_track", () -> new BoostTrackItem(new Item.Properties()));

    /** Sensor track: hazard-striped rails, emits redstone when a coaster passes. */
    public static final DeferredBlock<BoostTrackBlock> SENSOR_TRACK_MATERIAL =
            BLOCKS.register("sensor_track_material",
                    () -> new BoostTrackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_YELLOW)
                                    .strength(0.8F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(),
                            ModTrackMaterials.SENSOR));

    public static final DeferredItem<BoostTrackItem> SENSOR_TRACK =
            ITEMS.register("sensor_track", () -> new BoostTrackItem(new Item.Properties()));

    /** Brake track: hazard bars, slows coasters to a set speed. */
    public static final DeferredBlock<BoostTrackBlock> BRAKE_TRACK_MATERIAL =
            BLOCKS.register("brake_track_material",
                    () -> new BoostTrackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_YELLOW)
                                    .strength(0.8F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(),
                            ModTrackMaterials.BRAKE));

    public static final DeferredItem<BoostTrackItem> BRAKE_TRACK =
            ITEMS.register("brake_track", () -> new BoostTrackItem(new Item.Properties()));


    /** Station track: holds an arriving cart, then dispatches it. */
    public static final DeferredBlock<BoostTrackBlock> STATION_TRACK_MATERIAL =
            BLOCKS.register("station_track_material",
                    () -> new BoostTrackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                                    .strength(0.8F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(),
                            ModTrackMaterials.STATION));

    public static final DeferredItem<BoostTrackItem> STATION_TRACK =
            ITEMS.register("station_track", () -> new BoostTrackItem(new Item.Properties()));

    /** Slippery track: sheds almost no speed, so momentum carries further. */
    public static final DeferredBlock<BoostTrackBlock> SLIPPERY_TRACK_MATERIAL =
            BLOCKS.register("slippery_track_material",
                    () -> new BoostTrackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.ICE)
                                    .strength(0.8F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(),
                            ModTrackMaterials.SLIPPERY));

    public static final DeferredItem<BoostTrackItem> SLIPPERY_TRACK =
            ITEMS.register("slippery_track", () -> new BoostTrackItem(new Item.Properties()));

    /**
     * Bobsled track: the cart rides free in a trough, banking up the walls. The physics come
     * from {@code CoasterGuideBobsledMixin}, which relaxes the rail guide for this material.
     */
    public static final DeferredBlock<BoostTrackBlock> BOBSLED_TRACK_MATERIAL =
            BLOCKS.register("bobsled_track_material",
                    () -> new BoostTrackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                                    .strength(0.8F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(),
                            ModTrackMaterials.BOBSLED));

    public static final DeferredItem<BoostTrackItem> BOBSLED_TRACK =
            ITEMS.register("bobsled_track", () -> new BoostTrackItem(new Item.Properties()));

    /** Splash track: water rails that throw a splash off both sides as a cart passes. */
    public static final DeferredBlock<BoostTrackBlock> SPLASH_TRACK_MATERIAL =
            BLOCKS.register("splash_track_material",
                    () -> new BoostTrackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WATER)
                                    .strength(0.8F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(),
                            ModTrackMaterials.SPLASH));

    public static final DeferredItem<BoostTrackItem> SPLASH_TRACK =
            ITEMS.register("splash_track", () -> new BoostTrackItem(new Item.Properties()));

    /** Launch track: a hard kick from standstill up to a high speed. */
    public static final DeferredBlock<BoostTrackBlock> LAUNCH_TRACK_MATERIAL =
            BLOCKS.register("launch_track_material",
                    () -> new BoostTrackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_ORANGE)
                                    .strength(0.8F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(),
                            ModTrackMaterials.LAUNCH));

    public static final DeferredItem<BoostTrackItem> LAUNCH_TRACK =
            ITEMS.register("launch_track", () -> new BoostTrackItem(new Item.Properties()));

    /** Reverse track: flips the cart's direction once per pass. */
    public static final DeferredBlock<BoostTrackBlock> REVERSE_TRACK_MATERIAL =
            BLOCKS.register("reverse_track_material",
                    () -> new BoostTrackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_PURPLE)
                                    .strength(0.8F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(),
                            ModTrackMaterials.REVERSE));

    public static final DeferredItem<BoostTrackItem> REVERSE_TRACK =
            ITEMS.register("reverse_track", () -> new BoostTrackItem(new Item.Properties()));

    /**
     * The Coaster Controls block, and its item under the same id it always had.
     *
     * <p>Keeping the id means existing recipes and anything already in an inventory carry
     * straight over; only the class behind it changed, from a hand-held item to a block you
     * place on the cart.
     */
    public static final DeferredBlock<dev.notzyvex.coasters_extras.control.CoasterControlsBlock>
            COASTER_CONTROLS_BLOCK = BLOCKS.register("coaster_controls",
                    () -> new dev.notzyvex.coasters_extras.control.CoasterControlsBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(1.2F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));

    public static final DeferredItem<dev.notzyvex.coasters_extras.control.CoasterControlsItem>
            COASTER_CONTROLS = ITEMS.register("coaster_controls",
                    () -> new dev.notzyvex.coasters_extras.control.CoasterControlsItem(
                            COASTER_CONTROLS_BLOCK.get(), new Item.Properties()));

    /** Registry for block-entity types, added for the Coaster Controls' animated handle. */
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>>
            BLOCK_ENTITIES = DeferredRegister.create(
                    net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,
                    CoastersExtras.MOD_ID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<
            net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<
                    dev.notzyvex.coasters_extras.control.CoasterControlsBlockEntity>>
            COASTER_CONTROLS_BE = BLOCK_ENTITIES.register("coaster_controls",
                    () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                            .of(dev.notzyvex.coasters_extras.control
                                        .CoasterControlsBlockEntity::new,
                                COASTER_CONTROLS_BLOCK.get())
                            .build(null));

    /** Powered Boost: a boost that only fires while its anchorpoint is receiving redstone. */
    public static final DeferredBlock<BoostTrackBlock> POWERED_BOOST_TRACK_MATERIAL =
            BLOCKS.register("powered_boost_track_material",
                    () -> new BoostTrackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_RED)
                                    .strength(0.8F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(),
                            ModTrackMaterials.POWERED_BOOST));

    public static final DeferredItem<BoostTrackItem> POWERED_BOOST_TRACK =
            ITEMS.register("powered_boost_track",
                    () -> new BoostTrackItem(new Item.Properties()));

    public static void register(IEventBus bus) {
        ModTrackMaterials.init();     // build the boost material before blocks resolve
        ModTrackVariants.register();  // 16 cosmetic variants
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    private ModTracks() {}
}
