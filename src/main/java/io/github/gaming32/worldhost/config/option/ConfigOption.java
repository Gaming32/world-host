package io.github.gaming32.worldhost.config.option;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.config.ConfigProperty;
import io.github.gaming32.worldhost.config.WorldHostConfig;
import net.minecraft.client.gui.components.Button;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.JsonWriter;

import java.beans.PropertyDescriptor;
import java.io.IOException;

public abstract sealed class ConfigOption<T> permits EnumOption, YesNoOption {
    protected final PropertyDescriptor property;
    private final int order;
    private Runnable onSet = null;

    protected ConfigOption(PropertyDescriptor property) {
        this.property = property;

        final ConfigProperty info = property.getReadMethod().getAnnotation(ConfigProperty.class);
        order = info.order();
    }

    public String getName() {
        return property.getName();
    }

    public int getOrder() {
        return order;
    }

    public void onSet(Runnable onSet) {
        if (this.onSet == null) {
            this.onSet = onSet;
        } else {
            final Runnable previous = this.onSet;
            this.onSet = () -> {
                previous.run();
                onSet.run();
            };
        }
    }

    // TODO: UncheckedReflectiveOperationException when 1.20.4+ becomes the minimum
    @SuppressWarnings("unchecked")
    public T getValue(WorldHostConfig config) {
        try {
            return (T)property.getReadMethod().invoke(config);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public void setValue(WorldHostConfig config, T value) {
        setValueSilently(config, value);
        WorldHost.saveConfig();
        if (onSet != null) {
            onSet.run();
        }
    }

    // TODO: UncheckedReflectiveOperationException when 1.20.4+ becomes the minimum
    public void setValueSilently(WorldHostConfig config, T value) {
        try {
            property.getWriteMethod().invoke(config, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public final void readValue(JsonReader reader, WorldHostConfig config) throws IOException {
        setValueSilently(config, readValue(reader));
    }

    public abstract T readValue(JsonReader reader) throws IOException;

    public final void writeValue(WorldHostConfig config, JsonWriter writer) throws IOException {
        writeValue(getValue(config), writer);
    }

    public abstract void writeValue(T value, JsonWriter writer) throws IOException;

    public abstract Button createButton(int x, int y, int width, int height);
}
