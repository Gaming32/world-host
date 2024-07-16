package io.github.gaming32.worldhost.plugin;

import io.github.gaming32.worldhost.SecurityLevel;
import net.minecraft.client.gui.screens.Screen;

import java.util.UUID;

public interface OnlineFriend extends Profilable {
    UUID uuid();

    default SecurityLevel security() {
        return SecurityLevel.SECURE;
    }

    void joinWorld(Screen parentScreen);

    default Joinability joinability() {
        return Joinability.Joinable.INSTANCE;
    }
}
