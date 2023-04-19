package io.github.gaming32.worldhost.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FriendsScreen extends WorldHostScreen {
    private static final Component ADD_FRIEND_TEXT = Components.translatable("world-host.add_friend");
    private static final Component ADD_SILENTLY_TEXT = Components.translatable("world-host.friends.add_silently");

    private final Screen parent;
    private Button removeButton;
    private FriendsList list;

    public FriendsScreen(Screen parent) {
        super(WorldHostComponents.FRIENDS);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        if (list == null) {
            list = addWidget(new FriendsList(width, height, 32, height - 64, 36));
            if (minecraft != null && minecraft.level != null) {
                list.setRenderBackground(false);
            }
        } else {
            list.updateSize(width, height, 32, height - 64);
        }

        addRenderableWidget(
            button(ADD_FRIEND_TEXT, button -> {
                assert minecraft != null;
                minecraft.setScreen(new AddFriendScreen(this, ADD_FRIEND_TEXT, profile -> {
                    addFriend(profile);
//                    if (WorldHost.wsClient != null) {
//                        WorldHost.wsClient.friendRequest(profile.getId());
//                    }
                }));
            }).pos(width / 2 - 152, height - 52)
                .build()
        );

        addRenderableWidget(
            button(ADD_SILENTLY_TEXT, button -> {
                assert minecraft != null;
                minecraft.setScreen(new AddFriendScreen(this, ADD_SILENTLY_TEXT, this::addFriend));
            }).pos(width / 2 - 152, height - 28)
                .build()
        );

        removeButton = addRenderableWidget(
            button(Components.translatable("world-host.friends.remove"), button -> {
                if (list.getSelected() != null) {
                    list.getSelected().maybeRemove();
                }
            }).pos(width / 2 + 2, height - 52)
                .build()
        );
        removeButton.active = false;

        addRenderableWidget(
            button(CommonComponents.GUI_DONE, button -> {
                assert minecraft != null;
                minecraft.setScreen(parent);
            }).pos(width / 2 + 2, height - 28)
                .build()
        );

        list.updateEntries();
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    private void addFriend(GameProfile profile) {
        WorldHost.CONFIG.getFriends().add(profile.getId());
        WorldHost.saveConfig();
        list.addEntry(new FriendsEntry(profile));
    }

    @Override
    public void render(@NotNull PoseStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        list.render(matrices, mouseX, mouseY, delta);
        drawCenteredString(matrices, font, title, width / 2, 15, 0xffffff);
        super.render(matrices, mouseX, mouseY, delta);
    }

    public class FriendsList extends ObjectSelectionList<FriendsEntry> {
        public FriendsList(int i, int j, int k, int l, int m) {
            //noinspection DataFlowIssue
            super(FriendsScreen.this.minecraft, i, j, k, l, m);
        }

        @Override
        public void setSelected(@Nullable FriendsEntry entry) {
            super.setSelected(entry);
            removeButton.active = entry != null;
        }

        private void updateEntries() {
            clearEntries();
            WorldHost.CONFIG.getFriends().forEach(uuid -> addEntry(new FriendsEntry(new GameProfile(uuid, null))));
        }

        @Override
        public int addEntry(@NotNull FriendsEntry entry) {
            return super.addEntry(entry);
        }
    }

    public class FriendsEntry extends ObjectSelectionList.Entry<FriendsEntry> {
        private final Minecraft client;
        private GameProfile profile;

        public FriendsEntry(GameProfile profile) {
            client = Minecraft.getInstance();
            this.profile = profile;
            Util.backgroundExecutor().execute(
                () -> this.profile = client.getMinecraftSessionService().fillProfileProperties(profile, false)
            );
        }

        @NotNull
        @Override
        public Component getNarration() {
            return Components.immutable(getName());
        }

        @Override
        public void render(@NotNull PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            final ResourceLocation skinTexture = client.getSkinManager().getInsecureSkinLocation(profile);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.setShaderTexture(0, skinTexture);
            RenderSystem.enableBlend();
            GuiComponent.blit(matrices, x, y, 32, 32, 8, 8, 8, 8, 64, 64);
            GuiComponent.blit(matrices, x, y, 32, 32, 40, 8, 8, 8, 64, 64);
            RenderSystem.disableBlend();
            drawCenteredString(matrices, client.font, getName(), x + 110, y + 16 - client.font.lineHeight / 2, 0xffffff);
        }

        public String getName() {
            return WorldHost.getName(profile);
        }

        public void maybeRemove() {
            assert client != null;
            client.setScreen(new ConfirmScreen(
                yes -> {
                    if (yes) {
                        WorldHost.CONFIG.getFriends().remove(profile.getId());
                        WorldHost.saveConfig();
                        list.updateEntries();
                    }
                    client.setScreen(FriendsScreen.this);
                },
                Components.translatable("world-host.friends.remove.title"),
                Components.translatable("world-host.friends.remove.message")
            ));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            list.setSelected(this);
            return false;
        }
    }
}