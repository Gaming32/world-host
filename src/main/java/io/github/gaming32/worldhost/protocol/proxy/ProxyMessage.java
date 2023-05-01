package io.github.gaming32.worldhost.protocol.proxy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;

// Mirrors https://github.com/Gaming32/world-host-external-proxy/blob/main/src/main/kotlin/io/github/gaming32/worldhostexternalproxy/Message.kt
public abstract sealed class ProxyMessage {
    public static final class Open extends ProxyMessage {
        private final InetAddress address;

        public Open(long connectionId, InetAddress address) {
            super(connectionId, (byte)0);
            this.address = address;
        }

        public InetAddress getAddress() {
            return address;
        }

        @Override
        public void write(DataOutputStream out) {
            throw new IllegalStateException("Cannot write Open message on client");
        }
    }

    public static final class Packet extends ProxyMessage {
        private final byte[] buffer;

        public Packet(long connectionId, byte[] buffer) {
            super(connectionId, (byte)1);
            if (buffer.length > 0xffff) {
                throw new IllegalArgumentException("Packet exceeds max packet size");
            }
            this.buffer = buffer;
        }

        @Override
        public void write(DataOutputStream out) throws IOException {
            out.writeShort(buffer.length);
            out.write(buffer);
        }

        public byte[] getBuffer() {
            return buffer;
        }
    }

    public static final class Close extends ProxyMessage {
        public Close(long connectionId) {
            super(connectionId, (byte)2);
        }
    }

    private final long connectionId;
    private final byte type;

    protected ProxyMessage(long connectionId, byte type) {
        this.connectionId = connectionId;
        this.type = type;
    }

    public final long getConnectionId() {
        return connectionId;
    }

    public final byte getType() {
        return type;
    }

    public void write(DataOutputStream out) throws IOException {
    }

    public static ProxyMessage read(DataInputStream in) throws IOException {
        final long connectionId = in.readLong();
        final byte packetId = in.readByte();
        return switch (packetId) {
            case 0 -> new Open(connectionId, InetAddress.getByAddress(in.readNBytes(in.readUnsignedByte())));
            case 1 -> new Packet(connectionId, in.readNBytes(in.readUnsignedShort()));
            case 2 -> new Close(connectionId);
            default -> throw new IllegalArgumentException("Unknown packet ID: " + packetId);
        };
    }
}
