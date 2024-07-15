package io.github.gaming32.worldhost.plugin.vanilla;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.plugin.OnlineFriend;
import io.github.gaming32.worldhost.plugin.WorldHostPlugin;

import java.util.Collection;
import java.util.function.Consumer;

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
    public void refreshOnlineFriends() {
        if (WorldHost.protoClient == null) return;
        WorldHost.protoClient.listOnline(WorldHost.CONFIG.getFriends());
    }

    @Override
    public void listFriends(Consumer<FriendListFriend> friendConsumer) {
        WorldHost.CONFIG.getFriends()
            .stream()
            .map(WorldHostFriendListFriend::new)
            .forEach(friendConsumer);
    }
}
