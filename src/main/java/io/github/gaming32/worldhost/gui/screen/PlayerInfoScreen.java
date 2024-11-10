package io.github.gaming32.worldhost.gui.screen;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.widget.WHPlayerSkinWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public class PlayerInfoScreen extends WorldHostScreen {
    private final Screen parentScreen;
    private GameProfile profile;

    public PlayerInfoScreen(Screen parentScreen, GameProfile profile) {
        super(Component.empty());
        this.parentScreen = parentScreen;

        this.profile = profile;
        WorldHost.resolveGameProfile(profile)
            .thenAccept(ready -> this.profile = ready)
            .exceptionally(t -> {
                WorldHost.LOGGER.error("Failed to resolve profile {}", profile, t);
                return null;
            });
    }

    @Override
    protected void init() {
        assert minecraft != null;
        final int top = height / 4 - 25;
        final int bottom = height / 2 + 75;
        addRenderableWidget(new WHPlayerSkinWidget(
            width / 2 - 100, top,
            200, bottom - top,
            () -> WorldHost.getInsecureSkin(profile),
            minecraft.getEntityModels()
        ));
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
        super.render(context, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parentScreen);
    }
}
