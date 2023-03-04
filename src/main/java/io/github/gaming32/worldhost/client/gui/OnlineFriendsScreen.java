package io.github.gaming32.worldhost.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.GeneralUtil;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostTexts;
import io.github.gaming32.worldhost.client.FriendsListUpdate;
import io.github.gaming32.worldhost.client.WorldHostClient;
import io.github.gaming32.worldhost.mixin.client.MultiplayerServerListPingerAccessor;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.text.ParseException;
import java.util.*;

// A lot of this is based off of MultiplayerScreen and MultiplayerServerListWidget
public class OnlineFriendsScreen extends Screen implements FriendsListUpdate {
    private final Screen parent;
    private OnlineFriendsList list;
    private ButtonWidget joinButton;
    private List<Text> tooltip;

    public OnlineFriendsScreen(Screen parent) {
        super(Text.translatable("world-host.online_friends.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        assert client != null;
        client.keyboard.setRepeatEvents(true);
        if (list == null) {
            list = new OnlineFriendsList(client, width, height, 60, height - 32, 36);
            WorldHostClient.ONLINE_FRIENDS.forEach(uuid -> list.addEntry(new OnlineFriendsListEntry(uuid)));
            WorldHostClient.pingFriends();
            WorldHostClient.ONLINE_FRIEND_UPDATES.add(this);
        } else {
            list.updateSize(width, height, 60, height - 32);
        }

        addSelectableChild(list);

        joinButton = addDrawableChild(new ButtonWidget(
            width / 2 - 229, height - 28, 150, 20,
            Text.translatable("selectServer.select"),
            button -> connect()
        ));

        addDrawableChild(new ButtonWidget(
            width / 2 - 75, height - 28, 150, 20,
            Text.translatable("selectServer.refresh"),
            button -> client.setScreen(new OnlineFriendsScreen(parent))
        ));

        addDrawableChild(new ButtonWidget(
            width / 2 + 79, height - 28, 150, 20,
            ScreenTexts.CANCEL,
            button -> client.setScreen(parent)
        ));

        addDrawableChild(new ButtonWidget(
            width / 2 - 102, 32, 100, 20, WorldHostTexts.SERVERS,
            button -> {
                assert client != null;
                client.setScreen(new MultiplayerScreen(parent));
            }
        ));

        addDrawableChild(new FriendsButtonWidget(
            width / 2 + 2, 32, 100, 20,
            button -> {}
        )).active = false;

        updateButtonActivationStates();
    }

    @Override
    public void removed() {
        assert client != null;
        client.keyboard.setRepeatEvents(false);
        WorldHostClient.ONLINE_FRIEND_UPDATES.remove(this);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_5) {
            assert client != null;
            client.setScreen(new OnlineFriendsScreen(parent));
            return true;
        }
        if (list.getSelectedOrNull() != null) {
            if (keyCode != GLFW.GLFW_KEY_ENTER && keyCode != GLFW.GLFW_KEY_KP_ENTER) {
                return list.keyPressed(keyCode, scanCode, modifiers);
            }
            connect();
            return true;
        }
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        tooltip = null;
        renderBackground(matrices);
        list.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, textRenderer, title, width / 2, 15, 0xffffff);
        super.render(matrices, mouseX, mouseY, delta);
        if (tooltip != null) {
            renderTooltip(matrices, tooltip, mouseX, mouseY);
        }
    }

    public void connect() {
        final OnlineFriendsListEntry entry = list.getSelectedOrNull();
        if (entry == null) return;
        WorldHost.LOGGER.info("Requesting to join {}", entry.profile.getId());
        if (WorldHostClient.wsClient != null) {
            WorldHostClient.wsClient.requestJoin(entry.profile.getId());
        }
    }

    public void select(OnlineFriendsListEntry entry) {
        list.setSelected(entry);
        updateButtonActivationStates();
    }

