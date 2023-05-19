package io.github.gaming32.worldhost.toast;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;

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

    private final Font font = Minecraft.getInstance().font;

    @NotNull
    public final List<FormattedCharSequence> title;
    @NotNull
    public final List<FormattedCharSequence> description;
    @Nullable
    public final IconRenderer iconRenderer;
    @Nullable
    public final Runnable clickAction;

    public final int width;
    public final int height;
    public final int ticksTotal;
    public int ticksRemaining;

    public float yShift, prevYShift;

    private boolean clicked;

    public ToastInstance(
        @NotNull Component title,
        @Nullable Component description,
        @Nullable IconRenderer iconRenderer,
        @Nullable Runnable clickAction,
        int ticks
    ) {
        this.title = font.split(title, TEXT_WIDTH);
        this.description = description != null ? font.split(description, TEXT_WIDTH) : Collections.emptyList();
        this.iconRenderer = iconRenderer;
        this.clickAction = clickAction;

        width = TEXT_WIDTH
            + 2 * BORDER_SIZE
            + (iconRenderer != null ? ICON_SIZE + BORDER_SIZE : 0);

        int height = BORDER_SIZE
            + this.title.size() * (font.lineHeight + LINE_BREAK) - LINE_BREAK
            + DESCRIPTION_BREAK
            + this.description.size() * (font.lineHeight + LINE_BREAK) - LINE_BREAK
            + (clickAction != null ? PROGRESS_BORDER_HEIGHT : BORDER_SIZE);
        if (iconRenderer != null) {
            height = Math.max(height, 2 * BORDER_SIZE + ICON_SIZE);
        }
        this.height = height;

        ticksRemaining = ticksTotal = ticks;
    }

    public void render(PoseStack poseStack, float x, float y, int mouseX, int mouseY, float tickDelta) {
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

        fill(poseStack, x, y, x + width, y + height, BACKGROUND_COLOR);

        final float textX = x + BORDER_SIZE + (iconRenderer != null ? ICON_SIZE + BORDER_SIZE : 0);
        float textY = y + BORDER_SIZE;
        for (final FormattedCharSequence line : title) {
            font.draw(poseStack, line, textX, textY, TITLE_COLOR);
            textY += font.lineHeight + LINE_BREAK;
        }
        textY += -LINE_BREAK + DESCRIPTION_BREAK;
        for (final FormattedCharSequence line : description) {
            font.draw(poseStack, line, textX, textY, DESCRIPTION_COLOR);
            textY += font.lineHeight + LINE_BREAK;
        }

        if (iconRenderer != null) {
            poseStack.pushPose();
            poseStack.translate(x, y, 0);
            iconRenderer.draw(poseStack, BORDER_SIZE, BORDER_SIZE, ICON_SIZE, ICON_SIZE);
            poseStack.popPose();
        }

        if (clickAction != null && ticksRemaining > 20) {
            fill(
                poseStack,
                x,
                y + height - PROGRESS_HEIGHT,
                x + Mth.lerp(tickDelta, ticksRemaining - 20, ticksRemaining - 21) / (ticksTotal - 20) * width,
                y + height,
                mouseX >= originalX && mouseX <= originalX + width && mouseY >= y && mouseY <= y + height
                    ? PROGRESS_COLOR_HOVERED : PROGRESS_COLOR
            );
        }
    }

    public boolean click() {
        if (clicked) {
            return false;
        }
        clicked = true;
        ticksRemaining = 20;
        if (clickAction != null) {
            clickAction.run();
        }
        return true;
    }

    private static float calculateX(int ticks) {
        return (float)(-Math.pow(ticks - 16, 2) + 16);
    }

    private static void fill(PoseStack poseStack, float minX, float minY, float maxX, float maxY, int color) {
        Matrix4f matrix4f = poseStack.last().pose();
        if (minX < maxX) {
            float o = minX;
            minX = maxX;
            maxX = o;
        }

        if (minY < maxY) {
            float o = minY;
            minY = maxY;
            maxY = o;
        }

        float f = (float)FastColor.ARGB32.alpha(color) / 255.0F;
        float g = (float)FastColor.ARGB32.red(color) / 255.0F;
        float h = (float)FastColor.ARGB32.green(color) / 255.0F;
        float p = (float)FastColor.ARGB32.blue(color) / 255.0F;
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, minX, minY, 0).color(g, h, p, f).endVertex();
        bufferBuilder.vertex(matrix4f, minX, maxY, 0).color(g, h, p, f).endVertex();
        bufferBuilder.vertex(matrix4f, maxX, maxY, 0).color(g, h, p, f).endVertex();
        bufferBuilder.vertex(matrix4f, maxX, minY, 0).color(g, h, p, f).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();
    }
}
