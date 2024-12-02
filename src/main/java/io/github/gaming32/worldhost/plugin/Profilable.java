package io.github.gaming32.worldhost.plugin;

import java.util.concurrent.CompletableFuture;

/**
 * Represents an object that can be resolved into a {@link ProfileInfo}.
 */
public interface Profilable {
    /**
     * A {@link ProfileInfo} that is definitely ready now, and may be used before {@link #profileInfo} is complete.
     */
    ProfileInfo fallbackProfileInfo();

    /**
     * Begin resolving a full {@link ProfileInfo}. {@link #fallbackProfileInfo} may be used before the returned
     * {@link CompletableFuture} completes.
     */
    CompletableFuture<ProfileInfo> profileInfo();
}
