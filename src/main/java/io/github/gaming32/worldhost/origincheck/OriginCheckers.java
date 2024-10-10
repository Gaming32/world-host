package io.github.gaming32.worldhost.origincheck;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.origincheck.checker.GioOriginChecker;
import io.github.gaming32.worldhost.origincheck.checker.MacOriginChecker;
import io.github.gaming32.worldhost.origincheck.checker.NoopOriginChecker;
import io.github.gaming32.worldhost.origincheck.checker.WindowsOriginChecker;
import net.minecraft.Util;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public class OriginCheckers {
    public static final OriginChecker NATIVE_CHECKER = switch (Util.getPlatform()) {
        case WINDOWS -> new WindowsOriginChecker();
        case OSX -> new MacOriginChecker();
        default -> {
            try {
                yield new GioOriginChecker();
            } catch (IllegalStateException e) {
                WorldHost.LOGGER.info("GIO origin checker not available", e);
                yield new NoopOriginChecker();
            }
        }
    };
    public static final List<String> STANDARD_ORIGINS = List.of(
        "modrinth.com",
        "github.com",
        "githubusercontent.com",
        "maven.jemnetworks.com"
    );

    public static boolean isStandardHost(String host) {
        final var hostLength = host.length();
        for (final var origin : STANDARD_ORIGINS) {
            if (host.equals(origin)) {
                return true;
            }
            final var originLength = origin.length();
            if (hostLength <= originLength) continue;
            if (host.endsWith(origin) && host.charAt(hostLength - originLength - 1) == '.') {
                return true;
            }
        }
        return false;
    }

    public static boolean hasStandardOrigin(URI uri) {
        return isStandardHost(uri.getHost());
    }

    public static List<URI> getNonstandardOrigins(List<URI> uris) {
        return uris.stream().filter(Predicate.not(OriginCheckers::hasStandardOrigin)).toList();
    }

    public static List<URI> getNonstandardOrigins(OriginChecker checker, Path file) {
        return getNonstandardOrigins(checker.findOrigins(file));
    }

    public static List<URI> getNonstandardOriginsOnce(OriginChecker checker, Path file) {
        if (!checker.needsChecking(file)) {
            return List.of();
        }
        final var result = getNonstandardOrigins(checker, file);
        checker.markChecked(file);
        return result;
    }
}
