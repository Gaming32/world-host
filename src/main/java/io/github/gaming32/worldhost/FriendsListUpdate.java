package io.github.gaming32.worldhost;

import java.util.Map;
import java.util.UUID;

public interface FriendsListUpdate {
    // TODO: fastutil
    void friendsListUpdate(Map<UUID, Long> friends);

    default void friendsListUpdate() {
        friendsListUpdate(WorldHost.ONLINE_FRIENDS);
    }

    default void registerForUpdates() {
        friendsListUpdate();
        WorldHost.ONLINE_FRIEND_UPDATES.add(this);
    }
}
