package io.github.gaming32.worldhost;

import java.util.Set;
import java.util.UUID;

public interface FriendsListUpdate {
    void friendsListUpdate(Set<UUID> friends);

    default void friendsListUpdate() {
        friendsListUpdate(WorldHost.ONLINE_FRIENDS);
    }

    default void registerForUpdates() {
        friendsListUpdate();
        WorldHost.ONLINE_FRIEND_UPDATES.add(this);
    }
}