    private void updateButtonActivationStates() {
        joinButton.active = list.getSelectedOrNull() != null;
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

    public class OnlineFriendsList extends AlwaysSelectedEntryListWidget<OnlineFriendsListEntry> {
        public OnlineFriendsList(MinecraftClient minecraftClient, int i, int j, int k, int l, int m) {
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
            final OnlineFriendsListEntry entry = getSelectedOrNull();
            return (entry != null && entry.keyPressed(keyCode, scanCode, modifiers)) || super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        protected int getScrollbarPositionX() {
            return super.getScrollbarPositionX() + 30;
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 85;
        }

        @Override
        protected boolean isFocused() {
            return OnlineFriendsScreen.this.getFocused() == this;
        }

        @Override
        protected int getRowTop(int index) {
            return super.getRowTop(index);
        }
    }

    public class OnlineFriendsListEntry extends AlwaysSelectedEntryListWidget.Entry<OnlineFriendsListEntry> {
        private final MinecraftClient client;
        private final ServerInfo serverInfo = new ServerInfo("", "", false);
        private GameProfile profile;

        private final Identifier iconTextureId;
        @Nullable
        private String iconUri;
        @Nullable
        private NativeImageBackedTexture icon;
        private long clickTime;

        public OnlineFriendsListEntry(UUID friendUuid) {
            client = MinecraftClient.getInstance();
            profile = new GameProfile(friendUuid, null);
            Util.getMainWorkerExecutor().execute(
                () -> profile = client.getSessionService().fillProfileProperties(profile, false)
            );
            iconTextureId = new Identifier("world-host", "servers/" + friendUuid + "/icon");
        }

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", getName());
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            updateServerInfo();

            final boolean incompatibleVersion = serverInfo.protocolVersion != SharedConstants.getGameVersion().getProtocolVersion();
            client.textRenderer.draw(matrices, serverInfo.name, x + 35, y + 1, 0xffffff);

            final List<OrderedText> lines = client.textRenderer.wrapLines(serverInfo.label, entryWidth - 34);
            for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                client.textRenderer.draw(matrices, lines.get(i), x + 35, y + 12 + 9 * i, 0x808080);
            }

            final Text sideLabel = incompatibleVersion
                ? serverInfo.version.copy().formatted(Formatting.RED)
                : serverInfo.playerCountLabel;
            final int labelWidth = client.textRenderer.getWidth(sideLabel);
            client.textRenderer.draw(matrices, sideLabel, x + entryWidth - labelWidth - 17, y + 1, 0x808080);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (incompatibleVersion) {
                RenderSystem.setShaderTexture(0, DrawableHelper.GUI_ICONS_TEXTURE);
                RenderSystem.enableBlend();
                DrawableHelper.drawTexture(matrices, x + entryWidth - 15, y, 0, 216, 10, 8, 256, 256);
                RenderSystem.disableBlend();
            }

            final String icon = serverInfo.getIcon();
            if (!Objects.equals(icon, iconUri)) {
                if (isNewIconValid(icon)) {
                    iconUri = icon;
                } else {
                    serverInfo.setIcon(null);
                }
            }

            if (icon == null) {
                RenderSystem.setShaderTexture(0, client.getSkinProvider().loadSkin(profile));
                RenderSystem.enableBlend();
                DrawableHelper.drawTexture(matrices, x, y, 32, 32, 8, 8, 8, 8, 64, 64);
                DrawableHelper.drawTexture(matrices, x, y, 32, 32, 40, 8, 8, 8, 64, 64);
                RenderSystem.disableBlend();
            } else {
                RenderSystem.setShaderTexture(0, iconTextureId);
                RenderSystem.enableBlend();
                DrawableHelper.drawTexture(matrices, x, y, 0, 0, 32, 32, 32, 32);
                RenderSystem.disableBlend();
            }

            final int relX = mouseX - x;
            final int relY = mouseY - y;
            if (relX >= entryWidth - 15 && relX <= entryWidth - 5 && relY >= 0 && relY <= 8) {
                if (incompatibleVersion) {
                    tooltip = List.of(Text.translatable("multiplayer.status.incompatible"));
                }
            } else if (relX >= entryWidth - labelWidth - 17 && relX <= entryWidth - 17 && relY >= 0 && relY <= 8) {
                tooltip = serverInfo.playerListSummary;
            }

            if (this.client.options.getTouchscreen().getValue() || hovered) {
                RenderSystem.setShaderTexture(0, new Identifier("textures/gui/server_selection.png"));
                DrawableHelper.fill(matrices, x, y, x + 32, y + 32, -1601138544);
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                if (relX < 32 && relX > 16) {
                    DrawableHelper.drawTexture(matrices, x, y, 0.0F, 32.0F, 32, 32, 256, 256);
                } else {
                    DrawableHelper.drawTexture(matrices, x, y, 0.0F, 0.0F, 32, 32, 256, 256);
                }
            }
        }

        // Mostly from MultiplayerServerListPinger.add
        private void updateServerInfo() {
            serverInfo.name = getName();
            final ServerMetadata metadata = WorldHostClient.ONLINE_FRIEND_PINGS.get(profile.getId());
            if (metadata == null) {
                serverInfo.playerCountLabel = ScreenTexts.EMPTY;
                serverInfo.label = ScreenTexts.EMPTY;
                return;
            }

            if (metadata.getDescription() != null) {
                serverInfo.label = metadata.getDescription();
            } else {
                serverInfo.label = ScreenTexts.EMPTY;
            }

            if (metadata.getVersion() != null) {
                serverInfo.version = Text.literal(metadata.getVersion().getGameVersion());
                serverInfo.protocolVersion = metadata.getVersion().getProtocolVersion();
            } else {
                serverInfo.version = Text.translatable("multiplayer.status.old");
                serverInfo.protocolVersion = 0;
            }

            serverInfo.playerListSummary = List.of();
            if (metadata.getPlayers() != null) {
                serverInfo.playerCountLabel = MultiplayerServerListPingerAccessor.createPlayerCountText(
                    metadata.getPlayers().getOnlinePlayerCount(), metadata.getPlayers().getPlayerLimit()
                );
                final List<Text> lines = new ArrayList<>();
                final GameProfile[] sampleProfiles = metadata.getPlayers().getSample();
                if (sampleProfiles != null && sampleProfiles.length > 0) {
                    for (final GameProfile sampleProfile : sampleProfiles) {
                        lines.add(Text.literal(sampleProfile.getName()));
                    }
                    if (sampleProfiles.length < metadata.getPlayers().getOnlinePlayerCount()) {
                        lines.add(Text.translatable(
                            "multiplayer.status.and_more", metadata.getPlayers().getOnlinePlayerCount() - sampleProfiles.length
                        ));
                    }
                    serverInfo.playerListSummary = lines;
                }
            } else {
                serverInfo.playerCountLabel = Text.translatable("multiplayer.status.unknown").formatted(Formatting.DARK_GRAY);
            }

            String favicon = serverInfo.getIcon();
            if (favicon != null) {
                try {
                    favicon = ServerInfo.parseFavicon(favicon);
                } catch (ParseException e) {
                    WorldHost.LOGGER.error("Invalid server icon", e);
                }
            }

            serverInfo.setIcon(favicon);
        }

        private String getName() {
            return GeneralUtil.getName(profile);
        }

        private boolean isNewIconValid(@Nullable String newIconUri) {
            if (newIconUri == null) {
                client.getTextureManager().destroyTexture(iconTextureId);
                if (icon != null && icon.getImage() != null) {
                    icon.getImage().close();
                }

                icon = null;
            } else {
                try {
                    NativeImage nativeImage = NativeImage.read(newIconUri);
                    Validate.validState(nativeImage.getWidth() == 64, "Must be 64 pixels wide");
                    Validate.validState(nativeImage.getHeight() == 64, "Must be 64 pixels high");
                    if (icon == null) {
                        icon = new NativeImageBackedTexture(nativeImage);
                    } else {
                        icon.setImage(nativeImage);
                        icon.upload();
                    }

                    client.getTextureManager().registerTexture(iconTextureId, icon);
                } catch (Throwable t) {
                    WorldHost.LOGGER.error("Invalid icon for World Host server {} ({})", serverInfo.name, profile.getId(), t);
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

            if (Util.getMeasuringTimeMs() - clickTime < 250L) {
                connect();
            }

            clickTime = Util.getMeasuringTimeMs();
            return false;
        }
    }
}
