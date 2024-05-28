package io.github.gaming32.worldhost.mixin;

import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerStatusPinger.class)
public interface ServerStatusPingerAccessor {
    @Invoker
    static Component callFormatPlayerCount(int current, int max) {
        throw new AssertionError();
    }
}
