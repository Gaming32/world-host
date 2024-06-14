package io.github.gaming32.worldhost;

import net.minecraft.resources.ResourceLocation;

public final class ResourceLocations {
    private static final ResourceLocation WORLD_HOST_TEMPLATE = namespaced("world-host", "");

    private ResourceLocations() {
    }

    public static ResourceLocation minecraft(String path) {
        //#if MC >= 1.21
        //$$ return ResourceLocation.withDefaultNamespace(path);
        //#else
        return new ResourceLocation(ResourceLocation.DEFAULT_NAMESPACE, path);
        //#endif
    }

    public static ResourceLocation worldHost(String path) {
        return WORLD_HOST_TEMPLATE.withPath(path);
    }

    public static ResourceLocation namespaced(String namespace, String path) {
        //#if MC >= 1.21
        //$$ return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //#else
        return new ResourceLocation(namespace, path);
        //#endif
    }
}
