package dev.notzyvex.coasters_extras.net;

import dev.notzyvex.coasters_extras.CoastersExtras;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = CoastersExtras.MOD_ID)
public final class ServerEvents {

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BrakingTracker.flush(level);
            StationTracker.flush(level);
        }
    }

    private ServerEvents() {}
}
