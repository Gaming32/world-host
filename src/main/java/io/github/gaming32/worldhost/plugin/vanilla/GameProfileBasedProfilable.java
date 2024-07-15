package io.github.gaming32.worldhost.plugin.vanilla;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.Profilable;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;

public interface GameProfileBasedProfilable extends Profilable {
    GameProfile defaultProfile();

    @Override
    default ProfileInfo fallbackProfileInfo() {
        return new GameProfileProfileInfo(defaultProfile());
    }

    @Override
    default CompletableFuture<ProfileInfo> profileInfo() {
        if (defaultProfile().getId().version() != 4) {
            return CompletableFuture.completedFuture(fallbackProfileInfo());
        }
        return CompletableFuture.supplyAsync(
            () -> WorldHost.fetchProfile(Minecraft.getInstance().getMinecraftSessionService(), defaultProfile()),
            //#if MC >= 1.20.4
            Util.nonCriticalIoPool()
            //#else
            //$$ Util.ioPool()
            //#endif
        ).thenApply(GameProfileProfileInfo::new);
    }
}
