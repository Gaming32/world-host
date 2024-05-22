package io.github.gaming32.worldhost.config.option;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.widget.YesNoButton;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.JsonWriter;

import java.beans.PropertyDescriptor;
import java.io.IOException;

public final class YesNoOption extends ConfigOption<Boolean> {
    public YesNoOption(PropertyDescriptor property) {
        super(property);
        if (property.getPropertyType() != boolean.class) {
            throw new IllegalArgumentException("Using YesNoOption for non-boolean option");
        }
    }

    @Override
    public Boolean readValue(JsonReader reader) throws IOException {
        return reader.nextBoolean();
    }

    @Override
    public void writeValue(Boolean value, JsonWriter writer) throws IOException {
        writer.value(value);
    }

    @Override
    public Button createButton(int x, int y, int width, int height) {
        final String translationBase = "world-host.config." + property.getName();
        final String tooltipKey = translationBase + ".tooltip";
        final YesNoButton button = new YesNoButton(
            x, y, width, height,
            Components.translatable(translationBase),
            I18n.exists(tooltipKey) ? Components.translatable(tooltipKey) : null,
            b -> setValue(WorldHost.CONFIG, b.isToggled())
        );
        button.setToggled(getValue(WorldHost.CONFIG));
        return button;
    }
}
