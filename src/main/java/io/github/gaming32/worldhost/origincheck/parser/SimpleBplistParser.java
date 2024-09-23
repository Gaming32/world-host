package io.github.gaming32.worldhost.origincheck.parser;

import it.unimi.dsi.fastutil.bytes.Byte2LongFunction;
import net.minecraft.Util;

import java.io.Serial;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.IntFunction;

public class SimpleBplistParser {
    private static final long CORE_DATA_EPOCH = Util.make(() -> {
        final var calender = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        calender.set(2001, Calendar.JANUARY, 1, 0, 0, 0);
        return calender.getTimeInMillis() / 1000L;
    });

    private static final int TYPE_SIMPLE = 0b0000;
    private static final int TYPE_INT = 0b0001;
    private static final int TYPE_REAL = 0b0010;
    private static final int TYPE_DATE = 0b0011;
    private static final int TYPE_DATA = 0b0100;
    private static final int TYPE_ASCII = 0b0101;
    private static final int TYPE_UNICODE = 0b0110;
    private static final int TYPE_UTF8 = 0b0111;
    private static final int TYPE_UID = 0b1000;
    private static final int TYPE_ARRAY = 0b1010;
    private static final int TYPE_ORDSET = 0b1011;
    private static final int TYPE_SET = 0b1100;
    private static final int TYPE_DICT = 0b1101;

    private static final int SIMPLE_NULL = 0b0000;
    private static final int SIMPLE_FALSE = 0b1000;
    private static final int SIMPLE_TRUE = 0b1001;
    private static final int SIMPLE_URL = 0b1100;
    private static final int SIMPLE_URL_WITH_BASE = 0b1101;
    private static final int SIMPLE_UUID = 0b1110;
    private static final int SIMPLE_FILL = 0b1111;

    private static final byte[] MAGIC = "bplist".getBytes(StandardCharsets.US_ASCII);

    private final ByteBuffer data;
    private final int[] offsets;
    private final int refSize;
    private final int rootObject;
    private final Map<Charset, CharsetDecoder> decoderCache = HashMap.newHashMap(3);

    private SimpleBplistParser(ByteBuffer data) {
        this.data = data;
        data.position(data.limit() - 32 + 6);
        final var offsetSize = data.get() & 0xff;
        refSize = data.get() & 0xff;
        offsets = new int[(int)data.getLong()];
        rootObject = (int)data.getLong();
        final var offsetTableStart = (int)data.getLong();
        readOffsetTable(offsetTableStart, offsetSize);
    }

    public static Object parseBplist(ByteBuffer data) {
        data.order(ByteOrder.BIG_ENDIAN);
        data.position(0);
        final var magic = new byte[MAGIC.length];
        data.get(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new IllegalArgumentException("Not a bplist file");
        }
        data.get(); // Major
        data.get(); // Minor
        return new SimpleBplistParser(data).readRoot();
    }

    private Object readRoot() {
        return readObjectByIndex(rootObject);
    }

    private Object readObjectByIndex(int index) {
        data.position(offsets[index]);
        return readObject();
    }

