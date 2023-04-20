package io.github.gaming32.worldhost.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.FriendsListUpdate;
import io.github.gaming32.worldhost.WorldHostComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public class FriendsButton extends Button implements FriendsListUpdate {
    private int bgX, bgWidth;

    public FriendsButton(int x, int y, int width, int height, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        registerForUpdates();
    }

    @Override
    public void friendsListUpdate(Set<UUID> friends) {
        final int online = friends.size();
        final Component baseText = WorldHostComponents.FRIENDS.copy().append("  " + online + " ");
        setMessage(baseText);
        final Font textRenderer = Minecraft.getInstance().font;
        bgX = width / 2 - textRenderer.width(baseText) / 2 + textRenderer.width(WorldHostComponents.FRIENDS.copy().append(" "));
        bgWidth = textRenderer.width(" " + online + " ");
    }

    @Override
    //#if MC >= 11904
    public void renderString(@NotNull PoseStack poseStack, @NotNull Font font, int i) {
    //#else
    //$$ protected void renderBg(PoseStack matrices, Minecraft minecraft, int mouseX, int mouseY) {
    //#endif
        final int baseX = getX() + bgX;
        final int baseY = getY() + (height - 12) / 2;
        fill(poseStack, baseX, baseY, baseX + bgWidth, baseY + 12, 0x80000000);
        //#if MC >= 11904
        super.renderString(poseStack, font, i);
        //#endif
    }
}
