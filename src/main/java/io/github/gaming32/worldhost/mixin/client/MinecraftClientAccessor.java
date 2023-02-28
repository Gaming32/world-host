package io.github.gaming32.worldhost.mixin.client;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor
    YggdrasilAuthenticationService getAuthenticationService();
}
