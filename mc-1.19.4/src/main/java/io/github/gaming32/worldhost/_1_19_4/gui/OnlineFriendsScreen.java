package io.github.gaming32.worldhost._1_19_4.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost._1_19_4.mixin.client.ServerStatusPingerAccessor;
import io.github.gaming32.worldhost.common.FriendsListUpdate;
import io.github.gaming32.worldhost.common.WorldHostCommon;
import io.github.gaming32.worldhost.common.WorldHostTexts;
import io.github.gaming32.worldhost.common.gui.screen.FriendsScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;

// A lot of this is based off of MultiplayerScreen and MultiplayerServerListWidget
public class OnlineFriendsScreen extends Screen implements FriendsListUpdate {
    private final Screen parent;
    private OnlineFriendsList list;
    private Button joinButton;
    private List<Component> tooltip;

    public OnlineFriendsScreen(Screen parent) {
        super(Component.translatable("world-host.online_friends.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        assert minecraft != null;
        if (list == null) {
            list = new OnlineFriendsList(minecraft, width, height, 60, height - 64, 36);
            WorldHostCommon.ONLINE_FRIENDS.forEach(uuid -> list.addEntry(new OnlineFriendsListEntry(uuid)));
            WorldHostCommon.pingFriends();
            WorldHostCommon.ONLINE_FRIEND_UPDATES.add(this);
        } else {
            list.updateSize(width, height, 60, height - 64);
        }

        addWidget(list);

        joinButton = addRenderableWidget(
            Button.builder(Component.translatable("selectServer.select"), button -> connect())
                .pos(width / 2 - 152, height - 52)
                .build()
        );

        addRenderableWidget(
            Button.builder(
                Component.translatable("selectServer.refresh"),
                button -> minecraft.setScreen(new OnlineFriendsScreen(parent))
            ).pos(width / 2 + 2, height - 52)
                .build()
        );

        addRenderableWidget(
            Button.builder(WorldHostTexts.FRIENDS, button -> minecraft.setScreen(new FriendsScreen(this)))
                .pos(width / 2 - 152, height - 28)
                .build()
        );

        addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, button -> minecraft.setScreen(parent))
                .pos(width / 2 + 2, height - 28)
                .build()
        );

        addRenderableWidget(
            Button.builder(WorldHostTexts.SERVERS, button -> {
                assert minecraft != null;
                minecraft.setScreen(new JoinMultiplayerScreen(parent));
            }).pos(width / 2 - 102, 32)
                .width(100)
                .build()
        );

        addRenderableWidget(new FriendsButtonWidget(
            width / 2 + 2, 32, 100, 20,
            button -> {}
        )).active = false;

        updateButtonActivationStates();
    }

