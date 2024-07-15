package io.github.gaming32.worldhost.gui.screen;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.plugin.InfoTextsCategory;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.plugin.vanilla.WorldHostFriendListFriend;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public class FriendsScreen extends ScreenWithInfoTexts {
    public static final Component ADD_FRIEND_TEXT = Components.translatable("world-host.add_friend");
    private static final Component ADD_SILENTLY_TEXT = Components.translatable("world-host.friends.add_silently");

    private final Screen parent;
    private Button removeButton;
    private FriendsList list;

    public FriendsScreen(Screen parent) {
        super(WorldHostComponents.FRIENDS, InfoTextsCategory.FRIENDS_SCREEN);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        if (list == null) {
            list = new FriendsList();
            //#if MC < 1.20.5
            //$$ if (minecraft != null && minecraft.level != null) {
            //$$     list.setRenderBackground(false);
            //$$ }
            //#endif
        }
        setListSize(list, 32, getInfoTextsAdjustedBottomMargin(64));
        addWidget(list);

        addRenderableWidget(
            button(ADD_FRIEND_TEXT, button -> {
                assert minecraft != null;
                minecraft.setScreen(new AddFriendScreen(this, ADD_FRIEND_TEXT, null, profile -> {
                    addFriendAndUpdate(profile);
                    if (WorldHost.protoClient != null) {
                        WorldHost.protoClient.friendRequest(profile.getId());
                    }
                }));
            }).width(152)
                .pos(width / 2 - 154, height - 54)
                .tooltip(Components.translatable("world-host.add_friend.tooltip"))
                .build()
        );

        addRenderableWidget(
            button(ADD_SILENTLY_TEXT, button -> {
                assert minecraft != null;
                minecraft.setScreen(new AddFriendScreen(this, ADD_SILENTLY_TEXT, null, this::addFriendAndUpdate));
            }).width(152)
                .pos(width / 2 - 154, height - 30)
                .tooltip(Components.translatable("world-host.friends.add_silently.tooltip"))
                .build()
        );

        removeButton = addRenderableWidget(
            button(Components.translatable("world-host.friends.remove"), button -> {
                if (list.getSelected() != null) {
                    list.getSelected().maybeRemove();
                }
            }).width(152)
                .pos(width / 2 + 2, height - 54)
                .build()
        );
        removeButton.active = false;

        addRenderableWidget(
            button(CommonComponents.GUI_DONE, button -> {
                assert minecraft != null;
                minecraft.setScreen(parent);
            }).width(152)
                .pos(width / 2 + 2, height - 30)
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
        list.addEntry(new FriendsEntry(new WorldHostFriendListFriend(profile)));
    }

    public static void addFriend(GameProfile profile) {
        WorldHost.addFriends(profile.getId());
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
        list.render(context, mouseX, mouseY, delta);
        drawCenteredString(context, font, title, width / 2, 15, 0xffffff);
        super.render(context, mouseX, mouseY, delta);
    }

    public class FriendsList extends ObjectSelectionList<FriendsEntry> {
        public FriendsList() {
            super(
                FriendsScreen.this.minecraft,
                //#if MC >= 1.20.3
                0, 0, 0,
                //#else
                //$$ 0, 0, 0, 0,
                //#endif
                36
            );
        }

        @Override
        public void setSelected(@Nullable FriendsEntry entry) {
            super.setSelected(entry);
            removeButton.active = entry != null;
        }

        private void updateEntries() {
            clearEntries();
            for (final var plugin : WorldHost.getPlugins()) {
                plugin.plugin().listFriends(friend ->
                    Minecraft.getInstance().execute(() -> addEntry(new FriendsEntry(friend)))
                );
            }
        }

        @Override
        public int addEntry(@NotNull FriendsEntry entry) {
            return super.addEntry(entry);
        }
    }

    public class FriendsEntry extends ObjectSelectionList.Entry<FriendsEntry> {
        private final Minecraft minecraft;
        private final FriendListFriend friend;
        private ProfileInfo profile;

        public FriendsEntry(FriendListFriend friend) {
            minecraft = Minecraft.getInstance();
            this.friend = friend;
            profile = friend.fallbackProfileInfo();
            friend.profileInfo()
                .thenAccept(ready -> profile = ready)
                .exceptionally(t -> {
                    WorldHost.LOGGER.error("Failed to request profile skin for {}", friend, t);
                    return null;
                });
            }

        @NotNull
        @Override
        public Component getNarration() {
            return Components.translatable("narrator.select", profile.name());
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
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            profile.iconRenderer().draw(context, x, y, 32, 32);
            RenderSystem.disableBlend();
            drawCenteredString(context, minecraft.font, profile.name(), x + 110, y + 16 - minecraft.font.lineHeight / 2, 0xffffff);
        }

        public void maybeRemove() {
            assert minecraft != null;
            minecraft.setScreen(new ConfirmScreen(
                yes -> {
                    if (yes) {
                        friend.removeFriend(() -> minecraft.execute(() -> FriendsScreen.this.list.updateEntries()));
                    }
                    minecraft.setScreen(FriendsScreen.this);
                },
                Components.translatable("world-host.friends.remove.title"),
                Components.translatable("world-host.friends.remove.message")
            ));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            FriendsScreen.this.list.setSelected(this);
            return false;
        }
    }
}
