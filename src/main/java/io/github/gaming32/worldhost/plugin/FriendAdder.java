package io.github.gaming32.worldhost.plugin;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FriendAdder {
    Component label();

    CompletableFuture<List<? extends FriendListFriend>> searchFriends(String name, int maxResults);

    boolean delayLookup(String name);
}
