package io.github.gaming32.worldhost.plugin;

import io.github.gaming32.worldhost.SecurityLevel;
import io.github.gaming32.worldhost.config.WorldHostConfig;
import java.util.UUID;
import net.minecraft.client.gui.screens.Screen;

/**
 * Represents a friend that is currently online, to be shown in the online friends list.
 */
public interface OnlineFriend extends Profilable {
    /**
     * The friend's UUID.
     */
    UUID uuid();

    /**
     * The security level of this friend. I.e. the confidence that this friend is who they say they are. Security
     * levels below {@link SecurityLevel#SECURE SECURE} will display their security level in the UI. Note that this
     * method does not deal with filtering based on {@link WorldHostConfig#getRequiredSecurityLevel the config}.
     * Filtering must be done manually in {@link WorldHostPlugin#refreshOnlineFriends}.
     */
    default SecurityLevel security() {
        return SecurityLevel.SECURE;
    }

    /**
     * Joins this friend's world.
     * @param parentScreen The screen to return to if the user closes your screen.
     */
    void joinWorld(Screen parentScreen);

    /**
     * Returns the {@link Joinability joinability} of this friend. {@link #joinWorld} will not be called on an
     * unjoinable friend.
     */
    default Joinability joinability() {
        return Joinability.Joinable.INSTANCE;
    }
}
