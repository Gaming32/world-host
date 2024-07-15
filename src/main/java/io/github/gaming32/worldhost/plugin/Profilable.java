package io.github.gaming32.worldhost.plugin;

import java.util.concurrent.CompletableFuture;

public interface Profilable {
    ProfileInfo fallbackProfileInfo();

    CompletableFuture<ProfileInfo> profileInfo();
}
