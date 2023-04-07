package io.github.gaming32.worldhost._1_19_2.mixin.client;

import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerStatusPinger.class)
public interface ServerStatusPingerAccessor {
    @Invoker("formatPlayerCount")
    static Component formatPlayerCount(int current, int max) {
        throw new AssertionError();
    }
}
