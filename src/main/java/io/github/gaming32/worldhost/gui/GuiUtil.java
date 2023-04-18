package io.github.gaming32.worldhost.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import static net.minecraft.client.gui.GuiComponent.drawString;

//#if MC < 11904
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.components.Button;
//$$ import org.jetbrains.annotations.NotNull;
//$$ import java.util.function.Consumer;
//#endif

public class GuiUtil {
    public static void drawRightString(PoseStack poseStack, Font font, Component text, int x, int y, int color) {
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
    //$$         @Override
    //$$         public void narrateTooltip(@NotNull Consumer<Component> contents) {
    //$$             contents.accept(tooltip);
    //$$         }
    //$$     };
    //$$ }
    //#endif
}
