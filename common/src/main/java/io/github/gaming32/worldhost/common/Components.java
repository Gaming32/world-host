package io.github.gaming32.worldhost.common;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class Components {
    public static MutableComponent translatable(String key) {
        return WorldHostCommon.getPlatform().translatableComponent(key);
    }

    public static Component immutable(String text) {
        return WorldHostCommon.getPlatform().immutableComponent(text);
    }
}
