package io.github.gaming32.worldhost.gui.widget;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.screen.WorldHostConfigScreen;
import io.github.gaming32.worldhost.mixin.PlainTextButtonAccessor;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

//#if MC > 1.17.1
import net.minecraft.client.gui.components.PlainTextButton;
//#else
//$$ import io.github.gaming32.worldhost.gui.PlainTextButton;
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

    private final int alignedX;
    private final boolean rightAlign;
    private final Font font;

    private int currentStatus = getStatus();

    public OnlineStatusButton(int alignedX, int y, int height, boolean rightAlign, Font font) {
        super(alignedX, y, 0, height, generateStatusComponent(), b -> {
            if (Screen.hasShiftDown()) {
                if (getStatus() != 1) {
                    WorldHost.reconnect(true, true);
                }
            } else {
                final Minecraft minecraft = Minecraft.getInstance();
                minecraft.setScreen(new WorldHostConfigScreen(minecraft.screen));
            }
        }, font);
        this.alignedX = alignedX;
        this.font = font;
        this.rightAlign = rightAlign;
        setWidth(font.width(getMessage()));
        updateX();
    }

    private void updateX() {
        //#if MC >= 1.19.4
        setX(
            //#else
            //$$ x = (
            //#endif
            rightAlign ? alignedX - getWidth() : alignedX
        );
    }

    private static int getStatus() {
        if (WorldHost.protoClient == null) {
            return 0;
        }
        return WorldHost.protoClient.getConnectingFuture().isDone() ? 2 : 1;
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
    //#if MC >= 1.19.4
    renderWidget
    //#else
    //$$ renderButton
    //#endif
        (
            @NotNull
            //#if MC < 1.20.0
            //$$ PoseStack context,
            //#else
            GuiGraphics context,
            //#endif
            int i, int j, float f
        ) {
        final int status = getStatus();
        if (status != currentStatus || (status == 0 && (WorldHost.reconnectDelay + 1) % 20 == 0)) {
            currentStatus = status;
            final var accessor = (PlainTextButtonAccessor)this;
            final Component message = generateStatusComponent();
            setMessage(message);
            accessor.setPTBMessage(message);
            accessor.setUnderlinedMessage(ComponentUtils.mergeStyles(message.copy(), Style.EMPTY.applyFormat(ChatFormatting.UNDERLINE)));
            setWidth(font.width(message));
            updateX();
        }
        super.
            //#if MC >= 1.19.4
            renderWidget
            //#else
            //$$ renderButton
            //#endif
                (context, i, j, f);
    }

    //#if MC >= 1.18.0
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
