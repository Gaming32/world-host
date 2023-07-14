package io.github.gaming32.worldhost.protocol;

import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

// Mirrors https://github.com/Gaming32/world-host-server-kotlin/blob/main/src/main/kotlin/io/github/gaming32/worldhostserver/WorldHostC2SMessage.kt
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

    record FriendRequest(UUID toUser) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(1);
            writeUuid(dos, toUser);
        }
    }

    record PublishedWorld(Collection<UUID> friends) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(2);
            dos.writeInt(friends.size());
            for (final UUID friend : friends) {
                writeUuid(dos, friend);
            }
        }
    }

    record ClosedWorld(Collection<UUID> friends) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(3);
            dos.writeInt(friends.size());
            for (final UUID friend : friends) {
                writeUuid(dos, friend);
            }
        }
    }

    @Deprecated
    record RequestJoin(UUID friend) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(4);
            writeUuid(dos, friend);
        }
    }

    record JoinGranted(long connectionId, JoinType joinType) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(5);
            dos.writeLong(connectionId);
            joinType.encode(dos);
        }
    }

    record QueryRequest(Collection<UUID> friends) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(6);
            dos.writeInt(friends.size());
            for (final UUID friend : friends) {
                writeUuid(dos, friend);
            }
        }
    }

    @Deprecated
    record QueryResponse(long connectionId, ServerStatus metadata) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(7);
            dos.writeLong(connectionId);
            final FriendlyByteBuf buf = WorldHost.createByteBuf();
            new ClientboundStatusResponsePacket(metadata != null ? metadata : WorldHost.createEmptyServerStatus())
                .write(buf);
            dos.writeInt(buf.readableBytes());
            buf.readBytes(dos, buf.readableBytes());
        }
    }

    record ProxyS2CPacket(long connectionId, byte[] data) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(8);
            dos.writeLong(connectionId);
            dos.write(data);
        }
    }

    record ProxyDisconnect(long connectionId) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(9);
            dos.writeLong(connectionId);
        }
    }

    record RequestDirectJoin(long connectionId) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(10);
            dos.writeLong(connectionId);
        }
    }

    record NewQueryResponse(long connectionId, ServerStatus metadata) implements WorldHostC2SMessage {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(11);
            dos.writeLong(connectionId);
            final FriendlyByteBuf buf = WorldHost.createByteBuf();
            new ClientboundStatusResponsePacket(metadata != null ? metadata : WorldHost.createEmptyServerStatus())
                .write(buf);
            buf.readBytes(dos, buf.readableBytes());
        }
    }

    void encode(DataOutputStream dos) throws IOException;

    static void writeUuid(DataOutputStream dos, UUID uuid) throws IOException {
        dos.writeLong(uuid.getMostSignificantBits());
        dos.writeLong(uuid.getLeastSignificantBits());
    }
}
