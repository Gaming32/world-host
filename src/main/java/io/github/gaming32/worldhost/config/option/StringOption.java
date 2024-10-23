package io.github.gaming32.worldhost.config.option;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.config.ConfigProperty;
import io.github.gaming32.worldhost.gui.screen.WorldHostScreen;
import io.github.gaming32.worldhost.gui.widget.SimpleStringWidget;
import io.github.gaming32.worldhost.gui.widget.TooltipEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.JsonWriter;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public final class StringOption extends ConfigOption<String> {
    private static final Component RESET = Component.translatable("controls.reset");

    private final String defaultValue;

    public StringOption(PropertyDescriptor property) {
        super(property);

        final var stringDefault = property.getReadMethod().getAnnotation(ConfigProperty.StringDefault.class);
        if (stringDefault == null) {
            throw new IllegalArgumentException("@ConfigProperty.StringDefault not specified for " + property.getName());
        }
        defaultValue = stringDefault.value();
    }

    @Override
    public String readValue(JsonReader reader) throws IOException {
        return reader.nextString();
    }

    @Override
    public void writeValue(String value, JsonWriter writer) throws IOException {
        writer.value(value);
    }

    @Override
    public boolean isWide() {
        return true;
    }

    @Override
    public Collection<? extends AbstractWidget> createWidgets(int x, int y, int width, int height, Font font) {
        final String translationBase = "world-host.config." + property.getName();
        final String tooltipKey = translationBase + ".tooltip";

        final Component translation = Component.translatable(translationBase);
        final Component tooltip = I18n.exists(tooltipKey) ? Component.translatable(tooltipKey) : null;

        final var label = new SimpleStringWidget(x + 5, y + 10 - font.lineHeight / 2, translation, tooltip, font);

        final var editBox = new TooltipEditBox(
            font, x + width / 2 + 5, y, width / 2 - 5, height, translation, tooltip
        );
        editBox.setValue(getValue(WorldHost.CONFIG));
        editBox.setResponder(value -> setValue(WorldHost.CONFIG, value));

        final int resetPos = 145 - label.getWidth();
        final var button = WorldHostScreen.button(RESET, b -> editBox.setValue(defaultValue))
            .pos(x + width / 2 - resetPos, y)
            .width(resetPos - 5)
            .build();

        return List.of(label, button, editBox);
    }
}
