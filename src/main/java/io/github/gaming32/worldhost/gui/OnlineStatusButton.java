package io.github.gaming32.worldhost.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.mixin.PlainTextButtonAccessor;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

public class OnlineStatusButton extends PlainTextButton {
    private final int rightX;
    private final Font font;

    private boolean wasOnline = WorldHost.protoClient != null;

    public OnlineStatusButton(int rightX, int y, int height, Font font) {
        super(rightX, y, 0, height, generateStatus(), b -> WorldHost.reconnect(true, true), font);
        this.rightX = rightX;
        this.font = font;
        setWidth(font.width(getMessage()));
        setX(rightX - getWidth());
    }

    private static Component generateStatus() {
        return Components.empty()
            .append(Components.literal("\u25cf").withStyle(WorldHost.protoClient != null ? ChatFormatting.DARK_GREEN : ChatFormatting.RED))
            .append(" World Host: " + (WorldHost.protoClient != null ? "Online" : "Offline"));
    }

    @Override
    public void renderWidget(@NotNull PoseStack poseStack, int i, int j, float f) {
        //noinspection DoubleNegation
        if ((WorldHost.protoClient != null) != wasOnline) {
            wasOnline = WorldHost.protoClient != null;
            final var accessor = (PlainTextButtonAccessor)this;
            final Component message = generateStatus();
            setMessage(message);
            accessor.setPTBMessage(message);
            accessor.setUnderlinedMessage(ComponentUtils.mergeStyles(message.copy(), Style.EMPTY.withUnderlined(true)));
            setWidth(font.width(message));
            setX(rightX - getWidth());
        }
        super.renderWidget(poseStack, i, j, f);
    }

    @Override
    public void setY(int i) {
        // Dirty hack to avoid Mod Menu moving buttons around
    }
}
