package io.github.gaming32.worldhost.common;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.common.gui.WHGuiPlatform;
import io.github.gaming32.worldhost.common.gui.WHScreen;
import io.github.gaming32.worldhost.common.gui.screen.FriendsScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class MidnightConfig {
    private static final Pattern INTEGER_ONLY = Pattern.compile("(-?[0-9]*)");
    private static final Pattern DECIMAL_ONLY = Pattern.compile("-?(\\d+\\.?\\d*|\\d*\\.?\\d+|\\.)");
    private static final Pattern HEXADECIMAL_ONLY = Pattern.compile("(-?[#0-9a-fA-F]*)");

    private static final List<EntryInfo> ENTRIES = new ArrayList<>();

    protected static class EntryInfo {
        Field field;
        Object widget;
        int width;
        int max;
        boolean centered;
        Map.Entry<EditBox,Component> error;
        Object defaultValue;
        Object value;
        String tempValue;
        boolean inLimits = true;
        String id;
        Component name;
        int index;
        AbstractWidget colorButton;
    }

    public static final Map<String,Class<?>> CONFIG_CLASS = new HashMap<>();
    private static Path path;

    private static final Gson GSON = new GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.PRIVATE)
        .addSerializationExclusionStrategy(new HiddenAnnotationExclusionStrategy())
        .setPrettyPrinting()
        .create();

    public static void init(String modid, Class<?> config) {
        path = FabricLoader.getInstance().getConfigDir().resolve(modid + ".json");
        CONFIG_CLASS.put(modid, config);

        for (Field field : config.getFields()) {
            EntryInfo info = new EntryInfo();
            if ((field.isAnnotationPresent(Entry.class) || field.isAnnotationPresent(Comment.class)) && !field.isAnnotationPresent(Server.class) && !field.isAnnotationPresent(Hidden.class))
                initClient(modid, field, info);
            if (field.isAnnotationPresent(Comment.class)) info.centered = field.getAnnotation(Comment.class).centered();
            if (field.isAnnotationPresent(Entry.class))
                try {
                    info.defaultValue = field.get(null);
                } catch (IllegalAccessException ignored) {}
        }
        try { GSON.fromJson(Files.newBufferedReader(path), config); }
        catch (Exception e) { write(modid); }

        for (EntryInfo info : ENTRIES) {
            if (info.field.isAnnotationPresent(Entry.class))
                try {
                    info.value = info.field.get(null);
                    info.tempValue = info.value.toString();
                } catch (IllegalAccessException ignored) {}
        }
    }

    public static void write(String modid) {
        path = FabricLoader.getInstance().getConfigDir().resolve(modid + ".json");
        try {
            if (!Files.exists(path)) Files.createFile(path);
            Files.write(path, GSON.toJson(CONFIG_CLASS.get(modid).getDeclaredConstructor().newInstance()).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) public @interface Entry {
        int width() default 100;
        double min() default Double.MIN_NORMAL;
        double max() default Double.MAX_VALUE;
        String name() default "";
        boolean isColor() default false;
        boolean isSlider() default false;
        int precision() default 100;
    }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) public @interface Client {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) public @interface Server {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) public @interface Hidden {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) public @interface Comment {
        boolean centered() default false;
    }

    public static class HiddenAnnotationExclusionStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipClass(Class<?> clazz) { return false; }
        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            return fieldAttributes.getAnnotation(Entry.class) == null;
        }
    }

    static void initClient(String modid, Field field, EntryInfo info) {
        Class<?> type = field.getType();
        Entry e = field.getAnnotation(Entry.class);
        info.width = e != null ? e.width() : 0;
        info.field = field;
        info.id = modid;

        if (e != null) {
            if (!e.name().equals("")) info.name = Components.translatable(e.name());
            if (type == int.class) textField(info, Integer::parseInt, INTEGER_ONLY, (int) e.min(), (int) e.max(), true);
            else if (type == float.class) textField(info, Float::parseFloat, DECIMAL_ONLY, (float) e.min(), (float) e.max(), false);
            else if (type == double.class) textField(info, Double::parseDouble, DECIMAL_ONLY, e.min(), e.max(), false);
            else if (type == String.class || type == List.class) {
                info.max = e.max() == Double.MAX_VALUE ? Integer.MAX_VALUE : (int) e.max();
                textField(info, String::length, null, Math.min(e.min(), 0), Math.max(e.max(), 1), true);
            } else if (type == boolean.class) {
                Function<Object, Component> func = value -> Components.translatable((Boolean) value ? "gui.yes" : "gui.no").withStyle((Boolean) value ? ChatFormatting.GREEN : ChatFormatting.RED);
                info.widget = new AbstractMap.SimpleEntry<Button.OnPress, Function<Object, Component>>(button -> {
                    info.value = !(Boolean) info.value;
                    button.setMessage(func.apply(info.value));
                }, func);
            } else if (type.isEnum()) {
                List<?> values = Arrays.asList(field.getType().getEnumConstants());
                Function<Object, Component> func = value -> Components.translatable(modid + ".midnightconfig." + "enum." + type.getSimpleName() + "." + info.value.toString());
                info.widget = new AbstractMap.SimpleEntry<Button.OnPress, Function<Object, Component>>(button -> {
                    int index = values.indexOf(info.value) + 1;
                    info.value = values.get(index >= values.size() ? 0 : index);
                    button.setMessage(func.apply(info.value));
                }, func);
            }
        }
        ENTRIES.add(info);
    }

    private static void textField(EntryInfo info, Function<String,Number> f, Pattern pattern, double min, double max, boolean cast) {
        boolean isNumber = pattern != null;
        info.widget = (BiFunction<EditBox, Button, Predicate<String>>) (t, b) -> s -> {
            s = s.trim();
            if (!(s.isEmpty() || !isNumber || pattern.matcher(s).matches())) return false;

            Number value = 0;
            boolean inLimits = false;
            info.error = null;
            if (!(isNumber && s.isEmpty()) && !s.equals("-") && !s.equals(".")) {
                try { value = f.apply(s); } catch(NumberFormatException e){ return false; }
                inLimits = value.doubleValue() >= min && value.doubleValue() <= max;
                info.error = inLimits? null : new AbstractMap.SimpleEntry<>(t, Components.literal(value.doubleValue() < min ?
                    "§cMinimum " + (isNumber? "value" : "length") + (cast? " is " + (int)min : " is " + min) :
                    "§cMaximum " + (isNumber? "value" : "length") + (cast? " is " + (int)max : " is " + max)));
            }

            info.tempValue = s;
            t.setTextColor(inLimits? 0xFFFFFFFF : 0xFFFF7777);
            info.inLimits = inLimits;
            b.active = ENTRIES.stream().allMatch(e -> e.inLimits);

            if (inLimits && info.field.getType() != List.class)
                info.value = isNumber? value : s;
            else if (inLimits) {
                if (((List<String>) info.value).size() == info.index) ((List<String>) info.value).add("");
                ((List<String>) info.value).set(info.index, Arrays.stream(info.tempValue.replace("[", "").replace("]", "").split(", ")).toList().get(0));
            }

            if (info.field.getAnnotation(Entry.class).isColor()) {
                if (!s.contains("#")) s = '#' + s;
                if (!HEXADECIMAL_ONLY.matcher(s).matches()) return false;
                try {
                    info.colorButton.setMessage(Components.literal("⬛").setStyle(Style.EMPTY.withColor(Color.decode(info.tempValue).getRGB())));
                } catch (Exception ignored) {}
            }
            return true;
        };
    }

    public static class MidnightConfigScreen extends WHScreen {
        public MidnightConfigScreen(Screen parent, String modid) {
            super(Components.translatable(modid + ".midnightconfig." + "title"));
            this.parent = parent;
            this.modid = modid;
            this.translationPrefix = modid + ".midnightconfig.";
        }
        public final String translationPrefix;
        public final Screen parent;
        public final String modid;
        public MidnightConfigListWidget list;
        public boolean reload = false;

        // Real Time config update //
        @Override
        public void tick() {
            super.tick();
            for (EntryInfo info : ENTRIES) {
                try {info.field.set(null, info.value);} catch (IllegalAccessException ignored) {}
            }
            updateResetButtons();
        }
        public void updateResetButtons() {
            if (this.list != null) {
                for (ButtonEntry entry : this.list.children()) {
                    if (entry.buttons != null && entry.buttons.size() > 1 && entry.buttons.get(1) instanceof Button button) {
                        button.active = !Objects.equals(entry.info.value.toString(), entry.info.defaultValue.toString());
                    }
                }
            }
        }
        public void loadValues() {
            try { GSON.fromJson(Files.newBufferedReader(path), CONFIG_CLASS.get(modid)); }
            catch (Exception e) { write(modid); }

            for (EntryInfo info : ENTRIES) {
                if (info.field.isAnnotationPresent(Entry.class))
                    try {
                        info.value = info.field.get(null);
                        info.tempValue = info.value.toString();
                    } catch (IllegalAccessException ignored) {}
            }
        }
        public Component getTooltip(EntryInfo info) {
            return I18n.exists(translationPrefix+info.field.getName()+".tooltip") ? Components.translatable(translationPrefix+info.field.getName()+".tooltip") : Components.empty();
        }
        @Override
        public void init() {
            super.init();
            if (!reload) loadValues();

            this.addRenderableWidget(button(CommonComponents.GUI_CANCEL, button -> {
                loadValues();
                Objects.requireNonNull(minecraft).setScreen(parent);
            }).bounds(this.width / 2 - 122, this.height - 28, 80, 20).build());

            Button done = this.addRenderableWidget(button(CommonComponents.GUI_DONE, (button) -> {
                for (EntryInfo info : ENTRIES)
                    if (info.id.equals(modid)) {
                        try {
                            info.field.set(null, info.value);
                        } catch (IllegalAccessException ignored) {}
                    }
                write(modid);
                Objects.requireNonNull(minecraft).setScreen(parent);
            }).bounds(this.width / 2 + 42, this.height - 28, 80, 20).build());

            addRenderableWidget(
                button(WorldHostTexts.FRIENDS, button -> {
                        assert minecraft != null;
                        minecraft.setScreen(new FriendsScreen(this));
                    }).pos(width / 2 - 40, height - 28)
                    .width(80)
                    .build()
            );

            this.list = new MidnightConfigListWidget(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
            if (this.minecraft != null && this.minecraft.level != null) this.list.setRenderBackground(false);
            this.addWidget(this.list);
            for (EntryInfo info : ENTRIES) {
                if (info.id.equals(modid)) {
                    Component name = Objects.requireNonNullElseGet(info.name, () -> Components.translatable(translationPrefix + info.field.getName()));
                    Button resetButton = button(Components.literal("Reset").withStyle(ChatFormatting.RED), (button -> {
                        info.value = info.defaultValue;
                        info.tempValue = info.defaultValue.toString();
                        info.index = 0;
                        double scrollAmount = list.getScrollAmount();
                        this.reload = true;
                        Objects.requireNonNull(minecraft).setScreen(this);
                        list.setScrollAmount(scrollAmount);
                    })).bounds(width - 205, 0, 40, 20).build();

                    if (info.widget instanceof Map.Entry) {
                        Map.Entry<Button.OnPress, Function<Object, Component>> widget = (Map.Entry<Button.OnPress, Function<Object, Component>>) info.widget;
                        if (info.field.getType().isEnum()) widget.setValue(value -> Components.translatable(translationPrefix + "enum." + info.field.getType().getSimpleName() + "." + info.value.toString()));
                        this.list.addButton(List.of(button(widget.getValue().apply(info.value), widget.getKey()).bounds(width - 160, 0,150, 20).tooltip(getTooltip(info)).build(),resetButton), name, info);
                    } else if (info.field.getType() == List.class) {
                        if (!reload) info.index = 0;
                        EditBox widget = new EditBox(font, width - 160, 0, 150, 20, Components.empty());
                        widget.setMaxLength(info.width);
                        if (info.index < ((List<String>)info.value).size()) widget.setValue((String.valueOf(((List<String>)info.value).get(info.index))));
                        Predicate<String> processor = ((BiFunction<EditBox, Button, Predicate<String>>) info.widget).apply(widget, done);
                        widget.setFilter(processor);
                        resetButton.setWidth(20);
                        resetButton.setMessage(Components.literal("R").withStyle(ChatFormatting.RED));
                        Button cycleButton = button(Components.literal(String.valueOf(info.index)).withStyle(ChatFormatting.GOLD), (button -> {
                            ((List<String>)info.value).remove("");
                            double scrollAmount = list.getScrollAmount();
                            this.reload = true;
                            info.index = info.index + 1;
                            if (info.index > ((List<String>)info.value).size()) info.index = 0;
                            Objects.requireNonNull(minecraft).setScreen(this);
                            list.setScrollAmount(scrollAmount);
                        })).bounds(width - 185, 0, 20, 20).build();
                        platform.setTooltip(widget, getTooltip(info));
                        this.list.addButton(List.of(widget, resetButton, cycleButton), name, info);
                    } else if (info.widget != null) {
                        AbstractWidget widget;
                        Entry e = info.field.getAnnotation(Entry.class);
                        if (e.isSlider()) widget = new MidnightSliderWidget(width - 160, 0, 150, 20, Components.immutable(info.tempValue), (Double.parseDouble(info.tempValue)-e.min()) / (e.max() - e.min()), info);
                        else widget = new EditBox(font, width - 160, 0, 150, 20, null, Components.immutable(info.tempValue));
                        if (widget instanceof EditBox textField) {
                            textField.setMaxLength(info.width);
                            textField.setValue(info.tempValue);
                            Predicate<String> processor = ((BiFunction<EditBox, Button, Predicate<String>>) info.widget).apply(textField, done);
                            textField.setFilter(processor);
                        }
                        platform.setTooltip(widget, getTooltip(info));
                        if (e.isColor()) {
                            resetButton.setWidth(20);
                            resetButton.setMessage(Components.literal("R").withStyle(ChatFormatting.RED));
                            Button colorButton = button(Components.literal("⬛"), (button -> {})).bounds(width - 185, 0, 20, 20).build();
                            try {colorButton.setMessage(Components.literal("⬛").setStyle(Style.EMPTY.withColor(Color.decode(info.tempValue).getRGB())));} catch (Exception ignored) {}
                            info.colorButton = colorButton;
                            colorButton.active = false;
                            this.list.addButton(List.of(widget, resetButton, colorButton), name, info);
                        }
                        else this.list.addButton(List.of(widget, resetButton), name, info);
                    } else {
                        this.list.addButton(List.of(),name, info);
                    }
                }
                updateResetButtons();
            }

        }
        @Override
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
            this.renderBackground(matrices);
            this.list.render(matrices, mouseX, mouseY, delta);
            drawCenteredString(matrices, font, title, width / 2, 15, 0xFFFFFF);

            if (!platform.hasTooltips()) {
                for (EntryInfo info : ENTRIES) {
                    if (info.id.equals(modid)) {
                        if (list.getHoveredButton(mouseX,mouseY).isPresent()) {
                            AbstractWidget buttonWidget = list.getHoveredButton(mouseX,mouseY).get();
                            Component text = ButtonEntry.buttonsWithText.get(buttonWidget);
                            Component name = Components.translatable(this.translationPrefix + info.field.getName());
                            String key = translationPrefix + info.field.getName() + ".tooltip";

                            if (info.error != null && text.equals(name)) renderTooltip(matrices, info.error.getValue(), mouseX, mouseY);
                            else if (I18n.exists(key) && text.equals(name)) {
                                List<Component> list = new ArrayList<>();
                                for (String str : I18n.get(key).split("\n"))
                                    list.add(Components.literal(str));
                                renderComponentTooltip(matrices, list, mouseX, mouseY);
                            }
                        }
                    }
                }
            }

            super.render(matrices,mouseX,mouseY,delta);
        }
    }

    public static class MidnightConfigListWidget extends ContainerObjectSelectionList<ButtonEntry> {
        net.minecraft.client.gui.Font font;

        public MidnightConfigListWidget(Minecraft minecraftClient, int i, int j, int k, int l, int m) {
            super(minecraftClient, i, j, k, l, m);
            this.centerListVertically = false;
            font = minecraftClient.font;
        }
        @Override
        public int getScrollbarPosition() { return this.width -7; }

        public void addButton(List<AbstractWidget> buttons, Component text, EntryInfo info) {
            this.addEntry(new ButtonEntry(buttons, text, info));
        }
        @Override
        public int getRowWidth() { return 10000; }
        public Optional<AbstractWidget> getHoveredButton(double mouseX, double mouseY) {
            for (ButtonEntry buttonEntry : this.children()) {
                if (!buttonEntry.buttons.isEmpty() && buttonEntry.buttons.get(0).isMouseOver(mouseX, mouseY)) {
                    return Optional.of(buttonEntry.buttons.get(0));
                }
            }
            return Optional.empty();
        }
    }

    public static class ButtonEntry extends ContainerObjectSelectionList.Entry<ButtonEntry> {
        private static final Font font = Minecraft.getInstance().font;

        private final WHGuiPlatform platform = WorldHostCommon.getPlatform().getGuiPlatform();
        public final List<AbstractWidget> buttons;
        private final Component text;
        public final EntryInfo info;
        private final List<AbstractWidget> children = new ArrayList<>();
        public static final Map<AbstractWidget, Component> buttonsWithText = new HashMap<>();

        private ButtonEntry(List<AbstractWidget> buttons, Component text, EntryInfo info) {
            if (!buttons.isEmpty()) buttonsWithText.put(buttons.get(0),text);
            this.buttons = buttons;
            this.text = text;
            this.info = info;
            children.addAll(buttons);
        }
        @Override
        public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            buttons.forEach(b -> { platform.setY(b, y); b.render(matrices, mouseX, mouseY, tickDelta); });
            if (text != null && (!text.getString().contains("spacer") || !buttons.isEmpty())) {
                if (info.centered) font.drawShadow(matrices, text, Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2f - (font.width(text) / 2f), y + 5, 0xFFFFFF);
                else GuiComponent.drawString(matrices, font, text, 12, y + 5, 0xFFFFFF);
            }
        }
        @Override
        public @NotNull List<? extends GuiEventListener> children() {return children;}
        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {return children;}
    }

    private static class MidnightSliderWidget extends AbstractSliderButton {
        private final EntryInfo info; private final Entry e;
        public MidnightSliderWidget(int x, int y, int width, int height, Component text, double value, EntryInfo info) {
            super(x, y, width, height, text, value);
            this.e = info.field.getAnnotation(Entry.class);
            this.info = info;
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Components.immutable(info.tempValue));
        }

        @Override
        protected void applyValue() {
            if (info.field.getType() == int.class) info.value = ((Number) (e.min() + value * (e.max() - e.min()))).intValue();
            else if (info.field.getType() == double.class) info.value = Math.round((e.min() + value * (e.max() - e.min())) * (double) e.precision()) / (double) e.precision();
            else if (info.field.getType() == float.class) info.value = Math.round((e.min() + value * (e.max() - e.min())) * (float) e.precision()) / (float) e.precision();
            info.tempValue = String.valueOf(info.value);
        }
    }
}
