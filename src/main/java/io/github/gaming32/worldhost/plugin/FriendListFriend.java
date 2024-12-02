package io.github.gaming32.worldhost.plugin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Represents a friend to display in the friend list or on the add friend screen.
 */
public interface FriendListFriend extends Profilable {
    /**
     * Handles adding a friend. This should only be overridden if you implement a {@link FriendAdder}.
     * @param refresher {@link Runnable} to invoke when adding is complete. This may be called from any thread.
     */
    default void addFriend(boolean notify, Runnable refresher) {
    }

    /**
     * Whether this friend supports the ability to toggle sending a notification when they are added. Even if this
     * returns {@code true}, the notification need not be shown if the user receiving the friend request disables such
     * notifications.
     */
    default boolean supportsNotifyAdd() {
        return false;
    }

    /**
     * Removes this friend from the friend list.
     * @param refresher {@link Runnable} to invoke when removal is complete. This may be called from any thread.
     */
    void removeFriend(Runnable refresher);

    /**
     * Displays friend information in some capacity. This may be displaying an info screen or opening a webpage.
     * @param parentScreen The screen to return to if the user closes a screen you open for this purpose.
     */
    void showFriendInfo(Screen parentScreen);

    /**
     * A "tag" to show next to the friend's name, if any. If specified, the friend's name will be displayed as
     * "Username (Tag)".
     */
    default Optional<Component> tag() {
        return Optional.empty();
    }
}
