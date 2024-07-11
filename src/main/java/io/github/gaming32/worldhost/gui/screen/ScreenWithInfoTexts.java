package io.github.gaming32.worldhost.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.InfoTextsCategory;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public abstract class ScreenWithInfoTexts extends WorldHostScreen {
    private final List<Component> infoTexts;

    protected ScreenWithInfoTexts(Component component, InfoTextsCategory category) {
        super(component);
        infoTexts = WorldHost.getInfoTexts(category);
    }

    public final int getInfoTextsAdjustedBottomMargin(int baseMargin) {
        return !infoTexts.isEmpty()
            ? baseMargin + 10 + font.lineHeight * infoTexts.size()
            : baseMargin;
    }

    @Override
    public void render(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int mouseX, int mouseY, float delta
    ) {
        int textY = height - 73;
        for (final Component line : infoTexts.reversed()) {
            drawCenteredString(context, font, line, width / 2, textY, 0xffffff);
            textY -= font.lineHeight;
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int textY = height - 73;
        for (final Component line : infoTexts.reversed()) {
            final int textWidth = font.width(line);
            final int textX = width / 2 - textWidth / 2;
            if (mouseX >= textX && mouseX <= textX + textWidth) {
                if (mouseY >= textY && mouseY <= textY + font.lineHeight) {
                    final var style = font.getSplitter().componentStyleAtWidth(line, (int)Math.round(mouseX) - textX);
                    if (style != null) {
                        handleComponentClicked(style);
                        return true;
                    }
                }
            }
            textY -= font.lineHeight;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
