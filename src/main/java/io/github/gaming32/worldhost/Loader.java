package io.github.gaming32.worldhost;

import java.util.Locale;

public enum Loader {
    FABRIC, FORGE, NEOFORGE;

    private final String lowercase = name().toLowerCase(Locale.ROOT);

    @Override
    public String toString() {
        return lowercase;
    }
}
