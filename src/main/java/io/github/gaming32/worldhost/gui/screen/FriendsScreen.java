package io.github.gaming32.worldhost.gui.screen;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;

import java.util.Collections;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public class FriendsScreen extends WorldHostScreen {
    public static final Component ADD_FRIEND_TEXT = Components.translatable("world-host.add_friend");
    private static final Component ADD_SILENTLY_TEXT = Components.translatable("world-host.friends.add_silently");
    private static final Component BEDROCK_FRIENDS_TEXT = Components.translatable(
        "world-host.friends.bedrock_notice",
        Components.translatable("world-host.friends.bedrock_notice.link")
            .withStyle(s -> s
                .applyFormat(ChatFormatting.UNDERLINE)
                .withColor(ChatFormatting.BLUE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://account.xbox.com/Profile"))
            )
    );

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
            list = addWidget(new FriendsList(width, height, 32, height - (WorldHost.BEDROCK_SUPPORT ? 80 : 64), 36));
            //#if MC > 1.16.1
            if (minecraft != null && minecraft.level != null) {
                list.setRenderBackground(false);
            }
            //#endif
        } else {
            list.updateSize(width, height, 32, height - (WorldHost.BEDROCK_SUPPORT ? 80 : 64));
        }

        addRenderableWidget(
            button(ADD_FRIEND_TEXT, button -> {
                assert minecraft != null;
                minecraft.setScreen(new AddFriendScreen(this, ADD_FRIEND_TEXT, null, profile -> {
                    addFriendAndUpdate(profile);
                    if (WorldHost.protoClient != null) {
                        WorldHost.protoClient.friendRequest(profile.getId());
                    }
                }));
            }).pos(width / 2 - 152, height - 52)
                .tooltip(Components.translatable("world-host.add_friend.tooltip"))
                .build()
        );

        addRenderableWidget(
            button(ADD_SILENTLY_TEXT, button -> {
                assert minecraft != null;
                minecraft.setScreen(new AddFriendScreen(this, ADD_SILENTLY_TEXT, null, this::addFriendAndUpdate));
            }).pos(width / 2 - 152, height - 28)
                .tooltip(Components.translatable("world-host.friends.add_silently.tooltip"))
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

    private void addFriendAndUpdate(GameProfile profile) {
        addFriend(profile);
        list.addEntry(new FriendsEntry(profile));
    }

    public static void addFriend(GameProfile profile) {
        WorldHost.CONFIG.getFriends().add(profile.getId());
        WorldHost.saveConfig();
        final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null && server.isPublished() && WorldHost.protoClient != null) {
            WorldHost.protoClient.publishedWorld(Collections.singleton(profile.getId()));
        }
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
        renderBackground(context);
        list.render(context, mouseX, mouseY, delta);
        drawCenteredString(context, font, title, width / 2, 15, 0xffffff);
        if (WorldHost.BEDROCK_SUPPORT) {
            drawCenteredString(context, font, BEDROCK_FRIENDS_TEXT, width / 2, height - 66 - font.lineHeight / 2, 0xffffff);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (WorldHost.BEDROCK_SUPPORT) {
            final int textWidth = font.width(BEDROCK_FRIENDS_TEXT);
            final int textX = width / 2 - textWidth / 2;
            if (mouseX >= textX && mouseX <= textX + textWidth) {
                final int textY = height - 66 - font.lineHeight / 2;
                if (mouseY >= textY && mouseY <= textY + font.lineHeight) {
                    final Style component = font.getSplitter().componentStyleAtWidth(BEDROCK_FRIENDS_TEXT, (int)Math.round(mouseX) - textX);
                    if (component != null) {
                        handleComponentClicked(component);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        private final Minecraft minecraft;
        private GameProfile profile;

        public FriendsEntry(GameProfile profile) {
            minecraft = Minecraft.getInstance();
            this.profile = profile;
            Util.backgroundExecutor().execute(
                () -> this.profile = minecraft.getMinecraftSessionService().fillProfileProperties(profile, false)
            );
        }

        //#if MC > 1.16.5
        @NotNull
        @Override
        public Component getNarration() {
            return Components.immutable(getName());
        }
        //#endif

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
            final ResourceLocation skinTexture = WorldHost.getInsecureSkinLocation(profile);
            WorldHost.color(1f, 1f, 1f, 1f);
            RenderSystem.enableBlend();
            blit(context, skinTexture, x, y, 32, 32, 8, 8, 8, 8, 64, 64);
            blit(context, skinTexture, x, y, 32, 32, 40, 8, 8, 8, 64, 64);
            RenderSystem.disableBlend();
            drawCenteredString(context, minecraft.font, getName(), x + 110, y + 16 - minecraft.font.lineHeight / 2, 0xffffff);
        }

        public String getName() {
            return WorldHost.getName(profile);
        }

        public void maybeRemove() {
            assert minecraft != null;
            minecraft.setScreen(new ConfirmScreen(
                yes -> {
                    if (yes) {
                        WorldHost.CONFIG.getFriends().remove(profile.getId());
                        WorldHost.saveConfig();
                        FriendsScreen.this.list.updateEntries();
                        final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
                        if (server != null && server.isPublished() && WorldHost.protoClient != null) {
                            WorldHost.protoClient.closedWorld(Collections.singleton(profile.getId()));
                        }
                    }
                    minecraft.setScreen(FriendsScreen.this);
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
