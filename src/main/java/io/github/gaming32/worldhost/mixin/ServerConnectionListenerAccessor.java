package io.github.gaming32.worldhost.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ServerConnectionListener.class)
public interface ServerConnectionListenerAccessor {
    @Accessor
    List<Connection> getConnections();
}
