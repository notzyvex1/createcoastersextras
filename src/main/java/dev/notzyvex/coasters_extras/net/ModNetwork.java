package dev.notzyvex.coasters_extras.net;

import dev.notzyvex.coasters_extras.CoastersExtras;
import dev.notzyvex.coasters_extras.client.BrakingCarts;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CoastersExtras.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(BrakingPayload.TYPE, BrakingPayload.CODEC,
                (payload, context) -> context.enqueueWork(
                        // handled on the client thread; the renderer reads this next frame
                        () -> BrakingCarts.accept(payload.positions())));
        r.playToClient(StationPayload.TYPE, StationPayload.CODEC,
                (payload, context) -> context.enqueueWork(
                        // read by the goggle tooltip and the platform light
                        () -> dev.notzyvex.coasters_extras.client.StationStates
                                .accept(payload.entries())));
        // Client -> server: repaint an existing curve. The client knows which curve is under
        // the crosshair; only the server can make the change stick.
        r.playToServer(RepaintPayload.TYPE, RepaintPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player().level() instanceof net.minecraft.server.level.ServerLevel level)) {
                        return;
                    }
                    // Never trust a client-named position. Both ends must be close enough to
                    // the player to be something they could actually be pointing at.
                    var eye = context.player().blockPosition();
                    if (eye.distSqr(payload.anchor()) > 4096 || eye.distSqr(payload.peer()) > 4096) {
                        return;
                    }
                    var material = com.simibubi.create.content.trains.track.TrackMaterial
                            .ALL.get(payload.material());
                    if (material == null) {
                        return;
                    }
                    dev.notzyvex.coasters_extras.track.TrackRepaint.repaint(
                            level, payload.anchor(), payload.peer(), material);
                }));
    }

    private ModNetwork() {}
}
