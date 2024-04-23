package io.github.gaming32.worldhost.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.versions.ButtonBuilder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//$$ import net.minecraft.client.gui.GuiComponent;
//#endif

//#if MC < 1.19.4
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.components.Button;
//$$ import org.jetbrains.annotations.NotNull;
//$$
//$$ import java.util.List;
//$$ import java.util.function.Consumer;
//#endif

import net.minecraft.util.FormattedCharSequence;

public abstract class WorldHostScreen extends Screen {
    protected WorldHostScreen(Component component) {
        super(component);
    }

    public static void drawRightString(
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        Font font, Component text, int x, int y, int color
    ) {
        drawString(context, font, text, x - font.width(text), y, color);
    }

    public static void drawCenteredString(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        Font font,
        @NotNull Component text,
        int x, int y, int color
    ) {
        //#if MC >= 1.20.0
        context.
        //#else
        //$$ GuiComponent.
        //#endif
            drawCenteredString(
                //#if MC < 1.20.0
                //$$ context,
                //#endif
                font, text, x, y, color
            );
    }

    public static void drawCenteredString(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        Font font, @NotNull String text, int x, int y, int color
    ) {
        //#if MC >= 1.20.0
        context.
        //#else
        //$$ GuiComponent.
        //#endif
            drawCenteredString(
                //#if MC < 1.20.0
                //$$ context,
                //#endif
                font, text, x, y, color
            );
    }

    //#if MC < 1.20.0
    //$$ @Override
    //#endif
    public void renderComponentTooltip(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        @NotNull List<Component> tooltips, int mouseX, int mouseY
    ) {
        //#if MC >= 1.20.0
        context.renderComponentTooltip
        //#else
        //$$ super.renderComponentTooltip
        //#endif
            (
                //#if MC < 1.20.0
                //$$ context,
                //#else
                font,
                //#endif
                tooltips, mouseX, mouseY
            );
    }

    public static void drawString(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        Font font,
        @NotNull Component text,
        int x, int y, int color
    ) {
        //#if MC >= 1.20.0
        context.
        //#else
        //$$ GuiComponent.
        //#endif
            drawString(
                //#if MC < 1.20.0
                //$$ context,
                //#endif
                font, text, x, y, color
            );
    }

    public static void blit(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        ResourceLocation texture, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight
    ) {
        //#if MC >= 1.20.0
        context.
        //#else
        //$$ RenderSystem.setShaderTexture(0, texture);
        //$$ GuiComponent.
        //#endif
        blit(
            //#if MC < 1.20.0
            //$$ context,
            //#else
            texture,
            //#endif
            x, y, width, height, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight
        );
    }

    public static void blit(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        ResourceLocation texture, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight
    ) {
        //#if MC >= 1.20.0
        context.
        //#else
        //$$ RenderSystem.setShaderTexture(0, texture);
        //$$ GuiComponent.
        //#endif
        blit(
            //#if MC < 1.20.0
            //$$ context,
            //#else
            texture,
            //#endif
            x, y, uOffset, vOffset, width, height, textureWidth, textureHeight
        );
    }

    public static void drawString(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        Font font,
        @NotNull
        FormattedCharSequence text,
        int x, int y, int color, boolean dropShadow
    ) {
        //#if MC < 1.20.0
        //$$ if (dropShadow) {
        //$$     font.drawShadow(context, text, x, y, color);
        //$$ } else {
        //$$     font.draw(context, text, x, y, color);
        //$$ }
        //#else
        context.drawString(font, text, x, y, color, dropShadow);
        //#endif
    }

    public static void drawString(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        Font font, @NotNull String text, int x, int y, int color, boolean dropShadow
    ) {
        //#if MC < 1.20.0
        //$$ if (dropShadow) {
        //$$     font.drawShadow(context, text, x, y, color);
        //$$ } else {
        //$$     font.draw(context, text, x, y, color);
        //$$ }
        //#else
        context.drawString(font, text, x, y, color, dropShadow);
        //#endif
    }

    public static void drawString(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        Font font, @NotNull Component text, int x, int y, int color, boolean dropShadow
    ) {
        //#if MC < 1.20.0
        //$$ if (dropShadow) {
        //$$     font.drawShadow(context, text, x, y, color);
        //$$ } else {
        //$$     font.draw(context, text, x, y, color);
        //$$ }
        //#else
        context.drawString(font, text, x, y, color, dropShadow);
        //#endif
    }

    public static void fill(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int minX, int minY, int maxX, int maxY, int color
    ) {
        //#if MC < 1.20.0
        //$$ GuiComponent.fill(context,
        //#else
        context.fill(
        //#endif
            minX, minY, maxX, maxY, color
        );
    }

    public static PoseStack pose(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context
        //#else
        GuiGraphics context
        //#endif
    ) {
        return context
        //#if MC >= 1.20.0
        .pose()
        //#endif
        ;
    }

    //#if MC < 1.19.4
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
    //$$         @Override
    //$$         public void narrateTooltip(@NotNull Consumer<Component> contents) {
    //$$             contents.accept(tooltip);
    //$$         }
    //$$     };
    //$$ }
    //#endif

    public static ButtonBuilder button(Component message, Button.OnPress onPress) {
        return new ButtonBuilder(message, onPress);
    }

    public static void sendRepeatEvents(@SuppressWarnings("unused") boolean sendRepeatEvents) {
        //#if MC < 1.19.4
        //$$ Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(sendRepeatEvents);
        //#endif
    }

    @Override
    public void renderBackground(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context
        //#else
        GuiGraphics context
        //#endif
        //#if MC >= 1.20.2
        , int mouseX, int mouseY, float delta
        //#endif
    ) {
    }

    public void whRenderBackground(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int mouseX, int mouseY, float delta
    ) {
        //#if MC < 1.20.2
        //$$ super.renderBackground(context);
        //#else
        super.renderBackground(context, mouseX, mouseY, delta);
        //#endif
    }

    public void setListSize(AbstractSelectionList<?> list, int topMargin, int bottomMargin) {
        //#if MC >= 1.20.3
        list.setRectangle(width, height - bottomMargin - topMargin, 0, topMargin);
        //#else
        //$$ list.updateSize(width, height, topMargin, height - bottomMargin);
        //#endif
    }
}
