package io.github.gaming32.worldhost.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.FriendsListUpdate;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class FriendsButton extends Button implements FriendsListUpdate {
    private int bgX, bgWidth;

    public FriendsButton(int x, int y, int width, int height, OnPress onPress) {
        super(
            x, y, width, height, Components.EMPTY, onPress
            //#if MC >= 11904
            , DEFAULT_NARRATION
            //#endif
        );
        registerForUpdates();
    }

    @Override
    public void friendsListUpdate(Map<UUID, Long> friends) {
        final int online = friends.size();
        final Component baseText = WorldHostComponents.FRIENDS.copy().append("  " + online + " ");
        setMessage(baseText);
        final Font textRenderer = Minecraft.getInstance().font;
        bgX = width / 2 - textRenderer.width(baseText) / 2 + textRenderer.width(WorldHostComponents.FRIENDS.copy().append(" "));
        bgWidth = textRenderer.width(" " + online + " ");
    }

    //#if MC < 11904
    //$$ private int getX() {
    //$$     return x;
    //$$ }
    //$$
    //$$ private int getY() {
    //$$     return y;
    //$$ }
    //#endif

    @Override
    //#if MC >= 11904
    public void renderString(@NotNull PoseStack poseStack, @NotNull Font font, int i) {
    //#else
    //$$ protected void renderBg(@NotNull PoseStack poseStack, @NotNull Minecraft minecraft, int mouseX, int mouseY) {
    //#endif
        final int baseX = getX() + bgX;
        final int baseY = getY() + (height - 12) / 2;
        fill(poseStack, baseX, baseY, baseX + bgWidth, baseY + 12, 0x80000000);
        //#if MC >= 11904
        super.renderString(poseStack, font, i);
        //#endif
    }
}
