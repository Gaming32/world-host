package io.github.gaming32.worldhost.protocol;

import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.network.protocol.status.ServerStatus;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.UUID;

// Mirrors https://github.com/Gaming32/world-host-server-kotlin/blob/main/src/main/kotlin/io/github/gaming32/worldhostserver/WorldHostC2SMessage.kt
public sealed interface WorldHostC2SMessage {
    record ListOnline(Collection<UUID> friends) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 0;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeInt(friends.size());
            for (final UUID friend : friends) {
                writeUuid(dos, friend);
            }
        }
    }

    record FriendRequest(UUID toUser) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 1;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            writeUuid(dos, toUser);
        }
    }

    record PublishedWorld(Collection<UUID> friends) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 2;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeInt(friends.size());
            for (final UUID friend : friends) {
                writeUuid(dos, friend);
            }
        }
    }

    record ClosedWorld(Collection<UUID> friends) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 3;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeInt(friends.size());
            for (final UUID friend : friends) {
                writeUuid(dos, friend);
            }
        }
    }

    @Deprecated
    record RequestJoin(UUID friend) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 4;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            writeUuid(dos, friend);
        }
    }

    record JoinGranted(long connectionId, JoinType joinType) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 5;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeLong(connectionId);
            joinType.encode(dos);
        }
    }

    record QueryRequest(Collection<UUID> friends) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 6;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeInt(friends.size());
            for (final UUID friend : friends) {
                writeUuid(dos, friend);
            }
        }
    }

    @Deprecated
    record QueryResponse(long connectionId, ServerStatus metadata) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 7;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeLong(connectionId);
            final var buf = WorldHost.writeServerStatus(metadata);
            dos.writeInt(buf.readableBytes());
            buf.readBytes(dos, buf.readableBytes());
        }
    }

    record ProxyS2CPacket(long connectionId, byte[] data) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 8;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeLong(connectionId);
            dos.write(data);
        }
    }

    record ProxyDisconnect(long connectionId) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 9;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeLong(connectionId);
        }
    }

    record RequestDirectJoin(long connectionId) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 10;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeLong(connectionId);
        }
    }

    record NewQueryResponse(long connectionId, ServerStatus metadata) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 11;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeLong(connectionId);
            final var buf = WorldHost.writeServerStatus(metadata);
            buf.readBytes(dos, buf.readableBytes());
        }
    }

    record RequestPunchOpen(
        long targetConnection, String purpose, UUID punchId, String myHost, int myPort
    ) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 12;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeLong(targetConnection);
            writeString(dos, purpose);
            writeUuid(dos, punchId);
            writeString(dos, myHost);
            dos.writeShort(myPort);
        }
    }

    record PunchFailed(long targetConnection, UUID punchId) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 13;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            writeUuid(dos, punchId);
        }
    }

    record BeginPortLookup(UUID lookupId) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 14;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            writeUuid(dos, lookupId);
        }
    }

    record PunchSuccess(long connectionId, UUID punchId, String host, int port) implements WorldHostC2SMessage {
        @Override
        public byte typeId() {
            return 15;
        }

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeLong(connectionId);
            writeUuid(dos, punchId);
            writeString(dos, host);
            dos.writeShort(port);
        }
    }

    byte typeId();

    void encode(DataOutputStream dos) throws IOException;

    static void writeUuid(DataOutputStream dos, UUID uuid) throws IOException {
        dos.writeLong(uuid.getMostSignificantBits());
        dos.writeLong(uuid.getLeastSignificantBits());
    }

    static void writeString(DataOutputStream dos, String string) throws IOException {
        final byte[] buf = string.getBytes(StandardCharsets.UTF_8);
        dos.writeShort(buf.length);
        dos.write(buf);
    }
}
