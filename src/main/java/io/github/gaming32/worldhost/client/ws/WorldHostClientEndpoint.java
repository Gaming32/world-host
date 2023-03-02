package io.github.gaming32.worldhost.client.ws;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.client.DeferredToastManager;
import io.github.gaming32.worldhost.client.WorldHostClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

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
    @SuppressWarnings("unused")
    public void onMessage(Session session, WorldHostS2CMessage message) {
        WorldHost.LOGGER.info("Received WS message {}", message);
        message.handle(session);
    }

    @OnOpen
    @SuppressWarnings("unused")
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(0);
    }

    @OnError
    @SuppressWarnings("unused")
    public void onError(Session session, Throwable t) throws IOException {
        if (session == null) return;
        WorldHost.LOGGER.error("Error in WS client", t);
        session.close();
        WorldHostClient.wsClient = null;
        DeferredToastManager.show(
            SystemToast.Type.TUTORIAL_HINT,
            Text.translatable("world-host.error_in_connection"),
            Text.of(Util.getInnermostMessage(t))
        );
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
