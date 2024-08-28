package io.github.gaming32.worldhost.compat.simplevoicechat;

import de.maxhenkel.voicechat.api.ClientVoicechatSocket;
import de.maxhenkel.voicechat.api.RawUdpPacket;
import de.maxhenkel.voicechat.plugins.impl.VoicechatSocketBase;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Objects;

public class WorldHostClientVoicechatSocket extends VoicechatSocketBase implements ClientVoicechatSocket {
    private final byte[] buffer = new byte[4096];

    private DatagramSocket socket;
    private SocketAddress targetAddress;

    public DatagramSocket getSocket() {
        return socket;
    }

    public void setTargetAddress(SocketAddress targetAddress) {
        this.targetAddress = targetAddress;
    }

    @Override
    public void open() throws Exception {
        socket = new DatagramSocket();
    }

    @Override
    public RawUdpPacket read() throws Exception {
        if (socket == null) {
            throw new IllegalStateException("Socket not opened yet");
        }
        return read(socket);
    }

    @Override
    public void send(byte[] data, SocketAddress address) throws Exception {
        sendDirect(data, Objects.requireNonNullElse(targetAddress, address));
    }

    public void sendDirect(byte[] data, SocketAddress address) throws IOException {
        if (socket == null) return;
        socket.send(new DatagramPacket(data, data.length, address));
    }

    @Override
    public void close() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    @Override
    public boolean isClosed() {
        return socket == null;
    }
}
