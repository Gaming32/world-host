package io.github.gaming32.worldhost.plugin;

import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface FriendAdder {
    Component label();

    CompletableFuture<Optional<FriendListFriend>> resolveFriend(String name);

    boolean rateLimit(String name);
}
