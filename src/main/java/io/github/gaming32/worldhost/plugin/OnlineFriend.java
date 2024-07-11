package io.github.gaming32.worldhost.plugin;

import io.github.gaming32.worldhost.SecurityLevel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface OnlineFriend {
    UUID uuid();

    SecurityLevel security();

    ProfileInfo fallbackProfileInfo();

    CompletableFuture<ProfileInfo> profileInfo();

    void joinWorld(Screen parentScreen);

    default Optional<Component> unjoinableReason() {
        return Optional.empty();
    }
}
