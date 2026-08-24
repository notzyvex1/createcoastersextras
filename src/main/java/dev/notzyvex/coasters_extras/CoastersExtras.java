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
        // Attaching a display source to a block has to wait until registries are frozen, so it
        // belongs here rather than beside the DeferredRegister that created the sources.
        event.enqueueWork(dev.notzyvex.coasters_extras.display.ModDisplaySources::bind);
    }

    /**
     * Teach the base mod's balloon block entity type that our balloons are valid hosts.
     *
     * <p>Our blocks extend {@code RedBalloonBlock} so they already create a
     * {@code BalloonBlockEntity}, but {@code BlockEntityType} keeps a fixed set of allowed
     * blocks and theirs was built listing only {@code red_balloon}. Without this, the
     * block entity is rejected on load and the balloon never tethers.
     */
    private void registerWithBalloonBlockEntity() {
        try {
            BlockEntityType<?> type = SimulatedCoastersBlocks.BALLOON_BE.get();
            BlockEntityTypeAccessor accessor = (BlockEntityTypeAccessor) type;

            // The existing set may be immutable, so copy before adding.
            Set<Block> valid = new HashSet<>(accessor.coasters_extras$getValidBlocks());
            for (var holder : ModBlocks.BALLOONS.values()) {
                valid.add(holder.get());
            }
            accessor.coasters_extras$setValidBlocks(valid);

            LOGGER.info("Registered {} balloons with the Simulated Coasters balloon block entity.",
                    ModBlocks.BALLOONS.size());
        } catch (Throwable t) {
            // Non-fatal: balloons still place and render, they just will not tether.
            // Far better than hard-crashing the game if the base mod changes shape.
            LOGGER.error("Could not hook into Simulated Coasters' balloon block entity. "
                       + "Balloons will render but not behave as balloons. "
                       + "Has the base mod changed?", t);
        }
    }
}
