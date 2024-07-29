package io.github.gaming32.worldhost.protocol.punch;

import java.io.IOException;
import java.net.InetSocketAddress;

@FunctionalInterface
public interface PunchTransmitter {
    void transmit(byte[] packet, InetSocketAddress address) throws IOException;
}
