package io.github.gaming32.worldhost.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    //#if MC >= 1.19.2
    @Accessor
    YggdrasilAuthenticationService getAuthenticationService();
    //#endif
}
