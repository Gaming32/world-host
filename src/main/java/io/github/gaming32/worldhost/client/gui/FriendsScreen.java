package io.github.gaming32.worldhost.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import eu.midnightdust.lib.config.MidnightConfig;
import io.github.gaming32.worldhost.GeneralUtil;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostData;
import io.github.gaming32.worldhost.WorldHostTexts;
import io.github.gaming32.worldhost.client.WorldHostClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public class FriendsScreen extends Screen {
    private static final Text ADD_SILENTLY_TEXT = Text.translatable("world-host.friends.add_silently");

    private final Screen parent;
    private ButtonWidget removeButton;
    private FriendsList list;

    public FriendsScreen(Screen parent) {
        super(WorldHostTexts.FRIENDS);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        if (list == null) {
            list = addSelectableChild(new FriendsList(width, height, 32, height - 32, 36));
            if (client != null && client.world != null) {
                list.setRenderBackground(false);
            }
        } else {
            list.updateSize(width, height, 32, height - 32);
        }

        addDrawableChild(new ButtonWidget(
            width / 2 - 306, height - 28, 150, 20, Text.translatable("world-host.add_friend"),
            button -> {
                assert client != null;
                client.setScreen(new AddFriendScreen(this, ADD_SILENTLY_TEXT, profile -> {
                    addFriend(profile);
                    if (WorldHostClient.wsClient != null) {
                        WorldHostClient.wsClient.friendRequest(profile.getId());
                    }
                }));
            }
        ));

        addDrawableChild(new ButtonWidget(
            width / 2 - 152, height - 28, 150, 20, ADD_SILENTLY_TEXT,
            button -> {
                assert client != null;
                client.setScreen(new AddFriendScreen(this, ADD_SILENTLY_TEXT, this::addFriend));
            }
        ));

        removeButton = addDrawableChild(new ButtonWidget(width / 2 + 2, height - 28, 150, 20, Text.translatable("world-host.friends.remove"), button -> {
            if (list.getSelectedOrNull() != null) {
                list.getSelectedOrNull().maybeRemove();
            }
        }));
        removeButton.active = false;

        addDrawableChild(new ButtonWidget(
            width / 2 + 156, height - 28, 150, 20, ScreenTexts.DONE,
            button -> {
                assert client != null;
                client.setScreen(parent);
            }
        ));

        list.updateEntries();
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    private void addFriend(GameProfile profile) {
        WorldHostData.friends.add(profile.getId());
        MidnightConfig.write(WorldHost.MOD_ID);
        list.addEntry(new FriendsEntry(profile));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        list.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, textRenderer, title, width / 2, 15, 0xffffff);
        super.render(matrices, mouseX, mouseY, delta);
    }

    public class FriendsList extends AlwaysSelectedEntryListWidget<FriendsEntry> {
        public FriendsList(int i, int j, int k, int l, int m) {
            super(FriendsScreen.this.client, i, j, k, l, m);
        }

        @Override
        public void setSelected(@Nullable FriendsScreen.FriendsEntry entry) {
            super.setSelected(entry);
            removeButton.active = entry != null;
        }

        private void updateEntries() {
            clearEntries();
            WorldHostData.friends.forEach(uuid -> addEntry(new FriendsEntry(new GameProfile(uuid, null))));
        }

        @Override
        public int addEntry(FriendsEntry entry) {
            return super.addEntry(entry);
        }
    }

    public class FriendsEntry extends AlwaysSelectedEntryListWidget.Entry<FriendsEntry> {
        private final MinecraftClient client;
        private GameProfile profile;

        public FriendsEntry(GameProfile profile) {
            client = MinecraftClient.getInstance();
            this.profile = profile;
            Util.getMainWorkerExecutor().execute(
                () -> this.profile = client.getSessionService().fillProfileProperties(profile, false)
            );
        }

        @Override
        public Text getNarration() {
            return Text.of(getName());
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            final Identifier skinTexture = client.getSkinProvider().loadSkin(profile);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.setShaderTexture(0, skinTexture);
            RenderSystem.enableBlend();
            DrawableHelper.drawTexture(matrices, x, y, 32, 32, 8, 8, 8, 8, 64, 64);
            DrawableHelper.drawTexture(matrices, x, y, 32, 32, 40, 8, 8, 8, 64, 64);
            RenderSystem.disableBlend();
            drawCenteredText(matrices, client.textRenderer, getName(), x + 110, y + 16 - client.textRenderer.fontHeight / 2, 0xffffff);
        }

        public String getName() {
            return GeneralUtil.getName(profile);
        }

        public void maybeRemove() {
            assert client != null;
            client.setScreen(new ConfirmScreen(
                yes -> {
                    if (yes) {
                        WorldHostData.friends.remove(profile.getId());
                        MidnightConfig.write(WorldHost.MOD_ID);
                        list.updateEntries();
                    }
                    client.setScreen(FriendsScreen.this);
                },
                Text.translatable("world-host.friends.remove.title"),
                Text.translatable("world-host.friends.remove.message")
            ));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            list.setSelected(this);
            return false;
        }
    }
}
