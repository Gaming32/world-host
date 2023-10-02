package io.github.gaming32.worldhost.gui.screen;

import com.demonwav.mcdev.annotations.Translatable;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.WorldHostConfig;
import io.github.gaming32.worldhost.gui.widget.EnumButton;
import io.github.gaming32.worldhost.gui.widget.YesNoButton;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public class WorldHostConfigScreen extends WorldHostScreen {
    private static final Component TITLE = Components.translatable("world-host.config.title");
    private static final Component SERVER_IP = Components.translatable("world-host.config.serverIp");
    private static final Component UPNP = Components.literal("UPnP");

    private static final ConfigOption[] OPTIONS = {
        new EnumOption<>(
            "onlineStatusLocation",
            WorldHostConfig::getOnlineStatusLocation, WorldHostConfig::setOnlineStatusLocation
        ),
        new YesNoOption(
            "enableFriends",
            WorldHostConfig::isEnableFriends, WorldHostConfig::setEnableFriends
        ),
        new YesNoOption(
            "enableReconnectionToasts",
            WorldHostConfig::isEnableReconnectionToasts, WorldHostConfig::setEnableReconnectionToasts
        ),
        new YesNoOption(
            "noUPnP",
            WorldHostConfig::isNoUPnP, WorldHostConfig::setNoUPnP,
            WorldHost::scanUpnp
        ),
        new YesNoOption(
            "useShortIp",
            WorldHostConfig::isUseShortIp, WorldHostConfig::setUseShortIp
        ),
        new YesNoOption(
            "showOutdatedWorldHost",
            WorldHostConfig::isShowOutdatedWorldHost, WorldHostConfig::setShowOutdatedWorldHost
        ),
        new YesNoOption(
            "shareButton",
            WorldHostConfig::isShareButton, WorldHostConfig::setShareButton
        ),
        new YesNoOption(
            "allowFriendRequests",
            WorldHostConfig::isAllowFriendRequests, WorldHostConfig::setAllowFriendRequests
        ),
        new YesNoOption(
            "announceFriendsOnline",
            WorldHostConfig::isAnnounceFriendsOnline, WorldHostConfig::setAnnounceFriendsOnline
        ),
    };

    private final Screen parent;

    private final String oldServerIp;
    private final boolean oldEnableFriends;
    private EditBox serverIpBox;

    public WorldHostConfigScreen(Screen parent) {
        super(TITLE);
        oldServerIp = WorldHost.CONFIG.getServerIp();
        oldEnableFriends = WorldHost.CONFIG.isEnableFriends();
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        final int yOffset = height / 6;

        serverIpBox = addRenderableWidget(new EditBox(
            font, width / 2 + 5, yOffset, 150, 20, SERVER_IP
        ));
        serverIpBox.setValue(WorldHost.CONFIG.getServerIp());

        final int serverAddressResetX = 145 - font.width(SERVER_IP);
        addRenderableWidget(
            button(Components.translatable("controls.reset"), b -> serverIpBox.setValue(WorldHostConfig.DEFAULT_SERVER_IP))
                .pos(width / 2 - serverAddressResetX, yOffset)
                .width(serverAddressResetX - 5)
                .build()
        );

        for (int i = 0; i < OPTIONS.length; i++) {
            addRenderableWidget(OPTIONS[i].createButton(
                width / 2 - 155 + 160 * (i % 2),
                yOffset + 24 + 24 * (i / 2),
                150, 20
            ));
        }

        addRenderableWidget(
            button(WorldHostComponents.FRIENDS, button -> {
                assert minecraft != null;
                minecraft.setScreen(new FriendsScreen(this));
            }).pos(width / 2 - 155, yOffset + 168)
                .build()
        );

        addRenderableWidget(
            button(CommonComponents.GUI_DONE, button -> {
                assert minecraft != null;
                minecraft.setScreen(parent);
            }).pos(width / 2 + 5, yOffset + 168)
                .build()
        );
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        final String oldServerIp = serverIpBox.getValue();
        super.resize(minecraft, width, height);
        serverIpBox.setValue(oldServerIp);
    }

    @Override
    public void render(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int mouseX, int mouseY, float delta
    ) {
        whRenderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawCenteredString(context, font, title, width / 2, 15, 0xffffff);

        final int yOffset = height / 6 + 10 - font.lineHeight / 2;
        drawString(context, font, SERVER_IP, width / 2 - 150, yOffset, 0xffffff);

        drawRightString(context, font, UPNP, width - 7, height - 15, WorldHost.upnpGateway != null ? 0x55ff55 : 0xff5555);
    }

    @Override
    public void removed() {
        if (!serverIpBox.getValue().equals(oldServerIp)) {
            WorldHost.CONFIG.setServerIp(serverIpBox.getValue());
            WorldHost.saveConfig();
            WorldHost.reconnect(true, true);
        }
        if (oldEnableFriends && !WorldHost.CONFIG.isEnableFriends() && WorldHost.protoClient != null) {
            WorldHost.protoClient.closedWorld(WorldHost.CONFIG.getFriends());
        }
    }

    //#if MC < 1.20.2
    //$$ @Override
    //$$ public void tick() {
    //$$     serverIpBox.tick();
    //$$ }
    //#endif

    private interface ConfigOption {
        Button createButton(int x, int y, int width, int height);
    }

    private record YesNoOption(
        @Translatable(prefix = "world-host.config.") String name,
        Function<WorldHostConfig, Boolean> get,
        BiConsumer<WorldHostConfig, Boolean> set,
        @Nullable Runnable onSet
    ) implements ConfigOption {
        YesNoOption(
            @Translatable(prefix = "world-host.config.") String translationBase,
            Function<WorldHostConfig, Boolean> get,
            BiConsumer<WorldHostConfig, Boolean> set
        ) {
            this(translationBase, get, set, null);
        }

        @Override
        public Button createButton(int x, int y, int width, int height) {
            final String translationBase = "world-host.config." + name;
            final String tooltipKey = translationBase + ".tooltip";
            final YesNoButton button = new YesNoButton(
                x, y, width, height,
                Components.translatable(translationBase),
                I18n.exists(tooltipKey) ? Components.translatable(tooltipKey) : null,
                b -> {
                    set.accept(WorldHost.CONFIG, b.isToggled());
                    WorldHost.saveConfig();
                    if (onSet != null) {
                        onSet.run();
                    }
                }
            );
            button.setToggled(get.apply(WorldHost.CONFIG));
            return button;
        }
    }

    private record EnumOption<E extends Enum<E> & StringRepresentable>(
        @Translatable(prefix = "world-host.config.") String name,
        Function<WorldHostConfig, E> get,
        BiConsumer<WorldHostConfig, E> set,
        E... typeGetter
    ) implements ConfigOption {
        @SafeVarargs
        EnumOption {
            if (typeGetter.length != 0) {
                throw new IllegalArgumentException("typeGetter.length != 0");
            }
        }

        @Override
        public Button createButton(int x, int y, int width, int height) {
            final String translationBase = "world-host.config." + name;
            final String tooltipKey = translationBase + ".tooltip";
            @SuppressWarnings("unchecked") final EnumButton<E> button = new EnumButton<>(
                x, y, width, height,
                translationBase,
                Components.translatable(translationBase),
                I18n.exists(tooltipKey) ? Components.translatable(tooltipKey) : null,
                (Class<E>)typeGetter.getClass().getComponentType(),
                b -> {
                    set.accept(WorldHost.CONFIG, b.getValue());
                    WorldHost.saveConfig();
                }
            );
            button.setValue(get.apply(WorldHost.CONFIG));
            return button;
        }
    }
}
