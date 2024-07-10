package io.github.gaming32.worldhost;

import com.google.common.collect.ComparisonChain;
import io.github.gaming32.worldhost.plugin.WorldHostPlugin;
import org.jetbrains.annotations.NotNull;

public record LoadedWorldHostPlugin(
    String modId, WorldHostPlugin plugin
) implements Comparable<LoadedWorldHostPlugin> {
    @Override
    public int compareTo(@NotNull LoadedWorldHostPlugin o) {
        return ComparisonChain.start()
            .compare(o.plugin.priority(), plugin.priority())
            .compare(modId, o.modId)
            .compare(plugin.getClass().getName(), o.plugin.getClass().getName())
            .result();
    }
}
