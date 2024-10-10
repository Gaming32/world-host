package io.github.gaming32.worldhost.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

//#if MC >= 1.19.4
import net.minecraft.client.gui.components.Tooltip;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import io.github.gaming32.worldhost.gui.screen.WorldHostScreen;
//$$ import net.minecraft.client.gui.components.Button;
//#endif

public final class TooltipEditBox extends EditBox {
    //#if MC < 1.19.4
    //$$ private final WorldHostScreen.TooltipRenderer tooltip;
    //#endif

    public TooltipEditBox(Font font, int x, int y, int width, int height, Component message, @Nullable Component tooltip) {
        super(font, x, y, width, height, message);
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

    //#if MC < 1.19.4
    //$$ @Override
    //$$ public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
    //$$     super.renderButton(pose, mouseX, mouseY, partialTick);
    //$$     if (isHoveredOrFocused()) {
    //$$         renderToolTip(pose, mouseX, mouseY);
    //$$     }
    //$$ }
    //$$
    //$$ @Override
    //$$ public void renderToolTip(PoseStack pose, int mouseX, int mouseY) {
    //$$     tooltip.render(pose, mouseX, mouseY);
    //$$ }
    //#endif
}
