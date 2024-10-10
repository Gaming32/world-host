package io.github.gaming32.worldhost.origincheck.checker;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.origincheck.OriginChecker;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.ArrayList;
import java.util.List;

abstract class AbstractOriginChecker implements OriginChecker {
    abstract String getCheckedMarker();

    abstract String getOriginAttributeName();

    abstract List<@Nullable String> parseOrigins(ByteBuffer bb) throws Exception;

    @Override
    public boolean needsChecking(Path file) {
        final var attributes = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
        if (attributes == null) {
            return false;
        }
        try {
            return !attributes.list().contains(getCheckedMarker());
        } catch (IOException e) {
            WorldHost.LOGGER.error("Failed to check for {} on {}", getCheckedMarker(), file, e);
            return false;
        }
    }

    @Override
    public List<URI> findOrigins(Path file) {
        final var attributes = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
        if (attributes == null) {
            return List.of();
        }
        try {
            if (!attributes.list().contains(getOriginAttributeName())) {
                return List.of();
            }
            final var bb = ByteBuffer.allocate(attributes.size(getOriginAttributeName()));
            attributes.read(getOriginAttributeName(), bb);
            bb.flip();
            return toUriList(parseOrigins(bb));
        } catch (Exception e) {
            WorldHost.LOGGER.error("Failed to read {} on {}", getOriginAttributeName(), file, e);
            return List.of();
        }
    }

    private List<URI> toUriList(List<@Nullable String> uris) {
        final var result = new ArrayList<URI>(uris.size());
        for (final var url : uris) {
            if (url != null) {
                try {
                    result.add(new URI(url));
                } catch (URISyntaxException e) {
                    WorldHost.LOGGER.warn("Failed to parse {} URL {}", getOriginAttributeName(), url, e);
                }
            }
        }
        return result;
    }

    @Override
    public void markChecked(Path file) {
        final var attributes = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
        if (attributes == null) return;
        try {
            attributes.write(getCheckedMarker(), ByteBuffer.allocate(0));
        } catch (IOException e) {
            WorldHost.LOGGER.error("Failed to write {} on {}", getCheckedMarker(), file, e);
        }
    }
}
