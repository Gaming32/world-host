package io.github.gaming32.worldhost.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

//#if MC < 1.19.4
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.gui.components.Button;
//#endif

public class TooltipEditBox extends EditBox {
    //#if MC < 1.19.4
    //$$ private final Button.OnTooltip onTooltip;
    //#endif

    public TooltipEditBox(Font font, int x, int y, int width, int height, Component message, Component tooltip) {
        super(font, x, y, width, height, message);
        //#if MC >= 1.19.4
        if (tooltip != null) {
            setTooltip(Tooltip.create(tooltip));
        }
        //#else
        //$$ onTooltip = tooltip != null ? WorldHostScreen.onTooltip(tooltip) : Button.NO_TOOLTIP;
        //#endif
    }

    //#if MC < 1.19.4
    //$$ @Override
    //$$ protected void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
    //$$     super.renderButton(pose, mouseX, mouseY, partialTick);
    //$$     if (isHoveredOrFocused()) {
    //$$         renderTooltip(pose, mouseX, mouseY);
    //$$     }
    //$$ }
    //$$
    //$$ @Override
    //$$ public void renderTooltip(PoseStack pose, int mouseX, int mouseY) {
    //$$     onTooltip.onTooltip(this, pose, mouseX, mouseY);
    //$$ }
    //#endif
}
