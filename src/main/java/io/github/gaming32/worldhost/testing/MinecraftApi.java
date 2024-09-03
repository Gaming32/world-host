package io.github.gaming32.worldhost.testing;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.intellij.lang.annotations.RegExp;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

//#if MC >= 1.19.2
import net.minecraft.network.chat.contents.TranslatableContents;
//#else
//$$ import net.minecraft.network.chat.TranslatableComponent;
//#endif

public class MinecraftApi {
    private static final Thread.Builder SLEEP_BUILDER = Thread.ofVirtual().name("WH-TestingSleeper-", 1);

    public static void click(AbstractWidget widget) {
        click(
            //#if MC > 1.19.2
            widget.getX() + widget.getWidth() / 2.0,
            widget.getY() + widget.getHeight() / 2.0
            //#else
            //$$ widget.x + widget.getWidth() / 2.0,
            //$$ widget.x + widget.getHeight() / 2.0
            //#endif
        );
    }

    public static void click(double x, double y) {
        click(x, y, InputConstants.MOUSE_BUTTON_LEFT);
    }

    public static void click(double x, double y, int button) {
        final Window window = Minecraft.getInstance().getWindow();
        WindowCallbackManager.cursorPosCallback.invoke(
            window.getWindow(),
            x * window.getScreenWidth() / window.getGuiScaledWidth(),
            y * window.getScreenHeight() / window.getGuiScaledHeight()
        );
        WindowCallbackManager.mouseButtonCallback.invoke(window.getWindow(), button, InputConstants.PRESS, 0);
        WindowCallbackManager.mouseButtonCallback.invoke(window.getWindow(), button, InputConstants.RELEASE, 0);
    }

    public static void press(int keycode) {
        final long window = Minecraft.getInstance().getWindow().getWindow();
        WindowCallbackManager.keyCallback.invoke(window, keycode, 0, InputConstants.PRESS, 0);
        WindowCallbackManager.keyCallback.invoke(window, keycode, 0, InputConstants.RELEASE, 0);
    }

    public static void type(String message) {
        final long window = Minecraft.getInstance().getWindow().getWindow();
        message.codePoints().forEach(cp -> WindowCallbackManager.charModsCallback.invoke(window, cp, 0));
    }

    public static void enterText(AbstractWidget widget, String text) {
        click(widget);
        type(text);
    }

    public static AbstractWidget findWidgetByRegex(@RegExp String regex) {
        return findWidgetByRegex(Pattern.compile(regex));
    }

    public static AbstractWidget findWidgetByRegex(Pattern regex) {
        return findWidgetByString(
            regex.asMatchPredicate(),
            "Could not find widget for regex \"" + regex.pattern() + "\""
        );
    }

    public static AbstractWidget findWidgetByString(Predicate<String> messageMatcher, String errorMessage) {
        return findWidget(c -> messageMatcher.test(c.getString()), errorMessage);
    }

    public static AbstractWidget findWidgetByTranslation(String translation) {
        return findWidget(
            //#if MC >= 1.19.2
            c -> c.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(translation),
            //#else
            //$$ c -> c instanceof TranslatableComponent translatable && translatable.getKey().equals(translation),
            //#endif
            "Could not find widget with translation key \"" + translation + "\""
        );
    }

    public static AbstractWidget findWidgetByComponent(Component component) {
        return findWidget(
            component::equals,
            "Could not find widget with text \"" + component.getString() + "\""
        );
    }

    public static AbstractWidget findWidget(Predicate<Component> messageMatcher, String errorMessage) {
        return findGuiElements(AbstractWidget.class)
            .filter(b -> messageMatcher.test(b.getMessage()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(errorMessage));
    }

    public static <T extends GuiEventListener> Stream<T> findGuiElements(Class<T> ofClass) {
        return allGuiElements().filter(ofClass::isInstance).map(ofClass::cast);
    }

    public static Stream<GuiEventListener> allGuiElements() {
        final Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return Stream.empty();
        }
        return streamOf(screen);
    }

    private static Stream<GuiEventListener> streamOf(GuiEventListener widget) {
        final Stream<GuiEventListener> result = Stream.of(widget);
        if (!(widget instanceof ContainerEventHandler container)) {
            return result;
        }
        return Stream.concat(result, container.children().stream().flatMap(MinecraftApi::streamOf));
    }

    public static void sleep(long millis, Runnable wakeupAction) {
        SLEEP_BUILDER.start(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Minecraft.getInstance().execute(() -> {
                    throw new RuntimeException(e);
                });
                return;
            }
            Minecraft.getInstance().execute(() -> {
                try {
                    wakeupAction.run();
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        });
    }
}
