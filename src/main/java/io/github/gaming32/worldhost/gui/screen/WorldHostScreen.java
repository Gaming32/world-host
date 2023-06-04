package io.github.gaming32.worldhost.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.versions.ButtonBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

//#if MC < 1_19_04
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.components.Button;
//$$ import org.jetbrains.annotations.NotNull;
//$$
//$$ import java.util.List;
//$$ import java.util.function.Consumer;
//#endif

//#if MC <= 1_16_05
//$$ import net.minecraft.client.gui.components.AbstractWidget;
//#endif

public abstract class WorldHostScreen extends Screen {
    protected WorldHostScreen(Component component) {
        super(component);
    }

    public
    //#if MC > 1_16_01
    static
    //#endif
    void drawRightString(PoseStack poseStack, Font font, Component text, int x, int y, int color) {
        drawString(poseStack, font, text, x - font.width(text), y, color);
    }

    //#if MC < 1_19_04
    //$$ public static Button.OnTooltip onTooltip(Component tooltip) {
    //$$     // 170 matches 1.19.4+
    //$$     final var lines = Minecraft.getInstance().font.split(tooltip, 170);
    //$$     return new Button.OnTooltip() {
    //$$         @Override
    //$$         public void onTooltip(@NotNull Button arg, @NotNull PoseStack arg2, int i, int j) {
    //$$             assert Minecraft.getInstance().screen != null;
    //$$             Minecraft.getInstance().screen.renderTooltip(arg2, lines, i, j);
    //$$         }
    //$$
            //#if MC > 1_16_05
            //$$ @Override
            //$$ public void narrateTooltip(@NotNull Consumer<Component> contents) {
            //$$     contents.accept(tooltip);
            //$$ }
            //#endif
    //$$     };
    //$$ }
    //#endif

    //#if MC <= 1_16_05
    //$$ protected <T extends AbstractWidget> T addRenderableWidget(T widget) {
    //$$     return addButton(widget);
    //$$ }
    //#endif

    public static ButtonBuilder button(Component message, Button.OnPress onPress) {
        return new ButtonBuilder(message, onPress);
    }

    public static void sendRepeatEvents(@SuppressWarnings("unused") boolean sendRepeatEvents) {
        //#if MC < 1_19_04
        //$$ Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(sendRepeatEvents);
        //#endif
    }
}
