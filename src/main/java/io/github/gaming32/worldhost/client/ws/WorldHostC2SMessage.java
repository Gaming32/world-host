package io.github.gaming32.worldhost.client.ws;

import javax.websocket.EndpointConfig;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.UUID;

// Mirrors https://github.com/Gaming32/world-host-server/blob/main/src/c2s_message.rs
public sealed interface WorldHostC2SMessage {
    record ListOnline(Collection<UUID> friends) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(0);
            dos.writeInt(friends.size());
            for (final UUID friend : friends) {
                writeUuid(dos, friend);
            }
        }
    }

    record IsOnlineTo(UUID connectionId) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(1);
            writeUuid(dos, connectionId);
        }
    }

    record FriendRequest(UUID toUser) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(2);
            writeUuid(dos, toUser);
        }
    }

    record WentInGame(Collection<UUID> friends) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(3);
            dos.writeInt(friends.size());
            for (final UUID friend : friends) {
                writeUuid(dos, friend);
            }
        }
    }

    void encode(DataOutputStream dos) throws IOException;

    static void writeUuid(DataOutputStream dos, UUID uuid) throws IOException {
        dos.writeLong(uuid.getMostSignificantBits());
        dos.writeLong(uuid.getLeastSignificantBits());
    }

    class Encoder implements javax.websocket.Encoder.BinaryStream<WorldHostC2SMessage> {
        @Override
        public void encode(WorldHostC2SMessage object, OutputStream os) throws IOException {
            object.encode(new DataOutputStream(os));
        }

        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public void destroy() {
        }
    }
}
