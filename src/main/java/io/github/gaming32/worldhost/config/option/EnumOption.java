package io.github.gaming32.worldhost.config.option;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.config.ConfigProperty;
import io.github.gaming32.worldhost.gui.widget.EnumButton;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringRepresentable;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.JsonWriter;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EnumOption<E extends Enum<E> & StringRepresentable> extends ConfigOption<E> {
    private final Class<E> enumType;
    private final Function<String, E> lookup;
    private final E fallbackValue;

    @SuppressWarnings("unchecked")
    public EnumOption(PropertyDescriptor property) {
        super(property);
        if (!property.getPropertyType().isEnum()) {
            throw new IllegalArgumentException("Using EnumOption for non-enum option " + property.getName());
        }
        if (!StringRepresentable.class.isAssignableFrom(property.getPropertyType())) {
            throw new IllegalArgumentException("EnumOption values need to be StringRepresentable on " + property.getName());
        }

        enumType = (Class<E>)property.getPropertyType();
        lookup = Arrays.stream(enumType.getEnumConstants())
            .collect(Collectors.toMap(StringRepresentable::getSerializedName, Function.identity()))::get;

        final var enumFallback = property.getReadMethod().getAnnotation(ConfigProperty.EnumFallback.class);
        if (enumFallback == null) {
            throw new IllegalArgumentException("@ConfigProperty.EnumFallback not specified for " + property.getName());
        }
        fallbackValue = lookup.apply(enumFallback.value());
        if (fallbackValue == null) {
            throw new IllegalArgumentException("Unknown enum value " + enumFallback.value() + " specified for " + property.getName());
        }
    }

    @Override
    public E readValue(JsonReader reader) throws IOException {
        final String name = reader.nextString();
        final E value = lookup.apply(name);
        if (value == null) {
            WorldHost.LOGGER.warn(
                "Unknown value {} for {}. Defaulting to {}.",
                name, property.getName(), fallbackValue.getSerializedName()
            );
            return fallbackValue;
        }
        return value;
    }

    @Override
    public void writeValue(E value, JsonWriter writer) throws IOException {
        writer.value(value.getSerializedName());
    }

    @Override
    public Collection<? extends AbstractWidget> createWidgets(int x, int y, int width, int height, Font font) {
        final String translationBase = "world-host.config." + property.getName();
        final String tooltipKey = translationBase + ".tooltip";
        final EnumButton<E> button = new EnumButton<>(
            x, y, width, height,
            translationBase,
            Components.translatable(translationBase),
            I18n.exists(tooltipKey) ? Components.translatable(tooltipKey) : null,
            enumType,
            b -> setValue(WorldHost.CONFIG, b.getValue())
        );
        button.setValue(getValue(WorldHost.CONFIG));
        return List.of(button);
    }
}
