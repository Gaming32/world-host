package io.github.gaming32.worldhost;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum SecurityLevel implements StringRepresentable {
    INSECURE, OFFLINE, SECURE;

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    @NotNull
    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public static SecurityLevel byId(int id) {
        return values()[id];
    }
}
