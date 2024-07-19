package io.github.gaming32.worldhost.plugin.vanilla;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.FriendAdder;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class WorldHostFriendAdder implements FriendAdder {
    public static final Pattern VALID_USERNAME = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
    public static final Pattern VALID_UUID = Pattern.compile("^[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}$");

    @Override
    public Component label() {
        return Components.literal("World Host");
    }

    @Override
    public CompletableFuture<List<? extends FriendListFriend>> searchFriends(String name, int maxResults) {
        if (VALID_USERNAME.matcher(name).matches()) {
            final CompletableFuture<Optional<GameProfile>> result = new CompletableFuture<>();
            WorldHost.getMaybeAsync(WorldHost.getProfileCache(), name, result::complete);
            return result.thenApply(p -> p.map(WorldHostFriendListFriend::new).map(List::of).orElse(List.of()));
        }
        if (VALID_UUID.matcher(name).matches()) {
            return CompletableFuture.completedFuture(List.of(
                new WorldHostFriendListFriend(UUID.fromString(name))
            ));
        }
        if (name.startsWith("o:")) {
            final String actualName = name.substring(2);
            // TODO: Use createOfflinePlayerUUID when 1.19.2+ becomes the minimum, and createOfflineProfile in 1.20.4+
            return CompletableFuture.completedFuture(List.of(new WorldHostFriendListFriend(new GameProfile(
                UUID.nameUUIDFromBytes(("OfflinePlayer:" + actualName).getBytes(StandardCharsets.UTF_8)), actualName
            ))));
        }
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public boolean delayLookup(String name) {
        return !VALID_UUID.matcher(name).matches() && !name.startsWith("o:");
    }
}
