package io.github.gaming32.worldhost.gui.screen;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.FriendsListUpdate;
import io.github.gaming32.worldhost.ResourceLocations;
import io.github.gaming32.worldhost.SecurityLevel;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.gui.widget.EnumButton;
import io.github.gaming32.worldhost.plugin.InfoTextsCategory;
import io.github.gaming32.worldhost.gui.widget.FriendsButton;
import io.github.gaming32.worldhost.plugin.OnlineFriend;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.mixin.ServerStatusPingerAccessor;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//#if MC >= 1.20.4 && FABRIC
import de.florianmichael.viafabricplus.screen.base.ProtocolSelectionScreen;
import de.florianmichael.viafabricplus.settings.impl.GeneralSettings;
//#endif

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

//#if MC >= 1.19.4
import java.util.Arrays;
import net.minecraft.client.gui.components.Tooltip;
//#else
//$$ import java.util.Objects;
//#endif

public class OnlineFriendsScreen extends ScreenWithInfoTexts implements FriendsListUpdate {
    //#if MC >= 1.20.2
    private static final ResourceLocation INCOMPATIBLE_SPRITE = ResourceLocations.minecraft("server_list/incompatible");
    private static final ResourceLocation JOIN_HIGHLIGHTED_SPRITE = ResourceLocations.minecraft("server_list/join_highlighted");
    private static final ResourceLocation JOIN_SPRITE = ResourceLocations.minecraft("server_list/join");
    //#else
    //$$ private static final ResourceLocation GUI_ICONS_LOCATION = ResourceLocations.minecraft("textures/gui/icons.png");
    //$$ private static final ResourceLocation GUI_SERVER_SELECTION_LOCATION = ResourceLocations.minecraft("textures/gui/server_selection.png");
    //#endif

    private final Screen parent;
    private OnlineFriendsList list;
    private Button joinButton;
    private List<Component> tooltip;

