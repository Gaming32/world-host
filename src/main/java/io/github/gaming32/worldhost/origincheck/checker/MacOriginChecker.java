package io.github.gaming32.worldhost.origincheck.checker;

import io.github.gaming32.worldhost.origincheck.parser.SimpleBplistParser;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

public class MacOriginChecker extends AbstractOriginChecker {
    @Override
    String getCheckedMarker() {
        return "io.github.gaming32.worldhost:originChecked";
    }

    @Override
    String getOriginAttributeName() {
        return "com.apple.metadata:kMDItemWhereFroms";
    }

    @Override
    List<String> parseOrigins(ByteBuffer bb) {
        final var bplist = SimpleBplistParser.parseBplist(bb);
        if (!(bplist instanceof Collection<?> collection)) {
            return List.of();
        }
        return collection.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .toList();
    }
}
