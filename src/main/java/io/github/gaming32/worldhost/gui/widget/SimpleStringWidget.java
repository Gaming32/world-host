package io.github.gaming32.worldhost.gui.widget;

import io.github.gaming32.worldhost.gui.screen.WorldHostScreen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

//#if MC < 1.20.0
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

//#if MC < 1.19.4
//$$ import net.minecraft.client.gui.components.Button;
//#endif

public class SimpleStringWidget extends AbstractWidget {
    private final Font font;
    private final int xPos, yPos;

    //#if MC < 1.19.4
    //$$ private final Button.OnTooltip onTooltip;
    //#endif

    public SimpleStringWidget(int x, int y, Component message, Component tooltip, Font font) {
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
        //$$ onTooltip = tooltip != null ? WorldHostScreen.onTooltip(tooltip) : Button.NO_TOOLTIP;
        //#endif
    }

    @Override
    //#if MC >= 1.19.4
    protected void renderWidget(
    //#else
    //$$ protected void renderButton(
    //#endif
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int mouseX, int mouseY, float partialTick
    ) {
        WorldHostScreen.drawString(context, font, getMessage(), xPos, yPos, 0xffffff);
        //#if MC < 1.19.4
        //$$ if (isHoveredOrFocused()) {
        //$$     renderTooltip(context, mouseX, mouseY);
        //$$ }
        //#endif
    }

    //#if MC < 1.19.4
    //$$ @Override
    //$$ public void renderTooltip(PoseStack pose, int mouseX, int mouseY) {
    //$$     onTooltip.onTooltip(this, pose, mouseX, mouseY);
    //$$ }
    //#endif

    //#if MC >= 1.19.3
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
    //#endif
}
