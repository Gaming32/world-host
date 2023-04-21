package io.github.gaming32.worldhost.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.mixin.PlainTextButtonAccessor;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

//#if MC > 11605
import net.minecraft.client.gui.components.PlainTextButton;
//#endif

public class OnlineStatusButton extends PlainTextButton {
    private final int rightX;
    private final Font font;

    private boolean wasOnline = WorldHost.protoClient != null;

    public OnlineStatusButton(int rightX, int y, int height, Font font) {
        super(rightX, y, 0, height, generateStatus(), b -> WorldHost.reconnect(true, true), font);
        this.rightX = rightX;
        this.font = font;
        setWidth(font.width(getMessage()));
        //#if MC >= 11904
        setX(
        //#else
        //$$ x = (
        //#endif
            rightX - getWidth()
        );
    }

    private static Component generateStatus() {
        return Components.empty()
            .append(Components.literal("\u25cf").withStyle(WorldHost.protoClient != null ? ChatFormatting.DARK_GREEN : ChatFormatting.RED))
            .append(" World Host: " + (WorldHost.protoClient != null ? "Online" : "Offline"));
    }

    @Override
    public void
    //#if MC >= 11904
    renderWidget
    //#else
    //$$ renderButton
    //#endif
        (@NotNull PoseStack poseStack, int i, int j, float f) {
        //noinspection DoubleNegation
        if ((WorldHost.protoClient != null) != wasOnline) {
            wasOnline = WorldHost.protoClient != null;
            final var accessor = (PlainTextButtonAccessor)this;
            final Component message = generateStatus();
            setMessage(message);
            accessor.setPTBMessage(message);
            accessor.setUnderlinedMessage(ComponentUtils.mergeStyles(message.copy(), Style.EMPTY.applyFormat(ChatFormatting.UNDERLINE)));
            setWidth(font.width(message));
            //#if MC >= 11904
            setX(
            //#else
            //$$ x = (
            //#endif
                rightX - getWidth()
            );
        }
        super.
            //#if MC >= 11904
            renderWidget
            //#else
            //$$ renderButton
            //#endif
                (poseStack, i, j, f);
    }
}
