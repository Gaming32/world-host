package io.github.gaming32.worldhost.plugin.vanilla;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.OnlineFriend;
import io.github.gaming32.worldhost.plugin.WorldHostPlugin;

import java.util.Collection;

@WorldHostPlugin.Entrypoint
public final class VanillaWorldHostPlugin implements WorldHostPlugin {
    @Override
    public int priority() {
        return 1_000_000_000;
    }

    @Override
    public void pingFriends(Collection<OnlineFriend> friends) {
        if (WorldHost.protoClient == null) return;
        WorldHost.protoClient.queryRequest(friends.stream()
            .filter(WorldHostOnlineFriend.class::isInstance)
            .map(OnlineFriend::uuid)
            .toList()
        );
    }

    @Override
    public void refreshFriendsList() {
        if (WorldHost.protoClient == null) return;
        WorldHost.protoClient.listOnline(WorldHost.CONFIG.getFriends());
    }
}
