package io.github.gaming32.worldhost.common;

import java.util.Set;
import java.util.UUID;

public interface FriendsListUpdate {
    void friendsListUpdate(Set<UUID> friends);

    default void friendsListUpdate() {
        friendsListUpdate(WorldHostCommon.ONLINE_FRIENDS);
    }

    default void registerForUpdates() {
        friendsListUpdate();
        WorldHostCommon.ONLINE_FRIEND_UPDATES.add(this);
    }
}
