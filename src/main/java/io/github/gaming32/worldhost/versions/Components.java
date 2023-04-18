package io.github.gaming32.worldhost.versions;

import net.minecraft.network.chat.MutableComponent;

//#if MC >= 11902
import net.minecraft.network.chat.Component;
//#else
//$$ import net.minecraft.network.chat.TranslatableComponent;
//#endif

public class Components {
    public static final Component EMPTY = immutable("");

    public static MutableComponent translatable(String key) {
        //#if MC >= 11901
        return Component.translatable(key);
        //#else
        //$$ return new TranslatableComponent(key);
        //#endif
    }

    public static Component immutable(String text) {
        return Component.nullToEmpty(text);
    }
}
