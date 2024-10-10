package io.github.gaming32.worldhost.origincheck.checker;

import io.github.gaming32.worldhost.origincheck.parser.SimpleIniParser;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WindowsOriginChecker extends AbstractOriginChecker {
    @Override
    String getCheckedMarker() {
        return "WorldHostOriginChecked";
    }

    @Override
    String getOriginAttributeName() {
        return "Zone.Identifier";
    }

    @Override
    List<@Nullable String> parseOrigins(ByteBuffer bb) throws IOException {
        final var zoneIdentifier = parseZoneIdentifier(bb);
        final var zoneTransfer = zoneIdentifier.get("ZoneTransfer");
        if (zoneTransfer == null) {
            return List.of();
        }
        return Arrays.asList(zoneTransfer.get("ReferrerUrl"), zoneTransfer.get("HostUrl"));
    }

    private static Map<String, Map<String, String>> parseZoneIdentifier(ByteBuffer bb) throws IOException {
        final var cb = getWindowsCharset().decode(bb);
        try (var reader = new BufferedReader(new CharArrayReader(cb.array(), cb.arrayOffset() + cb.position(), cb.remaining()))) {
            return SimpleIniParser.parse(reader);
        }
    }

    // TODO: Replace with direct native.encoding usage when Java 18+ becomes the minimum
    private static Charset getWindowsCharset() {
        final var nativeCharset = System.getProperty("native.encoding");
        return nativeCharset == null ? Charset.defaultCharset() : Charset.forName(nativeCharset);
    }
}
