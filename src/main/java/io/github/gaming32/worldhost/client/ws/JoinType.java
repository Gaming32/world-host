package io.github.gaming32.worldhost.client.ws;

import java.io.DataOutputStream;
import java.io.IOException;

public sealed interface JoinType {
    record UPnP(int port) implements JoinType {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(0);
            dos.writeShort(port);
        }
    }

    record Proxy() implements JoinType {
        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(1);
        }
    }

    void encode(DataOutputStream dos) throws IOException;
}
