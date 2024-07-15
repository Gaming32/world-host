package io.github.gaming32.worldhost.plugin.vanilla;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.SecurityLevel;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.OnlineFriend;
import net.minecraft.client.gui.screens.Screen;

import java.util.UUID;

public record WorldHostOnlineFriend(
    UUID uuid, long connectionId, SecurityLevel security, GameProfile defaultProfile
) implements OnlineFriend, GameProfileBasedProfilable {
    public WorldHostOnlineFriend(UUID uuid, long connectionId, SecurityLevel security) {
        this(uuid, connectionId, security, new GameProfile(uuid, ""));
    }

    @Override
    public void joinWorld(Screen parentScreen) {
        WorldHost.join(connectionId, parentScreen);
    }
}
