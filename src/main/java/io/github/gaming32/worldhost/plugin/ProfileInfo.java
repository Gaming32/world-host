package io.github.gaming32.worldhost.plugin;

import io.github.gaming32.worldhost.toast.IconRenderer;

/**
 * Represents basic information about a user profile.
 */
public interface ProfileInfo {
    /**
     * The display name of the user.
     */
    String name();

    /**
     * The {@link IconRenderer} used to display the user's icon.
     */
    IconRenderer iconRenderer();

    /**
     * A basic implementation of {@link ProfileInfo} that holds values directly.
     */
    record Basic(String name, IconRenderer iconRenderer) implements ProfileInfo {
    }
}
