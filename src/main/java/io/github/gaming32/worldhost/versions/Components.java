package io.github.gaming32.worldhost.versions;

import com.demonwav.mcdev.annotations.Translatable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

//#if MC < 1.19.2
//$$ import net.minecraft.network.chat.TextComponent;
//$$ import net.minecraft.network.chat.TranslatableComponent;
//#endif

public class Components {
    //#if MC >= 1.19.2
    public static final Component EMPTY = CommonComponents.EMPTY;
    //#else
    //$$ public static final Component EMPTY = TextComponent.EMPTY;
    //#endif

    public static MutableComponent literal(String text) {
        //#if MC >= 1.19.1
        return Component.literal(text);
        //#else
        //$$ return new TextComponent(text);
        //#endif
    }

    public static MutableComponent translatable(@Translatable(foldMethod = true) String key) {
        //#if MC >= 1.19.1
        return Component.translatable(key);
        //#else
        //$$ return new TranslatableComponent(key);
        //#endif
    }

    public static MutableComponent translatable(@Translatable(foldMethod = true) String key, Object... args) {
        //#if MC >= 1.20.3
        return Component.translatableEscape(key, args);
        //#elseif MC >= 1.19.1
        //$$ return Component.translatable(key, args);
        //#else
        //$$ return new TranslatableComponent(key, args);
        //#endif
    }

    public static Component nullToEmpty(String text) {
        return Component.nullToEmpty(text);
    }

    public static MutableComponent empty() {
        return EMPTY.copy();
    }

    public static MutableComponent wrapInSquareBrackets(Component toWrap) {
        return translatable("chat.square_brackets", toWrap);
    }

    public static MutableComponent copyOnClickText(Object obj) {
        final String text = obj.toString();
        return wrapInSquareBrackets(
            literal(text).withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translatable("chat.copy.click")))
                .withInsertion(text)
            )
        );
    }
}
