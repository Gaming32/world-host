package io.github.gaming32.worldhost.client.gui;

import io.github.gaming32.worldhost.WorldHostTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class FriendsButtonWidget extends ButtonWidget {
    private final int bgX, bgWidth;

    public FriendsButtonWidget(int x, int y, int width, int height, int online, PressAction onPress) {
        this(x, y, width, height, WorldHostTexts.FRIENDS.copy().append("  " + online + " "), online, onPress);
    }

    private FriendsButtonWidget(int x, int y, int width, int height, Text text, int online, PressAction onPress) {
        super(x, y, width, height, text, onPress);
        final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        bgX = width / 2 - textRenderer.getWidth(text) / 2 + textRenderer.getWidth(WorldHostTexts.FRIENDS.copy().append(" "));
        bgWidth = textRenderer.getWidth(" " + online + " ");
    }

    @Override
    protected void renderBackground(MatrixStack matrices, MinecraftClient client, int mouseX, int mouseY) {
        final int baseX = x + bgX;
        final int baseY = y + (height - 12) / 2;
        fill(matrices, baseX, baseY, baseX + bgWidth, baseY + 12, 0x80000000);
    }
}
