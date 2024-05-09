package io.github.gaming32.worldhost;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.util.UUID;

public interface FriendsListUpdate {
    void friendsListUpdate(Object2LongMap<UUID> friends);

    default void friendsListUpdate() {
        friendsListUpdate(WorldHost.ONLINE_FRIENDS);
    }

    default void registerForUpdates() {
        friendsListUpdate();
        WorldHost.ONLINE_FRIEND_UPDATES.add(this);
    }
}
