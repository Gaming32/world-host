package io.github.gaming32.worldhost.gui.screen;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.FriendsListUpdate;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.gui.widget.FriendsButton;
import io.github.gaming32.worldhost.mixin.ServerStatusPingerAccessor;
import io.github.gaming32.worldhost.versions.Components;
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
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class OnlineFriendsScreen extends WorldHostScreen implements FriendsListUpdate {
    private final Screen parent;
    private OnlineFriendsList list;
    private Button joinButton;
    private List<Component> tooltip;

    public OnlineFriendsScreen(Screen parent) {
        super(Components.translatable("world-host.online_friends.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        assert minecraft != null;
        sendRepeatEvents(true);
        if (list == null) {
            list = new OnlineFriendsList(minecraft, width, height, 60, height - 64, 36);
            WorldHost.ONLINE_FRIENDS.forEach((u, c) -> list.addEntry(new OnlineFriendsListEntry(u, c)));
            WorldHost.pingFriends();
            WorldHost.ONLINE_FRIEND_UPDATES.add(this);
        } else {
            list.updateSize(width, height, 60, height - 64);
        }
        addWidget(list);

        joinButton = addRenderableWidget(
            button(Components.translatable("selectServer.select"), button -> connect())
                .pos(width / 2 - 152, height - 52)
                .build()
        );

        addRenderableWidget(
            button(
                Components.translatable("selectServer.refresh"),
                button -> minecraft.setScreen(new OnlineFriendsScreen(parent))
            ).pos(width / 2 + 2, height - 52)
                .build()
        );

        addRenderableWidget(
            button(WorldHostComponents.FRIENDS, button -> minecraft.setScreen(new FriendsScreen(this)))
                .pos(width / 2 - 152, height - 28)
                .build()
        );

        addRenderableWidget(
            button(CommonComponents.GUI_CANCEL, button -> minecraft.setScreen(parent))
                .pos(width / 2 + 2, height - 28)
                .build()
        );

        addRenderableWidget(
            button(WorldHostComponents.SERVERS, button -> {
                assert minecraft != null;
                minecraft.setScreen(new JoinMultiplayerScreen(parent));
            }).pos(width / 2 - 102, 32)
                .width(100)
                .build()
        );

        addRenderableWidget(new FriendsButton(
            width / 2 + 2, 32, 100, 20,
            button -> {}
        )).active = false;

        updateButtonActivationStates();
    }

    @Override
    public void removed() {
        assert minecraft != null;
        sendRepeatEvents(false);
        WorldHost.ONLINE_FRIEND_UPDATES.remove(this);
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
    public void render(@NotNull PoseStack matrices, int mouseX, int mouseY, float delta) {
        tooltip = null;
        renderBackground(matrices);
        list.render(matrices, mouseX, mouseY, delta);
        drawCenteredString(matrices, font, title, width / 2, 15, 0xffffff);
        super.render(matrices, mouseX, mouseY, delta);
        if (tooltip != null) {
            //#if MC > 11601
            renderComponentTooltip
            //#else
            //$$ renderTooltip
            //#endif
                (matrices, tooltip, mouseX, mouseY);
        }
    }

    public void connect() {
        final OnlineFriendsListEntry entry = list.getSelected();
        if (entry == null) return;
        WorldHost.LOGGER.info("Requesting to join {}", entry.profile.getId());
        if (WorldHost.protoClient != null) {
            WorldHost.join(entry.connectionId, this);
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
    public void friendsListUpdate(Map<UUID, Long> friends) {
        final var newFriends = new LinkedHashMap<>(friends);
        for (int i = list.children().size() - 1; i >= 0; i--) {
            final UUID uuid = list.children().get(i).profile.getId();
            if (friends.containsKey(uuid)) {
                newFriends.remove(uuid);
            } else {
                list.remove(i);
            }
        }

        for (final var friend : newFriends.entrySet()) {
            list.addEntry(new OnlineFriendsListEntry(friend.getKey(), friend.getValue()));
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
        protected int addEntry(@NotNull OnlineFriendsListEntry entry) {
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
        private final Minecraft minecraft;
        private final ServerData serverInfo = new ServerData("", "", false);
        private final long connectionId;
        private GameProfile profile;

        private final ResourceLocation iconTextureId;
        //#if MC >= 11904
        private byte @Nullable [] iconData;
        //#else
        //$$ @Nullable
        //$$ private String iconData;
        //#endif
        @Nullable
        private DynamicTexture icon;
        private long clickTime;

        public OnlineFriendsListEntry(UUID friendUuid, long connectionId) {
            minecraft = Minecraft.getInstance();
            this.connectionId = connectionId;
            profile = new GameProfile(friendUuid, null);
            Util.backgroundExecutor().execute(
                () -> profile = minecraft.getMinecraftSessionService().fillProfileProperties(profile, false)
            );
            iconTextureId = new ResourceLocation(WorldHost.MOD_ID, "servers/" + friendUuid + "/icon");
        }

        //#if MC >= 11802
        @NotNull
        @Override
        public Component getNarration() {
            return Components.translatable("narrator.select", getName());
        }
        //#endif

        @Override
        public void render(@NotNull PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            updateServerInfo();

            final boolean incompatibleVersion = serverInfo.protocol != SharedConstants.getCurrentVersion().getProtocolVersion();
            minecraft.font.draw(matrices, serverInfo.name, x + 35, y + 1, 0xffffff);

            final var lines = minecraft.font.split(serverInfo.motd, entryWidth - 34);
            for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                minecraft.font.draw(matrices, lines.get(i), x + 35, y + 12 + 9 * i, 0x808080);
            }

            final Component sideLabel = incompatibleVersion
                ? serverInfo.version.copy().withStyle(ChatFormatting.RED)
                : serverInfo.status;
            final int labelWidth = minecraft.font.width(sideLabel);
            minecraft.font.draw(matrices, sideLabel, x + entryWidth - labelWidth - 17, y + 1, 0x808080);

            WorldHost.positionTexShader();
            WorldHost.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (incompatibleVersion) {
                WorldHost.texture(GuiComponent.GUI_ICONS_LOCATION);
                RenderSystem.enableBlend();
                GuiComponent.blit(matrices, x + entryWidth - 15, y, 0, 216, 10, 8, 256, 256);
                RenderSystem.disableBlend();
            }

            //#if MC >= 11904
            final byte @Nullable [] icon = serverInfo.getIconBytes();
            if (!Arrays.equals(icon, iconData)) {
            //#else
            //$$ final String icon = serverInfo.getIconB64();
            //$$ if (!Objects.equals(icon, iconData)) {
            //#endif
                if (uploadServerIcon(icon)) {
                    iconData = icon;
                } else {
                    //#if MC >= 11904
                    // Mojang did "@Nullable byte[]" instead of "byte @Nullable []"
                    //noinspection DataFlowIssue
                    serverInfo.setIconBytes(null);
                    //#else
                    //$$ serverInfo.setIconB64(null);
                    //#endif
                }
            }

            // Since when does a value marked as @Nullable never satisfy == null?
            //noinspection ConstantValue
            if (icon == null) {
                WorldHost.texture(WorldHost.getInsecureSkinLocation(profile));
                RenderSystem.enableBlend();
                GuiComponent.blit(matrices, x, y, 32, 32, 8, 8, 8, 8, 64, 64);
                GuiComponent.blit(matrices, x, y, 32, 32, 40, 8, 8, 8, 64, 64);
                RenderSystem.disableBlend();
            } else {
                WorldHost.texture(iconTextureId);
                RenderSystem.enableBlend();
                GuiComponent.blit(matrices, x, y, 0, 0, 32, 32, 32, 32);
                RenderSystem.disableBlend();
            }

            final int relX = mouseX - x;
            final int relY = mouseY - y;
            if (relX >= entryWidth - 15 && relX <= entryWidth - 5 && relY >= 0 && relY <= 8) {
                if (incompatibleVersion) {
                    tooltip = List.of(Components.translatable("multiplayer.status.incompatible"));
                }
            } else if (relX >= entryWidth - labelWidth - 17 && relX <= entryWidth - 17 && relY >= 0 && relY <= 8) {
                tooltip = serverInfo.playerList;
            }

            if (
                minecraft.options.touchscreen
                    //#if MC >= 11900
                    ().get()
                    //#endif
                    || hovered
            ) {
                WorldHost.texture(new ResourceLocation("textures/gui/server_selection.png"));
                GuiComponent.fill(matrices, x, y, x + 32, y + 32, 0xa0909090);
                WorldHost.positionTexShader();
                WorldHost.color(1.0F, 1.0F, 1.0F, 1.0F);
                if (relX < 32 && relX > 16) {
                    GuiComponent.blit(matrices, x, y, 0.0F, 32.0F, 32, 32, 256, 256);
                } else {
                    GuiComponent.blit(matrices, x, y, 0.0F, 0.0F, 32, 32, 256, 256);
                }
            }
        }

        private void updateServerInfo() {
            serverInfo.name = getName();
            final ServerStatus metadata = WorldHost.ONLINE_FRIEND_PINGS.get(profile.getId());
            if (metadata == null) {
                serverInfo.status = Components.EMPTY;
                serverInfo.motd = Components.EMPTY;
                return;
            }

            //#if MC >= 11904
            serverInfo.motd = metadata.description();
            metadata.version().ifPresentOrElse(version -> {
                serverInfo.version = Components.literal(version.name());
                serverInfo.protocol = version.protocol();
            }, () -> {
                serverInfo.version = Components.translatable("multiplayer.status.old");
                serverInfo.protocol = 0;
            });
            metadata.players().ifPresentOrElse(players -> {
                serverInfo.status = ServerStatusPingerAccessor.formatPlayerCount(players.online(), players.max());
                serverInfo.players = players;
                if (!players.sample().isEmpty()) {
                    final List<Component> playerList = new ArrayList<>(players.sample().size());

                    for(GameProfile gameProfile : players.sample()) {
                        playerList.add(Components.literal(gameProfile.getName()));
                    }

                    if (players.sample().size() < players.online()) {
                        playerList.add(Components.translatable(
                            "multiplayer.status.and_more",
                            players.online() - players.sample().size()
                        ));
                    }

                    serverInfo.playerList = playerList;
                } else {
                    serverInfo.playerList = List.of();
                }
            }, () -> serverInfo.status = Components.translatable("multiplayer.status.unknown").withStyle(ChatFormatting.DARK_GRAY));
            metadata.favicon().ifPresent(favicon -> {
                if (!Arrays.equals(favicon.iconBytes(), serverInfo.getIconBytes())) {
                    serverInfo.setIconBytes(favicon.iconBytes());
                }
            });
            //#else
            //$$ if (metadata.getDescription() != null) {
            //$$     serverInfo.motd = metadata.getDescription();
            //$$ } else {
            //$$     serverInfo.motd = Components.EMPTY;
            //$$ }
            //$$
            //$$ if (metadata.getVersion() != null) {
            //$$     serverInfo.version = Components.literal(metadata.getVersion().getName());
            //$$     serverInfo.protocol = metadata.getVersion().getProtocol();
            //$$ } else {
            //$$     serverInfo.version = Components.translatable("multiplayer.status.old");
            //$$     serverInfo.protocol = 0;
            //$$ }
            //$$
            //$$ serverInfo.playerList = List.of();
            //$$ if (metadata.getPlayers() != null) {
            //$$     serverInfo.status = ServerStatusPingerAccessor.formatPlayerCount(
            //$$         metadata.getPlayers().getNumPlayers(), metadata.getPlayers().getMaxPlayers()
            //$$     );
            //$$     final List<Component> lines = new ArrayList<>();
            //$$     final GameProfile[] sampleProfiles = metadata.getPlayers().getSample();
            //$$     if (sampleProfiles != null && sampleProfiles.length > 0) {
            //$$         for (final GameProfile sampleProfile : sampleProfiles) {
            //$$             lines.add(Components.literal(sampleProfile.getName()));
            //$$         }
            //$$         if (sampleProfiles.length < metadata.getPlayers().getNumPlayers()) {
            //$$             lines.add(Components.translatable(
            //$$                 "multiplayer.status.and_more", metadata.getPlayers().getNumPlayers() - sampleProfiles.length
            //$$             ));
            //$$         }
            //$$         serverInfo.playerList = lines;
            //$$     }
            //$$ } else {
            //$$     serverInfo.status = Components.translatable("multiplayer.status.unknown").withStyle(ChatFormatting.DARK_GRAY);
            //$$ }
            //$$
            //$$ String favicon = serverInfo.getIconB64();
            //$$ if (favicon != null) {
            //$$     if (favicon.startsWith("data:image/png;base64,")) {
            //$$         serverInfo.setIconB64(favicon.substring("data:image/png;base64,".length()));
            //$$     } else {
            //$$         WorldHost.LOGGER.error("Invalid server icon");
            //$$     }
            //$$ } else {
            //$$     serverInfo.setIconB64(null);
            //$$ }
            //#endif
        }

        private String getName() {
            return WorldHost.getName(profile);
        }

        private boolean uploadServerIcon(
            //#if MC >= 11904
            byte @Nullable [] newIconData
            //#else
            //$$ @Nullable String newIconData
            //#endif
        ) {
            if (newIconData == null) {
                minecraft.getTextureManager().release(iconTextureId);
                if (icon != null && icon.getPixels() != null) {
                    icon.getPixels().close();
                }

                icon = null;
            } else {
                try {
                    //#if MC >= 11904
                    NativeImage image = NativeImage.read(newIconData);
                    //#else
                    //$$ NativeImage image = NativeImage.fromBase64(newIconData);
                    //#endif
                    Validate.validState(image.getWidth() == 64, "Must be 64 pixels wide");
                    Validate.validState(image.getHeight() == 64, "Must be 64 pixels high");
                    if (icon == null) {
                        icon = new DynamicTexture(image);
                    } else {
                        icon.setPixels(image);
                        icon.upload();
                    }

                    minecraft.getTextureManager().register(iconTextureId, icon);
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

            //#if MC > 11601
            final double relX = mouseX - list.getRowLeft();
            if (relX < 32.0 && relX > 16.0) {
                connect();
                return true;
            }
            //#endif

            if (Util.getMillis() - clickTime < 250L) {
                connect();
            }

            clickTime = Util.getMillis();
            return false;
        }
    }
}
