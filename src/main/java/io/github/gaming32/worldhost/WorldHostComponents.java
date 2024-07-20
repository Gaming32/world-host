package io.github.gaming32.worldhost;

import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.network.chat.Component;

public class WorldHostComponents {
    public static final Component FRIENDS = Components.translatable("world-host.friends");
    public static final Component SERVERS = Components.translatable("world-host.servers");
    public static final Component PLAY_TEXT = Components.translatable("world-host.play_world");
    public static final Component ELLIPSIS = Components.literal("..."); // TODO: Remove in 1.19.2
}
