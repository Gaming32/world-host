package io.github.gaming32.worldhost.plugin;

import net.minecraft.network.chat.Component;

import java.util.Optional;

public interface FriendListFriend extends Profilable {
    /**
     * Handles adding a friend. This should only be overridden if you implement a {@link FriendAdder}.
     */
    default void addFriend(boolean notify, Runnable refresher) {
    }

    default boolean supportsNotifyAdd() {
        return false;
    }

    void removeFriend(Runnable refresher);

    default Optional<Component> tag() {
        return Optional.empty();
    }
}
