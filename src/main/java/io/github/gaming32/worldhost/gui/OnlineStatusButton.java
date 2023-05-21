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

import java.util.concurrent.Future;
import java.util.function.Supplier;

//#if MC > 11605
import net.minecraft.client.gui.components.PlainTextButton;
//#endif

public class OnlineStatusButton extends PlainTextButton {
    private static final ChatFormatting[] COLORS = {
        ChatFormatting.RED,
        ChatFormatting.GOLD,
        ChatFormatting.DARK_GREEN
    };

    @SuppressWarnings("unchecked")
    private static final Supplier<Component>[] TEXTS = new Supplier[] {
        () -> Components.translatable("world-host.online_status.offline", WorldHost.reconnectDelay / 20 + 1),
        () -> Components.translatable("world-host.online_status.connecting"),
        () -> Components.translatable("world-host.online_status.online")
    };

    private final int rightX;
    private final Font font;

    private int currentStatus = getStatus();

    public OnlineStatusButton(int rightX, int y, int height, Font font) {
        super(rightX, y, 0, height, generateStatusComponent(), b -> {
            if (getStatus() != 1) {
                WorldHost.reconnect(true, true);
            }
        }, font);
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

    private static int getStatus() {
        if (WorldHost.protoClient == null) {
            return 0;
        }
        final Future<Void> connectingFuture = WorldHost.protoClient.getConnectingFuture();
        return connectingFuture.isDone() ? 2 : 1;
    }

    private static Component generateStatusComponent() {
        final int status = getStatus();
        return Components.translatable(
            "world-host.online_status",
            Components.literal("\u25cf").withStyle(COLORS[status]),
            TEXTS[status].get()
        );
    }

    @Override
    public void
    //#if MC >= 11904
    renderWidget
    //#else
    //$$ renderButton
    //#endif
        (@NotNull PoseStack poseStack, int i, int j, float f) {
        final int status = getStatus();
        if (status != currentStatus || (status == 0 && (WorldHost.reconnectDelay + 1) % 20 == 0)) {
            currentStatus = status;
            final var accessor = (PlainTextButtonAccessor)this;
            final Component message = generateStatusComponent();
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

    //#if MC > 11605
    @Override
    public boolean isHoveredOrFocused() {
        return currentStatus != 1 && super.isHoveredOrFocused();
    }
    //#else
    //$$ @Override
    //$$ public boolean isHovered() {
    //$$     return currentStatus != 1 && super.isHovered();
    //$$ }
    //#endif
}
