package io.github.gaming32.worldhost.common.ws;

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

    enum Proxy implements JoinType {
        INSTANCE;

        @Override
        public void encode(DataOutputStream dos) throws IOException {
            dos.writeByte(1);
        }
    }

    void encode(DataOutputStream dos) throws IOException;
}
