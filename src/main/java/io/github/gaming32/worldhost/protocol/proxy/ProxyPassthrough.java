package io.github.gaming32.worldhost.protocol.proxy;

public interface ProxyPassthrough {
    void proxyS2CPacket(long connectionId, byte[] data);

    void proxyDisconnect(long connectionId);
}
