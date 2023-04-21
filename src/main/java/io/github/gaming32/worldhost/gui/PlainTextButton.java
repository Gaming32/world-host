//#if MC <= 11605
//$$ package io.github.gaming32.worldhost.gui;
//$$
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.ChatFormatting;
//$$ import net.minecraft.client.gui.Font;
//$$ import net.minecraft.client.gui.components.Button;
//$$ import net.minecraft.network.chat.Component;
//$$ import net.minecraft.network.chat.ComponentUtils;
//$$ import net.minecraft.network.chat.Style;
//$$ import net.minecraft.util.Mth;
//$$ import org.jetbrains.annotations.NotNull;
//$$
//$$ public class PlainTextButton extends Button {
//$$     private final Font font;
//$$     private final Component message;
//$$     private final Component underlinedMessage;
//$$
//$$     public PlainTextButton(int x, int y, int width, int height, Component component, Button.OnPress onPress, Font font) {
//$$         super(x, y, width, height, component, onPress);
//$$         this.font = font;
//$$         this.message = component;
//$$         this.underlinedMessage = ComponentUtils.mergeStyles(component.copy(), Style.EMPTY.applyFormat(ChatFormatting.UNDERLINE));
//$$     }
//$$
//$$     @Override
//$$     public void renderButton(@NotNull PoseStack poseStack, int i, int j, float f) {
//$$         Component component = this.isHovered() ? this.underlinedMessage : this.message;
//$$         drawString(poseStack, this.font, component, this.x, this.y, 0xffffff | Mth.ceil(this.alpha * 255.0F) << 24);
//$$     }
//$$ }
//#endif
