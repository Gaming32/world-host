package io.github.gaming32.worldhost.plugin.vanilla;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.SecurityLevel;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.OnlineFriend;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record WorldHostOnlineFriend(
    UUID uuid, long connectionId, SecurityLevel security, GameProfile defaultProfile
) implements OnlineFriend {
    public WorldHostOnlineFriend(UUID uuid, long connectionId, SecurityLevel security) {
        this(uuid, connectionId, security, new GameProfile(uuid, ""));
    }

    @Override
    public ProfileInfo fallbackProfileInfo() {
        return new GameProfileProfileInfo(defaultProfile);
    }

    @Override
    public CompletableFuture<ProfileInfo> profileInfo() {
        return CompletableFuture.supplyAsync(
            () -> WorldHost.fetchProfile(Minecraft.getInstance().getMinecraftSessionService(), defaultProfile),
            Util.ioPool() // TODO: nonCriticalIoPool in 1.20.4+
        ).thenApply(GameProfileProfileInfo::new);
    }

    @Override
    public void joinWorld(Screen parentScreen) {
        WorldHost.join(connectionId, parentScreen);
    }
}
