package io.github.gaming32.worldhost.origincheck.checker;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.origincheck.OriginChecker;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

// This is more spotty, as not all web browsers implement this (for example, Firefox does, but Chromium doesn't).
// Furthermore, most things won't copy the metadata with the file, which means this metadata often won't be available
// from the mods directory, unless the file was directly saved there and not copied. When I wrote this, I only knew
// about the browser limitation, not the copying limitation, otherwise I wouldn't have written it.
public class GioOriginChecker implements OriginChecker {
    private static final String CHECKED_MARKER = "metadata::world-host-origin-checked";
    private static final String DOWNLOAD_URI = "metadata::download-uri";

    private final Gio gio;

    public GioOriginChecker() throws IllegalStateException {
        try {
            gio = Native.load("gio-2.0", Gio.class);
        } catch (UnsatisfiedLinkError e) {
            throw new IllegalStateException("Gio library not available", e);
        }
    }

    @Override
    public boolean needsChecking(Path file) {
        return doWithInfo(
            file, CHECKED_MARKER, false,
            (gioFile, info) -> gio.g_file_info_get_attribute_string(info, CHECKED_MARKER) == null
        );
    }

    @Override
    public List<URI> findOrigins(Path file) {
        final var uri = doWithInfo(
            file, DOWNLOAD_URI, null,
            (gioFile, info) -> gio.g_file_info_get_attribute_string(info, DOWNLOAD_URI)
        );
        if (uri == null) {
            return List.of();
        }
        try {
            return List.of(new URI(uri));
        } catch (URISyntaxException e) {
            WorldHost.LOGGER.warn("Failed to parse {} URI {}", DOWNLOAD_URI, uri, e);
            return List.of();
        }
    }

    @Override
    public void markChecked(Path file) {
        doWithInfo(file, "", (gioFile, info) -> {
            gio.g_file_info_set_attribute_string(info, CHECKED_MARKER, "");
            if (!gio.g_file_set_attributes_from_info(gioFile, info, 0, null, null)) {
                WorldHost.LOGGER.warn("Failed to set {} on {}", CHECKED_MARKER, file);
            }
        });
    }

    private void doWithInfo(Path file, String attributes, BiConsumer<Pointer, Pointer> action) {
        doWithInfo(file, attributes, null, (gioFile, info) -> {
            action.accept(gioFile, info);
            return null;
        });
    }

    private <T> T doWithInfo(Path file, String attributes, T defaultValue, BiFunction<Pointer, Pointer, T> action) {
        if (file.getFileSystem() != FileSystems.getDefault()) {
            return defaultValue;
        }
        final var gioFile = gio.g_file_new_for_path(file.toString());
        try {
            final var info = gio.g_file_query_info(gioFile, attributes, 0, null, null);
            if (info == null) {
                return defaultValue;
            }
            try {
                return action.apply(gioFile, info);
            } finally {
                Native.free(Pointer.nativeValue(info));
            }
        } finally {
            Native.free(Pointer.nativeValue(gioFile));
        }
    }

    private interface Gio extends Library {
        Pointer g_file_new_for_path(String path);

        Pointer g_file_query_info(Pointer file, String attributes, int flags, Pointer cancellable, Pointer error);

        String g_file_info_get_attribute_string(Pointer info, String attribute);

        void g_file_info_set_attribute_string(Pointer info, String attribute, String attr_value);

        boolean g_file_set_attributes_from_info(Pointer file, Pointer info, int flags, Pointer cancellable, Pointer error);
    }
}
