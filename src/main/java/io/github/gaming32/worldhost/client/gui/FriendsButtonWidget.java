package io.github.gaming32.worldhost.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.WorldHostTexts;
import io.github.gaming32.worldhost.client.FriendsListUpdate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.Set;
import java.util.UUID;

public class FriendsButtonWidget extends Button implements FriendsListUpdate {
    private int bgX, bgWidth;

    public FriendsButtonWidget(int x, int y, int width, int height, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress);
        registerForUpdates();
    }

    @Override
    public void friendsListUpdate(Set<UUID> friends) {
        final int online = friends.size();
        final Component baseText = WorldHostTexts.FRIENDS.copy().append("  " + online + " ");
        setMessage(baseText);
        final Font textRenderer = Minecraft.getInstance().font;
        bgX = width / 2 - textRenderer.width(baseText) / 2 + textRenderer.width(WorldHostTexts.FRIENDS.copy().append(" "));
        bgWidth = textRenderer.width(" " + online + " ");
    }

    @Override
    protected void renderBg(PoseStack matrices, Minecraft client, int mouseX, int mouseY) {
        final int baseX = x + bgX;
        final int baseY = y + (height - 12) / 2;
        fill(matrices, baseX, baseY, baseX + bgWidth, baseY + 12, 0x80000000);
    }
}
