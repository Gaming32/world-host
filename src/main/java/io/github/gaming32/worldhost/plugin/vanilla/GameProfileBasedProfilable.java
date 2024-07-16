package io.github.gaming32.worldhost.plugin.vanilla;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.Profilable;
import io.github.gaming32.worldhost.plugin.ProfileInfo;

import java.util.concurrent.CompletableFuture;

public interface GameProfileBasedProfilable extends Profilable {
    GameProfile defaultProfile();

    @Override
    default ProfileInfo fallbackProfileInfo() {
        return new GameProfileProfileInfo(defaultProfile());
    }

    @Override
    default CompletableFuture<ProfileInfo> profileInfo() {
        return WorldHost.resolveProfileInfo(defaultProfile());
    }
}
