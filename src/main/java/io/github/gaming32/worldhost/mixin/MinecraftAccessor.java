package io.github.gaming32.worldhost.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    //#if MC >= 11902
    @Accessor
    YggdrasilAuthenticationService getAuthenticationService();
    //#endif
}
