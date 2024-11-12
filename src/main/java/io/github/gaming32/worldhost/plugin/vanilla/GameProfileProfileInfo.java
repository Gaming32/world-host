package io.github.gaming32.worldhost.plugin.vanilla;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.toast.IconRenderer;

public final class GameProfileProfileInfo implements ProfileInfo {
    private final GameProfile profile;

    public GameProfileProfileInfo(GameProfile profile) {
        this.profile = profile;
    }

    @Override
    public String name() {
        return !profile.getName().isBlank()
            ? profile.getName()
            : profile.getId().toString();
    }

    @Override
    public IconRenderer iconRenderer() {
        return IconRenderer.createSkinIconRenderer(() -> WorldHost.getSkinLocationNow(profile));
    }
}
