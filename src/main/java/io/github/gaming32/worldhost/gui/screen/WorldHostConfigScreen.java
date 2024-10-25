package io.github.gaming32.worldhost.gui.screen;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.config.option.ConfigOptions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

//#if MC < 1.20.2
//$$ import net.minecraft.client.gui.components.EditBox;
//#endif

public class WorldHostConfigScreen extends WorldHostScreen {
    private static final Component TITLE = Component.translatable("world-host.config.title");
    private static final Component UPNP = Component.literal("UPnP");

    private final Screen parent;

    private final String oldServerIp;
    private final boolean oldEnableFriends;

    public WorldHostConfigScreen(Screen parent) {
        super(TITLE);
        oldServerIp = WorldHost.CONFIG.getServerIp();
        oldEnableFriends = WorldHost.CONFIG.isEnableFriends();
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int yOffset = height / 6 - 24;

        final boolean useThreeColumns = yOffset + 24 * getRowCount(2) + 68 >= height;
        final int columns = useThreeColumns ? 3 : 2;
        final int columnLeft = useThreeColumns ? 153 : 155;
        final int columnSpacing = useThreeColumns ? 4 : 10;
        final int columnWidth = useThreeColumns ? 100 : 150;

        int columnIndex = 0;
        for (final var option : ConfigOptions.OPTIONS.values()) {
            final boolean wide = option.isWide();
            if (columnIndex == columns - 1 || wide) {
                yOffset += 24;
                columnIndex = 0;
            } else {
                columnIndex++;
            }
            option.createWidgets(
                width / 2 - columnLeft + (columnWidth + columnSpacing) * columnIndex,
                yOffset,
                !wide ? columnWidth : columnWidth * columns + columnSpacing * (columns - 1),
                20,
                font
            ).forEach(this::addRenderableWidget);
            if (wide) {
                columnIndex = columns - 1;
            }
        }
        yOffset += 48;

        addRenderableWidget(
            button(WorldHostComponents.FRIENDS, button -> {
                assert minecraft != null;
                minecraft.setScreen(new FriendsScreen(this));
            }).pos(width / 2 - 155, yOffset)
                .build()
        );

        addRenderableWidget(
            button(CommonComponents.GUI_DONE, button -> {
                assert minecraft != null;
                minecraft.setScreen(parent);
            }).pos(width / 2 + 5, yOffset)
                .build()
        );
    }

    private static int getRowCount(int columns) {
        int columnIndex = 0;
        int rowCount = 0;
        for (final var option : ConfigOptions.OPTIONS.values()) {
            final boolean wide = option.isWide();
            if (columnIndex == columns - 1 || wide) {
                rowCount++;
                columnIndex = 0;
            } else {
                columnIndex++;
            }
            if (wide) {
                columnIndex = columns - 1;
            }
        }
        return rowCount;
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

        if (WorldHost.hasScannedForUpnp()) {
            drawRightString(
                context, font, UPNP, width - 7, height - 15,
                WorldHost.upnpGateway != null ? 0x55ff55 : 0xff5555
            );
        }
    }

    @Override
    public void removed() {
        if (!WorldHost.CONFIG.getServerIp().equals(oldServerIp)) {
            WorldHost.reconnect(true, true);
        }
        if (oldEnableFriends && !WorldHost.CONFIG.isEnableFriends() && WorldHost.protoClient != null) {
            WorldHost.protoClient.closedWorld(WorldHost.CONFIG.getFriends());
        }
    }

    //#if MC < 1.20.2
    //$$ @Override
    //$$ public void tick() {
    //$$     for (final var widget : children()) {
    //$$         if (widget instanceof EditBox editBox) {
    //$$             editBox.tick();
    //$$         }
    //$$     }
    //$$ }
    //#endif
}
