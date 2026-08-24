package dev.notzyvex.coasters_extras.camera;

import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCamera {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CoastersExtras.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CoastersExtras.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CameraCartEntity>> CAMERA_CART =
            ENTITIES.register("camera_cart", () -> EntityType.Builder
                    .<CameraCartEntity>of(CameraCartEntity::new, MobCategory.MISC)
                    .sized(0.6f, 0.6f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("camera_cart"));

    public static final DeferredItem<Item> CAMERA_CART_ITEM =
            ITEMS.register("camera_cart", () -> new CameraCartItem(new Item.Properties()));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
    }

    private ModCamera() {}
}
