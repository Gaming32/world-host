package io.github.gaming32.worldhost.origincheck.checker;

import io.github.gaming32.worldhost.origincheck.parser.SimpleIniParser;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
        try (var reader = new BufferedReader(
            new InputStreamReader(
                new ByteArrayInputStream(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining()),
                Charset.forName(System.getProperty("native.encoding"))
            )
        )) {
            return SimpleIniParser.parse(reader);
        }
    }
}
