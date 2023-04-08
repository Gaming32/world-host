package io.github.gaming32.worldhost._1_19_4.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.common.FriendsListUpdate;
import io.github.gaming32.worldhost.common.WorldHostTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.Set;
import java.util.UUID;

public class FriendsButtonWidget extends Button implements FriendsListUpdate {
    private int bgX, bgWidth;

    public FriendsButtonWidget(int x, int y, int width, int height, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
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
    public void renderString(PoseStack poseStack, Font font, int i) {
        final int baseX = getX() + bgX;
        final int baseY = getY() + (height - 12) / 2;
        fill(poseStack, baseX, baseY, baseX + bgWidth, baseY + 12, 0x80000000);
        super.renderString(poseStack, font, i);
    }
}
