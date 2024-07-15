package io.github.gaming32.worldhost.plugin;

import io.github.gaming32.worldhost.SecurityLevel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.UUID;

public interface OnlineFriend extends Profilable {
    UUID uuid();

    default SecurityLevel security() {
        return SecurityLevel.SECURE;
    }

    void joinWorld(Screen parentScreen);

    default Optional<Component> unjoinableReason() {
        return Optional.empty();
    }
}
