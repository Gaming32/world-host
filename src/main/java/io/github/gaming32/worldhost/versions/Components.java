package io.github.gaming32.worldhost.versions;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public class Components {
    // TODO: Remove when 1.19.4 becomes the minimum
    public static MutableComponent copyOnClickText(Object obj) {
        final String text = obj.toString();
        return ComponentUtils.wrapInSquareBrackets(
            Component.literal(text).withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click")))
                .withInsertion(text)
            )
        );
    }
}
