package io.github.gaming32.worldhost.common;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public class Components {
    public static MutableComponent translatable(String key) {
        return WorldHostCommon.getPlatform().translatableComponent(key);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return WorldHostCommon.getPlatform().translatableComponent(key, args);
    }

    public static MutableComponent literal(String text) {
        return WorldHostCommon.getPlatform().literalComponent(text);
    }

    public static Component immutable(String text) {
        return WorldHostCommon.getPlatform().immutableComponent(text);
    }

    public static MutableComponent wrapInSquareBrackets(Component toWrap) {
        return translatable("chat.square_brackets", toWrap);
    }

    public static MutableComponent copyOnClickText(Object obj) {
        final String text = obj.toString();
        return wrapInSquareBrackets(
            literal(text)
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translatable("chat.copy.click")))
                    .withInsertion(text)
                )
        );
    }
}
