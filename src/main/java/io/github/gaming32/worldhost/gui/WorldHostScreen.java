package io.github.gaming32.worldhost.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.versions.ButtonBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

//#if MC < 11904
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.components.Button;
//$$ import org.jetbrains.annotations.NotNull;
//$$ import java.util.function.Consumer;
//#endif

//#if MC <= 11605
//$$ import net.minecraft.client.gui.components.AbstractWidget;
//#endif

public abstract class WorldHostScreen extends Screen {
    protected WorldHostScreen(Component component) {
        super(component);
    }

    public
    //#if MC > 11601
    static
    //#endif
    void drawRightString(PoseStack poseStack, Font font, Component text, int x, int y, int color) {
        drawString(poseStack, font, text, x - font.width(text), y, color);
    }

    //#if MC < 11904
    //$$ public static Button.OnTooltip onTooltip(Component tooltip) {
    //$$     return new Button.OnTooltip() {
    //$$         @Override
    //$$         public void onTooltip(@NotNull Button arg, @NotNull PoseStack arg2, int i, int j) {
    //$$             assert Minecraft.getInstance().screen != null;
    //$$             Minecraft.getInstance().screen.renderTooltip(arg2, tooltip, i, j);
    //$$         }
    //$$
            //#if MC > 11605
            //$$ @Override
            //$$ public void narrateTooltip(@NotNull Consumer<Component> contents) {
            //$$     contents.accept(tooltip);
            //$$ }
            //#endif
    //$$     };
    //$$ }
    //#endif

    //#if MC <= 11605
    //$$ protected <T extends AbstractWidget> T addRenderableWidget(T widget) {
    //$$     return addButton(widget);
    //$$ }
    //#endif

    public static ButtonBuilder button(Component message, Button.OnPress onPress) {
        return new ButtonBuilder(message, onPress);
    }

    public static void sendRepeatEvents(boolean sendRepeatEvents) {
        //#if MC < 11904
        //$$ Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(sendRepeatEvents);
        //#endif
    }
}
