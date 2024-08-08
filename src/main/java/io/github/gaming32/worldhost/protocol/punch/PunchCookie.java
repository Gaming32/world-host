package io.github.gaming32.worldhost.protocol.punch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;

public final class PunchCookie {
    public static final int BITS = 128;
    public static final int BYTES = BITS / 8;

    private final byte[] cookie;
    private int hashCode;

    public PunchCookie(byte[] cookie) {
        if (cookie.length != BYTES) {
            throw new IllegalArgumentException("PunchCookie data length must be " + BITS + " bits");
        }
        this.cookie = cookie;
    }

    public static PunchCookie random() {
        final byte[] cookie = new byte[BYTES];
        new SecureRandom().nextBytes(cookie);
        return new PunchCookie(cookie);
    }

    public static PunchCookie readFrom(InputStream is) throws IOException {
        return new PunchCookie(is.readNBytes(BYTES));
    }

    public byte[] toBytes() {
        return cookie;
    }

    public void writeTo(OutputStream os) throws IOException {
        os.write(cookie);
    }

    @Override
    public String toString() {
        return HexFormat.of().formatHex(cookie);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PunchCookie other && Arrays.equals(cookie, other.cookie);
    }

    @Override
    public int hashCode() {
        if (hashCode != 0) {
            return hashCode;
        }
        int h = Arrays.hashCode(cookie);
        if (h == 0) {
            h = 31;
        }
        return hashCode = h;
    }
}
