package io.github.gaming32.worldhost.gui._1_19_4.mixin.client;

import io.github.gaming32.worldhost.common.gui.ButtonBuilder;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Button.Builder.class)
public class MixinButton_Builder implements ButtonBuilder {
    private Button.Builder wh$builder() {
        return (Button.Builder)(Object)this;
    }

    @Override
    public ButtonBuilder pos(int x, int y) {
        return (ButtonBuilder)wh$builder().pos(x, y);
    }

    @Override
    public ButtonBuilder width(int width) {
        return (ButtonBuilder)wh$builder().width(width);
    }

    @Override
    public ButtonBuilder size(int width, int height) {
        return (ButtonBuilder)wh$builder().size(width, height);
    }

    @Override
    public ButtonBuilder bounds(int x, int y, int width, int height) {
        return (ButtonBuilder)wh$builder().bounds(x, y, width, height);
    }

    @Override
    public ButtonBuilder tooltip(Component tooltip) {
        return (ButtonBuilder)wh$builder().tooltip(Tooltip.create(tooltip));
    }

    @Override
    public Button build() {
        return wh$builder().build();
    }
}
