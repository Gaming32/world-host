package io.github.gaming32.worldhost.client;

import java.util.Set;
import java.util.UUID;

public interface FriendsListUpdate {
    void friendsListUpdate(Set<UUID> friends);

    default void friendsListUpdate() {
        friendsListUpdate(WorldHostClient.ONLINE_FRIENDS);
    }

    default void registerForUpdates() {
        friendsListUpdate();
        WorldHostClient.ONLINE_FRIEND_UPDATES.add(this);
    }
}