    public OnlineFriendsScreen(Screen parent) {
        super(Components.translatable("world-host.online_friends.title"), InfoTextsCategory.ONLINE_FRIENDS_SCREEN);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        assert minecraft != null;
        sendRepeatEvents(true);
        if (list == null) {
            list = new OnlineFriendsList();
            WorldHost.pingFriends();
            addListEntries(WorldHost.ONLINE_FRIENDS);
            WorldHost.ONLINE_FRIEND_UPDATES.add(this);
        }
        setListSize(list, 60, getInfoTextsAdjustedBottomMargin(64));
        addWidget(list);

        joinButton = addRenderableWidget(
            button(Components.translatable("selectServer.select"), button -> connect())
                .width(152)
                .pos(width / 2 - 154, height - 54)
                .build()
        );

        addRenderableWidget(
            button(Components.translatable("selectServer.refresh"), button -> WorldHost.refreshFriendsList())
                .width(152)
                .pos(width / 2 + 2, height - 54)
                .build()
        );

        addRenderableWidget(
            button(WorldHostComponents.FRIENDS, button -> minecraft.setScreen(new FriendsScreen(this)))
                .width(152)
                .pos(width / 2 - 154, height - 30)
                .build()
        );

        addRenderableWidget(
            button(CommonComponents.GUI_CANCEL, button -> minecraft.setScreen(parent))
                .width(152)
                .pos(width / 2 + 2, height - 30)
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

        //#if MC >= 1.20.4 && FABRIC
        if (WorldHost.isModLoaded("viafabricplus")) {
            vfpInit();
        }
        //#endif
    }

    //#if MC >= 1.20.4 && FABRIC
    // Based on https://github.com/ViaVersion/ViaFabricPlus/blob/main/src/main/java/de/florianmichael/viafabricplus/injection/mixin/base/integration/MixinMultiplayerScreen.java
    private void vfpInit() {
        Button.Builder builder = Button.builder(
            Component.literal("ViaFabricPlus"),
            button -> ProtocolSelectionScreen.INSTANCE.open(this)
        ).size(98, 20);
        builder = GeneralSettings.withOrientation(
            builder, GeneralSettings.global().multiplayerScreenButtonOrientation.getIndex(), width, height
        );
        addRenderableWidget(builder.build());
    }
    //#endif

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
        if (keyCode == GLFW.GLFW_KEY_F5) {
            WorldHost.refreshFriendsList();
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
    public void render(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int mouseX, int mouseY, float delta
    ) {
        tooltip = null;
        whRenderBackground(context, mouseX, mouseY, delta);
        list.render(context, mouseX, mouseY, delta);
        drawCenteredString(context, font, title, width / 2, 15, 0xffffff);
        super.render(context, mouseX, mouseY, delta);
        if (tooltip != null) {
            renderComponentTooltip(context, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void tick() {
        super.tick();
        for (final OnlineFriendsListEntry entry : list.children()) {
            entry.updateDisplayInfo();
        }
    }

    public void connect() {
        final OnlineFriendsListEntry entry = list.getSelected();
        if (entry == null || entry.friend.unjoinableReason().isPresent()) return;
        WorldHost.LOGGER.info("Requesting to join {}", entry.friend);
        entry.friend.joinWorld(this);
    }

    public void select(OnlineFriendsListEntry entry) {
        list.setSelected(entry);
        updateButtonActivationStates();
    }

    private void updateButtonActivationStates() {
        final var selected = list.getSelected();
        if (selected != null) {
            final var unjoinableReason = selected.friend.unjoinableReason();
            //#if MC >= 1.19.4
            if (unjoinableReason.isEmpty()) {
                joinButton.active = true;
                joinButton.setTooltip(null);
            } else {
                joinButton.active = false;
                joinButton.setTooltip(Tooltip.create(unjoinableReason.get()));
            }
            //#endif
        } else {
            joinButton.active = false;
        }
    }

    @Override
    public void friendsListUpdate(Map<UUID, OnlineFriend> friends) {
        final var friendsToAdd = new LinkedHashMap<>(friends);
        for (int i = list.children().size() - 1; i >= 0; i--) {
            final UUID uuid = list.children().get(i).friend.uuid();
            if (friends.containsKey(uuid)) {
                friendsToAdd.remove(uuid);
            } else {
                list.remove(i);
            }
        }

        WorldHost.pingFriends(friends.values());
        addListEntries(friendsToAdd);
    }

    private void addListEntries(Map<?, OnlineFriend> friends) {
        friends.values()
            .stream()
            .map(OnlineFriendsListEntry::new)
            .forEach(list::addEntry);
    }

    public class OnlineFriendsList extends ObjectSelectionList<OnlineFriendsListEntry> {
        public OnlineFriendsList() {
            super(
                OnlineFriendsScreen.this.minecraft,
                //#if MC >= 1.20.3
                0, 0, 0,
                //#else
                //$$ 0, 0, 0, 0,
                //#endif
                36
            );
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
        public int getRowTop(int index) {
            return super.getRowTop(index);
        }

        public int getItemHeight() {
            return itemHeight;
        }
    }

    public class OnlineFriendsListEntry extends ObjectSelectionList.Entry<OnlineFriendsListEntry> {
        private final Minecraft minecraft;
        private final ServerData serverInfo = new ServerData(
            "", "",
            //#if MC < 1.20.2
            //$$ false
            //#else
            ServerData.Type.OTHER
            //#endif
        );
        private final OnlineFriend friend;
        private ProfileInfo profile;

        private Component displayName;
        private List<Component> unjoinableTooltip;

        private final ResourceLocation iconTextureId;
        //#if MC >= 1.19.4
        private byte[] iconData;
        //#else
        //$$ @Nullable
        //$$ private String iconData;
        //#endif
        @Nullable
        private DynamicTexture icon;

        private long clickTime;

        public OnlineFriendsListEntry(OnlineFriend friend) {
            minecraft = Minecraft.getInstance();
            this.friend = friend;
            profile = friend.fallbackProfileInfo();
            friend.profileInfo()
                .thenAccept(ready -> profile = ready)
                .exceptionally(t -> {
                    WorldHost.LOGGER.error("Failed to request profile info for {}", friend, t);
                    return null;
                });
            iconTextureId = ResourceLocations.worldHost("servers/" + friend.uuid() + "/icon");
            updateDisplayInfo();
        }

        @NotNull
        @Override
        public Component getNarration() {
            return Components.translatable("narrator.select", displayName);
        }

        @Override
        public void render(
            @NotNull
            //#if MC < 1.20.0
            //$$ PoseStack context,
            //#else
            GuiGraphics context,
            //#endif
            int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
        ) {
            final boolean incompatibleVersion = serverInfo.protocol != SharedConstants.getCurrentVersion().getProtocolVersion();
            WorldHostScreen.drawString(context, font, displayName, x + 35, y + 1, 0xffffff, false);

            final var lines = font.split(serverInfo.motd, entryWidth - 34);
            for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                WorldHostScreen.drawString(context, font, lines.get(i), x + 35, y + 12 + 9 * i, 0x808080, false);
            }

            final Component sideLabel = incompatibleVersion
                ? serverInfo.version.copy().withStyle(ChatFormatting.RED)
                : serverInfo.status;
            final int labelWidth = font.width(sideLabel);
            WorldHostScreen.drawString(context, font, sideLabel, x + entryWidth - labelWidth - 17, y + 1, 0x808080, false);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (incompatibleVersion) {
                RenderSystem.enableBlend();
                //#if MC >= 1.20.2
                context.blitSprite(INCOMPATIBLE_SPRITE, x + entryWidth - 15, y, 10, 8);
                //#else
                //$$ blit(context, GUI_ICONS_LOCATION, x + entryWidth - 15, y, 0, 216, 10, 8, 256, 256);
                //#endif
                RenderSystem.disableBlend();
            }

            //#if MC >= 1.19.4
            final byte[] icon = serverInfo.getIconBytes();
            if (!Arrays.equals(icon, iconData)) {
            //#else
            //$$ final String icon = serverInfo.getIconB64();
            //$$ if (!Objects.equals(icon, iconData)) {
            //#endif
                if (uploadServerIcon(icon)) {
                    iconData = icon;
                } else {
                    //#if MC >= 1.19.4
                    serverInfo.setIconBytes(null);
                    //#else
                    //$$ serverInfo.setIconB64(null);
                    //#endif
                }
            }

            //noinspection ConstantValue
            if (icon == null) {
                profile.iconRenderer().draw(context, x, y, 32, 32);
                RenderSystem.disableBlend();
            } else {
                RenderSystem.setShaderTexture(0, iconTextureId);
                RenderSystem.enableBlend();
                blit(context, iconTextureId, x, y, 0, 0, 32, 32, 32, 32);
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
            } else if (unjoinableTooltip != null && hovered) {
                tooltip = unjoinableTooltip;
            }

            //#if MC >= 1.19.0
            final boolean touchscreen = minecraft.options.touchscreen().get();
            //#else
            //$$ final boolean touchscreen = minecraft.options.touchscreen;
            //#endif
            if (unjoinableTooltip == null && (touchscreen || hovered)) {
                fill(context, x, y, x + 32, y + 32, 0xa0909090);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                //#if MC >= 1.20.2
                if (relX < 32 && relX > 16) {
                    context.blitSprite(JOIN_HIGHLIGHTED_SPRITE, x, y, 32, 32);
                } else {
                    context.blitSprite(JOIN_SPRITE, x, y, 32, 32);
                }
                //#else
                //$$ if (relX < 32 && relX > 16) {
                //$$     blit(context, GUI_SERVER_SELECTION_LOCATION, x, y, 0.0F, 32.0F, 32, 32, 256, 256);
                //$$ } else {
                //$$     blit(context, GUI_SERVER_SELECTION_LOCATION, x, y, 0.0F, 0.0F, 32, 32, 256, 256);
                //$$ }
                //#endif
            }
        }

        private void updateDisplayInfo() {
            serverInfo.name = profile.name();
            updateNameAndTooltip();
            final var metadata = WorldHost.ONLINE_FRIEND_PINGS.get(friend.uuid());
            if (metadata == null) {
                serverInfo.status = Components.EMPTY;
                serverInfo.motd = Components.EMPTY;
                return;
            }

            //#if MC >= 1.19.4
            serverInfo.motd = metadata.description();
            metadata.version().ifPresentOrElse(version -> {
                serverInfo.version = Components.literal(version.name());
                serverInfo.protocol = version.protocol();
            }, () -> {
                serverInfo.version = Components.translatable("multiplayer.status.old");
                serverInfo.protocol = 0;
            });
            metadata.players().ifPresentOrElse(players -> {
                serverInfo.status = ServerStatusPingerAccessor.callFormatPlayerCount(players.online(), players.max());
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
            //$$     serverInfo.status = ServerStatusPingerAccessor.callFormatPlayerCount(
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

        private void updateNameAndTooltip() {
            final var security = friend.security();
            final var unjoinableReason = friend.unjoinableReason();

            final MutableComponent newDisplayName;
            if (security == SecurityLevel.SECURE) {
                newDisplayName = Components.literal(profile.name());
            } else {
                newDisplayName = Components.translatable(
                    "world-host.world_with_security",
                    profile.name(),
                    EnumButton.getTranslation("world-host.config.requiredSecurityLevel", security)
                );
            }
            if (unjoinableReason.isPresent()) {
                newDisplayName.withStyle(ChatFormatting.RED);
            }
            displayName = newDisplayName;

            unjoinableTooltip = unjoinableReason.map(List::of).orElse(null);
        }

        private boolean uploadServerIcon(
            //#if MC >= 1.19.4
            byte[] newIconData
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
                    //#if MC >= 1.19.4
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
                    WorldHost.LOGGER.error("Invalid icon for World Host server {} ({})", serverInfo.name, friend.uuid(), t);
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            select(this);

            final double relX = mouseX - OnlineFriendsScreen.this.list.getRowLeft();
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
