package io.github.gaming32.worldhost.plugin;

import io.github.gaming32.worldhost.toast.IconRenderer;

public interface ProfileInfo {
    String name();

    IconRenderer iconRenderer();

    record Basic(String name, IconRenderer iconRenderer) implements ProfileInfo {
    }
}
