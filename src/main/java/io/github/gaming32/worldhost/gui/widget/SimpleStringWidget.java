package io.github.gaming32.worldhost.gui.widget;

import io.github.gaming32.worldhost.gui.screen.WorldHostScreen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

//#if MC >= 1.19.4
import net.minecraft.client.gui.components.Tooltip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//#endif

public final class SimpleStringWidget extends AbstractWidget {
    private final Font font;
    private final int xPos, yPos;

    //#if MC < 1.19.4
    //$$ private final WorldHostScreen.TooltipRenderer tooltip;
    //#endif

    public SimpleStringWidget(int x, int y, Component message, @Nullable Component tooltip, Font font) {
        super(x, y, font.width(message), font.lineHeight, message);
        this.xPos = x;
        this.yPos = y;
        this.font = font;
        active = false;
        //#if MC >= 1.19.4
        if (tooltip != null) {
            setTooltip(Tooltip.create(tooltip));
        }
        //#else
        //$$ this.tooltip = tooltip != null
        //$$     ? WorldHostScreen.TooltipRenderer.create(tooltip)
        //$$     : WorldHostScreen.TooltipRenderer.NONE;
        //#endif
    }

    @Override
    //#if MC >= 1.19.4
    public void renderWidget(
    //#else
    //$$ public void renderButton(
    //#endif
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        @NotNull GuiGraphics context,
        //#endif
        int mouseX, int mouseY, float partialTick
    ) {
        WorldHostScreen.drawString(context, font, getMessage(), xPos, yPos, 0xffffff);
        //#if MC < 1.19.4
        //$$ if (isHoveredOrFocused()) {
        //$$     renderToolTip(context, mouseX, mouseY);
        //$$ }
        //#endif
    }

    //#if MC >= 1.19.4
    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
    }
    //#else
    //$$ @Override
    //$$ public void renderToolTip(PoseStack pose, int mouseX, int mouseY) {
    //$$     tooltip.render(pose, mouseX, mouseY);
    //$$ }
    //$$
    //$$ @Override
    //$$ public void updateNarration(NarrationElementOutput output) {
    //$$ }
    //#endif
}
