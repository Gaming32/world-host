package io.github.gaming32.worldhost.gui.screen;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.GameProfileRenderer;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public class PlayerInfoScreen extends WorldHostScreen {
    private final Screen parentScreen;
    private GameProfile profile;
    private GameProfileRenderer renderer;

    public PlayerInfoScreen(Screen parentScreen, GameProfile profile) {
        super(Components.empty());
        this.parentScreen = parentScreen;
        setProfile(profile);
        WorldHost.resolveGameProfile(profile)
            .thenAccept(this::setProfile)
            .exceptionally(t -> {
                WorldHost.LOGGER.error("Failed to resolve profile {}", profile, t);
                return null;
            });
    }

    private void setProfile(GameProfile profile) {
        if (profileEquals(this.profile, profile)) return;
        this.profile = profile;
        renderer = GameProfileRenderer.create(profile);
    }

    private static boolean profileEquals(GameProfile a, GameProfile b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b) && a.getProperties().equals(b.getProperties());
    }

    @Override
    protected void init() {
        assert minecraft != null;
        addRenderableWidget(
            button(CommonComponents.GUI_BACK, b -> minecraft.setScreen(parentScreen))
                .pos(width / 2 - 75, height / 2 + 100)
                .build()
        );
    }

    @Override
    public void render(
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int mouseX, int mouseY, float partialTick
    ) {
        whRenderBackground(context, mouseX, mouseY, partialTick);
        drawCenteredString(context, font, profile.getName(), width / 2, height / 2 + 85, 0xffffff);
        renderer.renderFacingMouse(context, 0, -25, width, height - 25, 100, mouseX, mouseY);
        super.render(context, mouseX, mouseY, partialTick);
    }
}
