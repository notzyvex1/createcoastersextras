package dev.notzyvex.coasters_extras.net;

import dev.notzyvex.coasters_extras.CoastersExtras;
import dev.notzyvex.coasters_extras.client.BrakingCarts;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CoastersExtras.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(BrakingPayload.TYPE, BrakingPayload.CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> BrakingCarts.accept(payload.positions())));
        r.playToClient(StationPayload.TYPE, StationPayload.CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> dev.notzyvex.coasters_extras.client.StationStates
                                .accept(payload.entries())));
        r.playToServer(RepaintPayload.TYPE, RepaintPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player().level() instanceof net.minecraft.server.level.ServerLevel level)) {
                        return;
                    }
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
