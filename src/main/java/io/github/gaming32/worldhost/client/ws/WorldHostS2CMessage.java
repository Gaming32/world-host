package io.github.gaming32.worldhost.client.ws;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostData;

import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

// Mirrors https://github.com/Gaming32/world-host-server/blob/main/src/s2c_message.rs
public sealed interface WorldHostS2CMessage {
    record Error(String message) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            WorldHost.LOGGER.error("Received protocol error: {}", message);
        }
    }

    record IsOnlineTo(UUID user, UUID connectionId) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            if (WorldHostData.friends.contains(user)) {
                session.getAsyncRemote().sendObject(new WorldHostC2SMessage.IsOnlineTo(connectionId));
            }
        }
    }

    record OnlineGame(String ip) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            // TODO: Implement
        }
    }

    record FriendRequest(UUID fromUser) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            // TODO: Implement
        }
    }

    record WentInGame(UUID user) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            // TODO: Implement
        }
    }

    void handle(Session session);

    static WorldHostS2CMessage decode(DataInputStream dis) throws IOException {
        final int typeId = dis.readUnsignedByte();
        return switch (typeId) {
            case 0 -> new Error(readString(dis));
            case 1 -> new IsOnlineTo(readUuid(dis), readUuid(dis));
            case 2 -> new OnlineGame(readString(dis));
            case 3 -> new FriendRequest(readUuid(dis));
            case 4 -> new WentInGame(readUuid(dis));
            default -> new Error("Received packet with unknown type_id from server: " + typeId);
        };
    }

    static UUID readUuid(DataInputStream dis) throws IOException {
        return new UUID(dis.readLong(), dis.readLong());
    }

    static String readString(DataInputStream dis) throws IOException {
        final byte[] buf = new byte[dis.readUnsignedShort()];
        dis.readFully(buf);
        return new String(buf, StandardCharsets.UTF_8);
    }

    class Decoder implements javax.websocket.Decoder.BinaryStream<WorldHostS2CMessage> {
        @Override
        public WorldHostS2CMessage decode(InputStream is) throws IOException {
            return WorldHostS2CMessage.decode(new DataInputStream(is));
        }

        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public void destroy() {
        }
    }
}
