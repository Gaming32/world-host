package io.github.gaming32.worldhost.client.ws;

import io.github.gaming32.worldhost.WorldHost;

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
