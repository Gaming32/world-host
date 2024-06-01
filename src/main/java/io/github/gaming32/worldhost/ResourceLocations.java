package io.github.gaming32.worldhost;

import net.minecraft.resources.ResourceLocation;

public final class ResourceLocations {
    private ResourceLocations() {
    }

    public static ResourceLocation minecraft(String path) {
        return new ResourceLocation(ResourceLocation.DEFAULT_NAMESPACE, path);
    }

    public static ResourceLocation worldHost(String path) {
        return new ResourceLocation("world-host", path);
    }
}
