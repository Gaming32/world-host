package io.github.gaming32.worldhost.toast;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.gui.screen.WorldHostScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#endif

import net.minecraft.util.FormattedCharSequence;

class ToastInstance {
    private static final int TEXT_WIDTH = 200;
    private static final int BORDER_SIZE = 5;
    private static final int ICON_SIZE = 20;
    private static final int DESCRIPTION_BREAK = 6;
    private static final int LINE_BREAK = 3;
    private static final int PROGRESS_HEIGHT = 3;
    private static final int PROGRESS_BORDER_HEIGHT = 6;

    private static final int BACKGROUND_COLOR = 0xff262626;
    private static final int PROGRESS_COLOR = 0xff4d4d4d;
    private static final int PROGRESS_COLOR_HOVERED = 0xff6e6e6e;
    private static final int TITLE_COLOR = 0xffd6d6d6;
    private static final int DESCRIPTION_COLOR = 0xffa3a3a3;

    public final Font font = Minecraft.getInstance().font;

    @NotNull
    public final Component title;
    @Nullable
    public final Component description;
    @Nullable
    public final IconRenderer iconRenderer;
    @Nullable
    public final Runnable clickAction;
    public final boolean important;

    public final int width;
    public int height;
    public final int ticksTotal;
    public int ticksRemaining;

    public List<FormattedCharSequence> formattedTitle;
    public List<FormattedCharSequence> formattedDescription;

    public float y;

    private boolean clicked;

    public ToastInstance(
        @NotNull Component title,
        @Nullable Component description,
        @Nullable IconRenderer iconRenderer,
        @Nullable Runnable clickAction,
        boolean important,
        int ticks
    ) {
        this.title = title;
        this.description = description;
        this.iconRenderer = iconRenderer;
        this.clickAction = clickAction;
        this.important = important;

        width = TEXT_WIDTH
            + 2 * BORDER_SIZE
            + (iconRenderer != null ? ICON_SIZE + BORDER_SIZE : 0);

        ticksRemaining = ticksTotal = ticks;
    }

    public void calculateText() {
        formattedTitle = font.split(title, TEXT_WIDTH);
        formattedDescription = description != null ? font.split(description, TEXT_WIDTH) : Collections.emptyList();
        height = BORDER_SIZE
            + this.formattedTitle.size() * (font.lineHeight + LINE_BREAK) - LINE_BREAK
            + DESCRIPTION_BREAK
            + this.formattedDescription.size() * (font.lineHeight + LINE_BREAK) - LINE_BREAK
            + (clickAction != null ? PROGRESS_BORDER_HEIGHT : BORDER_SIZE);
        if (iconRenderer != null) {
            height = Math.max(height, 2 * BORDER_SIZE + ICON_SIZE);
        }
    }

    public void render(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        float x, float y, int mouseX, int mouseY, float tickDelta
    ) {
        final PoseStack poseStack = WorldHostScreen.pose(context);

        final float originalX = x;

        final float prevX;
        if (ticksRemaining < 20) {
            prevX = x - calculateX(ticksRemaining + 1);
            x -= calculateX(ticksRemaining);
        } else if (ticksTotal - ticksRemaining <= 20) {
            prevX = x - calculateX(ticksTotal - ticksRemaining - 1);
            x -= calculateX(ticksTotal - ticksRemaining);
        } else {
            prevX = x;
        }
        x = Mth.lerp(tickDelta, prevX, x);

        fill(context, x, y, x + width, y + height, BACKGROUND_COLOR);

        poseStack.pushPose();
        poseStack.translate(x + BORDER_SIZE + (iconRenderer != null ? ICON_SIZE + BORDER_SIZE : 0), y + BORDER_SIZE, 0);
        for (final var line : formattedTitle) {
            WorldHostScreen.drawString(context, font, line, 0, 0, TITLE_COLOR, false);
            poseStack.translate(0, font.lineHeight + LINE_BREAK, 0);
        }
        poseStack.translate(0, -LINE_BREAK + DESCRIPTION_BREAK, 0);
        for (final var line : formattedDescription) {
            WorldHostScreen.drawString(context, font, line, 0, 0, DESCRIPTION_COLOR, false);
            poseStack.translate(0, font.lineHeight + LINE_BREAK, 0);
        }
        poseStack.popPose();

        if (iconRenderer != null) {
            poseStack.pushPose();
            poseStack.translate(x, y, 0);
            iconRenderer.draw(context, BORDER_SIZE, BORDER_SIZE, ICON_SIZE, ICON_SIZE);
            poseStack.popPose();
        }

        if (clickAction != null && ticksRemaining > 20) {
            fill(
                context,
                x,
                y + height - PROGRESS_HEIGHT,
                x + Mth.lerp(tickDelta, ticksRemaining - 20, ticksRemaining - 21) / (ticksTotal - 20) * width,
                y + height,
                mouseX >= originalX && mouseX <= originalX + width && mouseY >= y && mouseY <= y + height
                    ? PROGRESS_COLOR_HOVERED : PROGRESS_COLOR
            );
        }
    }

    public boolean click(int button) {
        if (clicked) {
            return false;
        }
        if (button != GLFW_MOUSE_BUTTON_LEFT && button != GLFW_MOUSE_BUTTON_MIDDLE) {
            return false;
        }
        clicked = true;
        ticksRemaining = Math.min(ticksRemaining, 20);
        if (clickAction != null && button != GLFW_MOUSE_BUTTON_MIDDLE) {
            clickAction.run();
        }
        return true;
    }

    private static float calculateX(int ticks) {
        return (float)(-Math.pow(ticks - 16, 2) + 16);
    }

    private static void fill(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        float minX, float minY, float maxX, float maxY, int color
    ) {
        final PoseStack poseStack = WorldHostScreen.pose(context);
        poseStack.pushPose();
        poseStack.translate(minX, minY, 0f);
        poseStack.scale(maxX - minX, maxY - minY, 1f);
        WorldHostScreen.fill(context, 0, 0, 1, 1, color);
        poseStack.popPose();
    }
}
