package io.github.gaming32.worldhost.client.gui;

import io.github.gaming32.worldhost.WorldHostTexts;
import io.github.gaming32.worldhost.client.FriendsListUpdate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.Set;
import java.util.UUID;

public class FriendsButtonWidget extends ButtonWidget implements FriendsListUpdate {
    private int bgX, bgWidth;

    public FriendsButtonWidget(int x, int y, int width, int height, PressAction onPress) {
        super(x, y, width, height, Text.empty(), onPress);
        registerForUpdates();
    }

    @Override
    public void friendsListUpdate(Set<UUID> friends) {
        final int online = friends.size();
        final Text baseText = WorldHostTexts.FRIENDS.copy().append("  " + online + " ");
        setMessage(baseText);
        final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        bgX = width / 2 - textRenderer.getWidth(baseText) / 2 + textRenderer.getWidth(WorldHostTexts.FRIENDS.copy().append(" "));
        bgWidth = textRenderer.getWidth(" " + online + " ");
    }

    @Override
    protected void renderBackground(MatrixStack matrices, MinecraftClient client, int mouseX, int mouseY) {
        final int baseX = x + bgX;
        final int baseY = y + (height - 12) / 2;
        fill(matrices, baseX, baseY, baseX + bgWidth, baseY + 12, 0x80000000);
    }
}
