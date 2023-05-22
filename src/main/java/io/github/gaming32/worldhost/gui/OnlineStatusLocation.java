package io.github.gaming32.worldhost.gui;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum OnlineStatusLocation implements StringRepresentable {
    LEFT, RIGHT, OFF;

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    @NotNull
    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
