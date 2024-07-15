package io.github.gaming32.worldhost.plugin.vanilla;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.UUID;

public record WorldHostFriendListFriend(
    UUID uuid, GameProfile defaultProfile
) implements FriendListFriend, GameProfileBasedProfilable {
    public WorldHostFriendListFriend(UUID uuid) {
        this(uuid, new GameProfile(uuid, ""));
    }

    public WorldHostFriendListFriend(GameProfile profile) {
        this(profile.getId(), profile);
    }

    @Override
    public void addFriend(boolean notify, Runnable refresher) {
        WorldHost.addFriends(uuid);
        refresher.run();
        if (notify && WorldHost.protoClient != null) {
            WorldHost.protoClient.friendRequest(uuid);
        }
    }

    @Override
    public void removeFriend(Runnable refresher) {
        WorldHost.CONFIG.getFriends().remove(uuid);
        WorldHost.saveConfig();
        refresher.run();
        final var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null && server.isPublished() && WorldHost.protoClient != null) {
            WorldHost.protoClient.closedWorld(Collections.singleton(uuid));
        }
    }
}