    @Override
    public void removed() {
        WorldHostCommon.ONLINE_FRIEND_UPDATES.remove(this);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_5) {
            assert minecraft != null;
            minecraft.setScreen(new OnlineFriendsScreen(parent));
            return true;
        }
        if (list.getSelected() != null) {
            if (keyCode != GLFW.GLFW_KEY_ENTER && keyCode != GLFW.GLFW_KEY_KP_ENTER) {
                return list.keyPressed(keyCode, scanCode, modifiers);
            }
            connect();
            return true;
        }
        return false;
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        tooltip = null;
        renderBackground(matrices);
        list.render(matrices, mouseX, mouseY, delta);
        drawCenteredString(matrices, font, title, width / 2, 15, 0xffffff);
        super.render(matrices, mouseX, mouseY, delta);
        if (tooltip != null) {
            renderComponentTooltip(matrices, tooltip, mouseX, mouseY);
        }
    }

    public void connect() {
        final OnlineFriendsListEntry entry = list.getSelected();
        if (entry == null) return;
        WorldHostCommon.LOGGER.info("Requesting to join {}", entry.profile.getId());
        if (WorldHostCommon.wsClient != null) {
            WorldHostCommon.wsClient.requestJoin(entry.profile.getId());
        }
    }

    public void select(OnlineFriendsListEntry entry) {
        list.setSelected(entry);
        updateButtonActivationStates();
    }

    private void updateButtonActivationStates() {
        joinButton.active = list.getSelected() != null;
    }

    @Override
    public void friendsListUpdate(Set<UUID> friends) {
        final Set<UUID> newFriends = new HashSet<>(friends);
        for (int i = list.children().size() - 1; i >= 0; i--) {
            final UUID uuid = list.children().get(i).profile.getId();
            if (friends.contains(uuid)) {
                newFriends.remove(uuid);
            } else {
                list.remove(i);
            }
        }

        for (final UUID friend : newFriends) {
            list.addEntry(new OnlineFriendsListEntry(friend));
        }
    }

    public class OnlineFriendsList extends ObjectSelectionList<OnlineFriendsListEntry> {
        public OnlineFriendsList(Minecraft minecraftClient, int i, int j, int k, int l, int m) {
            super(minecraftClient, i, j, k, l, m);
        }

        @Nullable
        @Override
        protected OnlineFriendsListEntry remove(int index) {
            return super.remove(index);
        }

        @Override
        protected int addEntry(OnlineFriendsListEntry entry) {
            return super.addEntry(entry);
        }

        @Override
        public void setSelected(@Nullable OnlineFriendsScreen.OnlineFriendsListEntry entry) {
            super.setSelected(entry);
            updateButtonActivationStates();
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            final OnlineFriendsListEntry entry = getSelected();
            return (entry != null && entry.keyPressed(keyCode, scanCode, modifiers)) || super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        protected int getScrollbarPosition() {
            return super.getScrollbarPosition() + 30;
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 85;
        }

        @Override
        protected int getRowTop(int index) {
            return super.getRowTop(index);
        }
    }

    public class OnlineFriendsListEntry extends ObjectSelectionList.Entry<OnlineFriendsListEntry> {
        private final Minecraft client;
        private final ServerData serverInfo = new ServerData("", "", false);
        private GameProfile profile;

        private final ResourceLocation iconTextureId;
        private byte @Nullable [] iconBytes;
        @Nullable
        private DynamicTexture icon;
        private long clickTime;

        public OnlineFriendsListEntry(UUID friendUuid) {
            client = Minecraft.getInstance();
            profile = new GameProfile(friendUuid, null);
            Util.backgroundExecutor().execute(
                () -> profile = client.getMinecraftSessionService().fillProfileProperties(profile, false)
            );
            iconTextureId = new ResourceLocation(WorldHostCommon.MOD_ID, "servers/" + friendUuid + "/icon");
        }

        @NotNull
        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", getName());
        }

        @Override
        public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            updateServerInfo();

            final boolean incompatibleVersion = serverInfo.protocol != SharedConstants.getCurrentVersion().getProtocolVersion();
            client.font.draw(matrices, serverInfo.name, x + 35, y + 1, 0xffffff);

            final List<FormattedCharSequence> lines = client.font.split(serverInfo.motd, entryWidth - 34);
            for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                client.font.draw(matrices, lines.get(i), x + 35, y + 12 + 9 * i, 0x808080);
            }

            final Component sideLabel = incompatibleVersion
                ? serverInfo.version.copy().withStyle(ChatFormatting.RED)
                : serverInfo.status;
            final int labelWidth = client.font.width(sideLabel);
            client.font.draw(matrices, sideLabel, x + entryWidth - labelWidth - 17, y + 1, 0x808080);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (incompatibleVersion) {
                RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
                RenderSystem.enableBlend();
                GuiComponent.blit(matrices, x + entryWidth - 15, y, 0, 216, 10, 8, 256, 256);
                RenderSystem.disableBlend();
            }

            final byte[] icon = serverInfo.getIconBytes();
            if (!Arrays.equals(icon, iconBytes)) {
                if (uploadServerIcon(icon)) {
                    iconBytes = icon;
                } else {
                    serverInfo.setIconBytes(null);
                }
            }

            // Mojang did "@Nullable byte[]" instead of "byte @Nullable []"
            //noinspection ConstantValue
            if (icon == null) {
                RenderSystem.setShaderTexture(0, client.getSkinManager().getInsecureSkinLocation(profile));
                RenderSystem.enableBlend();
                GuiComponent.blit(matrices, x, y, 32, 32, 8, 8, 8, 8, 64, 64);
                GuiComponent.blit(matrices, x, y, 32, 32, 40, 8, 8, 8, 64, 64);
                RenderSystem.disableBlend();
            } else {
                RenderSystem.setShaderTexture(0, iconTextureId);
                RenderSystem.enableBlend();
                GuiComponent.blit(matrices, x, y, 0, 0, 32, 32, 32, 32);
                RenderSystem.disableBlend();
            }

            final int relX = mouseX - x;
            final int relY = mouseY - y;
            if (relX >= entryWidth - 15 && relX <= entryWidth - 5 && relY >= 0 && relY <= 8) {
                if (incompatibleVersion) {
                    tooltip = List.of(Component.translatable("multiplayer.status.incompatible"));
                }
            } else if (relX >= entryWidth - labelWidth - 17 && relX <= entryWidth - 17 && relY >= 0 && relY <= 8) {
                tooltip = serverInfo.playerList;
            }

            if (this.client.options.touchscreen().get() || hovered) {
                RenderSystem.setShaderTexture(0, new ResourceLocation("textures/gui/server_selection.png"));
                GuiComponent.fill(matrices, x, y, x + 32, y + 32, -1601138544);
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                if (relX < 32 && relX > 16) {
                    GuiComponent.blit(matrices, x, y, 0.0F, 32.0F, 32, 32, 256, 256);
                } else {
                    GuiComponent.blit(matrices, x, y, 0.0F, 0.0F, 32, 32, 256, 256);
                }
            }
        }

        // Mostly from MultiplayerServerListPinger.add
        private void updateServerInfo() {
            serverInfo.name = getName();
            final ServerStatus metadata = WorldHostCommon.ONLINE_FRIEND_PINGS.get(profile.getId());
            if (metadata == null) {
                serverInfo.status = CommonComponents.EMPTY;
                serverInfo.motd = CommonComponents.EMPTY;
                return;
            }

            serverInfo.motd = metadata.description();
            metadata.version().ifPresentOrElse(version -> {
                serverInfo.version = Component.literal(version.name());
                serverInfo.protocol = version.protocol();
            }, () -> {
                serverInfo.version = Component.translatable("multiplayer.status.old");
                serverInfo.protocol = 0;
            });
            metadata.players().ifPresentOrElse(players -> {
                serverInfo.status = ServerStatusPingerAccessor.formatPlayerCount(players.online(), players.max());
                serverInfo.players = players;
                if (!players.sample().isEmpty()) {
                    final List<Component> playerList = new ArrayList<>(players.sample().size());

                    for(GameProfile gameProfile : players.sample()) {
                        playerList.add(Component.literal(gameProfile.getName()));
                    }

                    if (players.sample().size() < players.online()) {
                        playerList.add(Component.translatable(
                            "multiplayer.status.and_more",
                            players.online() - players.sample().size()
                        ));
                    }

                    serverInfo.playerList = playerList;
                } else {
                    serverInfo.playerList = List.of();
                }
            }, () -> serverInfo.status = Component.translatable("multiplayer.status.unknown").withStyle(ChatFormatting.DARK_GRAY));
            metadata.favicon().ifPresent(favicon -> {
                if (!Arrays.equals(favicon.iconBytes(), serverInfo.getIconBytes())) {
                    serverInfo.setIconBytes(favicon.iconBytes());
                }
            });
        }

        private String getName() {
            return WorldHostCommon.getName(profile);
        }

        private boolean uploadServerIcon(byte @Nullable [] newIconUri) {
            if (newIconUri == null) {
                client.getTextureManager().release(iconTextureId);
                if (icon != null && icon.getPixels() != null) {
                    icon.getPixels().close();
                }

                icon = null;
            } else {
                try {
                    NativeImage nativeImage = NativeImage.read(newIconUri);
                    Validate.validState(nativeImage.getWidth() == 64, "Must be 64 pixels wide");
                    Validate.validState(nativeImage.getHeight() == 64, "Must be 64 pixels high");
                    if (icon == null) {
                        icon = new DynamicTexture(nativeImage);
                    } else {
                        icon.setPixels(nativeImage);
                        icon.upload();
                    }

                    client.getTextureManager().register(iconTextureId, icon);
                } catch (Throwable t) {
                    WorldHostCommon.LOGGER.error("Invalid icon for World Host server {} ({})", serverInfo.name, profile.getId(), t);
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            select(this);

            final double relX = mouseX - list.getRowLeft();
            if (relX < 32.0 && relX > 16.0) {
                connect();
                return true;
            }

            if (Util.getMillis() - clickTime < 250L) {
                connect();
            }

            clickTime = Util.getMillis();
            return false;
        }
    }
}
