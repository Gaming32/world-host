package io.github.gaming32.worldhost.plugin;

import net.minecraft.network.chat.Component;

import java.util.Optional;

public interface FriendListFriend extends Profilable {
    void removeFriend(Runnable refresher);

    default Optional<Component> tag() {
        return Optional.empty();
    }
}