    private Object readObject() {
        final var marker = data.get() & 0xff;
        final var type = (marker & 0xf0) >> 4;
        final var typeExtra = marker & 0x0f;
        return switch (type) {
            case TYPE_SIMPLE -> switch (typeExtra) {
                case SIMPLE_NULL -> null;
                case SIMPLE_FALSE -> false;
                case SIMPLE_TRUE -> true;
                case SIMPLE_URL -> {
                    final var urlObj = readObject();
                    if (!(urlObj instanceof String url)) {
                        throw new BplistParsingFailure("Expected url to be string");
                    }
                    try {
                        yield new URI(url).toURL();
                    } catch (URISyntaxException | MalformedURLException e) {
                        throw new BplistParsingFailure("Invalid URL " + url, e);
                    }
                }
                case SIMPLE_URL_WITH_BASE -> throw new BplistParsingFailure("Extended url not supported");
                case SIMPLE_UUID -> new UUID(data.getLong(), data.getLong());
                case SIMPLE_FILL -> throw new BplistParsingFailure("Should never parse fill byte");
                default -> throw new BplistParsingFailure("Unknown marker byte 0x" + Integer.toHexString(marker));
            };
            case TYPE_INT -> readIntObject(typeExtra);
            case TYPE_REAL -> readReal(typeExtra);
            case TYPE_DATE -> new Date((data.getLong() - CORE_DATA_EPOCH) * 1000L);
            case TYPE_DATA -> {
                final var result = new byte[readSize(typeExtra)];
                data.get(result);
                yield result;
            }
            case TYPE_ASCII -> readString(StandardCharsets.US_ASCII, typeExtra);
            case TYPE_UNICODE -> readString(StandardCharsets.UTF_16BE, typeExtra);
            case TYPE_UTF8 -> readString(StandardCharsets.UTF_8, typeExtra);
            case TYPE_UID -> throw new BplistParsingFailure("Unsupported object type uid");
            case TYPE_ARRAY -> readCollection(ArrayList::new, typeExtra);
            case TYPE_ORDSET -> readCollection(LinkedHashSet::newLinkedHashSet, typeExtra);
            case TYPE_SET -> readCollection(HashSet::newHashSet, typeExtra);
            case TYPE_DICT -> readDict(typeExtra);
            default -> throw new BplistParsingFailure("Unknown marker byte 0x" + Integer.toHexString(marker));
        };
    }

    private long readIntObject(int extra) {
        return readIntSigned(1 << extra);
    }

    private Number readReal(int extra) {
        final var bits = readIntObject(extra);
        return switch (extra) {
            case 2 -> Float.intBitsToFloat((int)bits);
            case 3 -> Double.longBitsToDouble(bits);
            default -> throw new BplistParsingFailure("Unsupported real size 2^" + extra);
        };
    }

    private String readString(Charset charset, int extra) {
        final var decoder = decoderCache.computeIfAbsent(
            charset, c -> c.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
        ).reset();
        final var result = CharBuffer.allocate(readSize(extra));
        decoder.decode(data, result, true);
        if (result.remaining() > 0) {
            throw new BplistParsingFailure("Failed to read entire string (" + result.remaining() + " unread)");
        }
        return result.flip().toString();
    }

    private <C extends Collection<Object>> C readCollection(IntFunction<C> factory, int extra) {
        final var size = readSize(extra);
        final var result = factory.apply(size);
        var pos = data.position();
        for (var i = 0; i < size; i++) {
            result.add(readObjectByIndex((int)readIntUnsigned(refSize)));
            data.position(pos += refSize);
        }
        return result;
    }

    private Map<?, ?> readDict(int extra) {
        final var size = readSize(extra);
        final var keys = new Object[size];
        final var values = new Object[size];
        var pos = data.position();
        for (var i = 0; i < size; i++) {
            keys[i] = readObjectByIndex((int)readIntUnsigned(refSize));
            data.position(pos += refSize);
        }
        for (var i = 0; i < size; i++) {
            values[i] = readObjectByIndex((int)readIntUnsigned(refSize));
            data.position(pos += refSize);
        }

        final var result = HashMap.newHashMap(size);
        for (var i = 0; i < size; i++) {
            result.put(keys[i], values[i]);
        }
        return result;
    }

    private int readSize(int extra) {
        if (extra != 0xf) {
            return extra;
        }
        return (int)readIntObject(data.get() & 0x0f);
    }

    private void readOffsetTable(int offsetTableStart, int offsetSize) {
        data.position(offsetTableStart);
        for (var i = 0; i < offsets.length; i++) {
            offsets[i] = (int)readIntUnsigned(offsetSize);
        }
    }

    private long readIntUnsigned(int size) {
        return readInt(size, x -> x & 0xffL);
    }

    private long readIntSigned(int size) {
        return readInt(size, x -> (long)x);
    }

    private long readInt(int size, Byte2LongFunction starter) {
        if (size < 1) {
            throw new IllegalArgumentException("Size < 1");
        }
        var result = starter.get(data.get());
        for (var i = 1; i < size; i++) {
            result = (result << 8) | (data.get() & 0xff);
        }
        return result;
    }

    public static class BplistParsingFailure extends RuntimeException {
        @Serial
        private static final long serialVersionUID = -5280726282273772182L;

        public BplistParsingFailure(String message) {
            super(message);
        }

        public BplistParsingFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
