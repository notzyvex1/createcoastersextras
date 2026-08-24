package dev.notzyvex.coasters_extras;

import dev.notzyvex.coasters_extras.mixin.BlockEntityTypeAccessor;
import dev.notzyvex.coasters_extras.track.ModTracks;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

@Mod(CoastersExtras.MOD_ID)
public class CoastersExtras {

    public static final String MOD_ID = "coasters_extras";
    public static final Logger LOGGER = LoggerFactory.getLogger("Create: Coasters Extras");

    public CoastersExtras(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        ModTracks.register(modBus);
        dev.notzyvex.coasters_extras.camera.ModCamera.register(modBus);
        dev.notzyvex.coasters_extras.cart.ModCartParts.register(modBus);
        dev.notzyvex.coasters_extras.sensor.SensorRegistry.register(modBus);
        dev.notzyvex.coasters_extras.display.ModDisplaySources.register(modBus);
        dev.notzyvex.coasters_extras.particle.ModParticles.register(modBus);
        ModCreativeTabs.register(modBus);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, Config.SPEC);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(this::registerWithBalloonBlockEntity);
        event.enqueueWork(dev.notzyvex.coasters_extras.display.ModDisplaySources::bind);
    }

    private void registerWithBalloonBlockEntity() {
        try {
            BlockEntityType<?> type = SimulatedCoastersBlocks.BALLOON_BE.get();
            BlockEntityTypeAccessor accessor = (BlockEntityTypeAccessor) type;

            Set<Block> valid = new HashSet<>(accessor.coasters_extras$getValidBlocks());
            for (var holder : ModBlocks.BALLOONS.values()) {
                valid.add(holder.get());
            }
            accessor.coasters_extras$setValidBlocks(valid);

            LOGGER.info("Registered {} balloons with the Simulated Coasters balloon block entity.",
                    ModBlocks.BALLOONS.size());
        } catch (Throwable t) {
            LOGGER.error("Could not hook into Simulated Coasters' balloon block entity. "
                       + "Balloons will render but not behave as balloons. "
                       + "Has the base mod changed?", t);
        }
    }
}
