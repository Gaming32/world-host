package io.github.gaming32.worldhost;

import io.github.gaming32.worldhost.plugin.OnlineFriend;

import java.util.Map;
import java.util.UUID;

public interface FriendsListUpdate {
    void friendsListUpdate(Map<UUID, OnlineFriend> friends);

    default void friendsListUpdate() {
        friendsListUpdate(WorldHost.ONLINE_FRIENDS);
    }

    default void registerForUpdates() {
        friendsListUpdate();
        WorldHost.ONLINE_FRIEND_UPDATES.add(this);
    }
}
