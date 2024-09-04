package io.github.gaming32.worldhost.plugin.vanilla;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.FriendAdder;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class WorldHostFriendAdder implements FriendAdder {
    public static final Pattern VALID_USERNAME = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
    public static final Pattern VALID_UUID = Pattern.compile("^[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}$");

    @Override
    public Component label() {
        return Components.literal("World Host");
    }

    @Override
    public void searchFriends(String name, int maxResults, Consumer<FriendListFriend> friendConsumer) {
        if (VALID_USERNAME.matcher(name).matches()) {
            WorldHost.getMaybeAsync(
                WorldHost.getProfileCache(), name,
                profile -> profile.map(WorldHostFriendListFriend::new).ifPresent(friendConsumer)
            );
        } else if (VALID_UUID.matcher(name).matches()) {
            friendConsumer.accept(new WorldHostFriendListFriend(UUID.fromString(name)));
        } else if (name.startsWith("o:")) {
            final String actualName = name.substring(2);
            // TODO: Use createOfflinePlayerUUID when 1.19.2+ becomes the minimum, and createOfflineProfile in 1.20.4+
            friendConsumer.accept(new WorldHostFriendListFriend(new GameProfile(
                UUID.nameUUIDFromBytes(("OfflinePlayer:" + actualName).getBytes(StandardCharsets.UTF_8)), actualName
            )));
        }
    }

    @Override
    public boolean delayLookup(String name) {
        return !VALID_UUID.matcher(name).matches() && !name.startsWith("o:");
    }

    @Override
    public int maxValidNameLength() {
        return 36;
    }
}
