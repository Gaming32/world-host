package io.github.gaming32.worldhost.client.ws;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.client.DeferredToastManager;
import io.github.gaming32.worldhost.client.WorldHostClient;
import net.minecraft.Util;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import javax.websocket.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

@ClientEndpoint(
    encoders = {WorldHostC2SMessage.Encoder.class, WorldHostClientEndpoint.UuidEncoder.class},
    decoders = WorldHostS2CMessage.Decoder.class
)
public class WorldHostClientEndpoint {
    @OnMessage
    public void onMessage(Session session, WorldHostS2CMessage message) {
        WorldHost.LOGGER.info("Received WS message {}", message);
        message.handle(session);
    }

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(0);
    }

    @OnError
    public void onError(Session session, Throwable t) throws IOException {
        WorldHost.LOGGER.error("Error in WS client", t);
        if (session == null) return;
        session.close();
        DeferredToastManager.show(
            SystemToast.SystemToastIds.TUTORIAL_HINT,
            Component.translatable("world-host.error_in_connection"),
            Component.nullToEmpty(Util.describeError(t))
        );
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        WorldHostClient.wsClient = null;
        WorldHost.LOGGER.info("WS connection terminated for {}", closeReason);
    }

    public static class UuidEncoder implements Encoder.BinaryStream<UUID> {
        @Override
        public void encode(UUID object, OutputStream os) throws IOException {
            final DataOutputStream dos = new DataOutputStream(os);
            dos.writeLong(object.getMostSignificantBits());
            dos.writeLong(object.getLeastSignificantBits());
        }

        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public void destroy() {
        }
    }
}
