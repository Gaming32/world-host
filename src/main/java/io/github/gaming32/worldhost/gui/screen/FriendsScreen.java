package io.github.gaming32.worldhost.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.gui.widget.UserListWidget;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.plugin.InfoTextsCategory;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.Util;
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

    private final Screen parent;
    private Button infoButton;
    private Button removeButton;
    private FriendsList list;

    private final Runnable refresher = () -> {
        assert minecraft != null;
        minecraft.execute(() -> list.reloadEntries());
    };

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
                minecraft.setScreen(new AddFriendScreen(
                    this, ADD_FRIEND_TEXT, null,
                    (friend, notify) -> friend.addFriend(notify, refresher)
                ));
            }).width(152)
                .pos(width / 2 - 154, height - 54)
                .build()
        );

        infoButton = addRenderableWidget(
            button(Components.translatable("world-host.friends.show_info"), button -> {
                if (list.getSelected() != null) {
                    list.getSelected().friend.showFriendInfo(this);
                }
            }).width(152)
                .pos(width / 2 - 154, height - 30)
                .build()
        );
        infoButton.active = false;

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

        list.reloadEntries();
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
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
        private int reloadCount = 0;

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
            infoButton.active = entry != null;
            removeButton.active = entry != null;
        }

        private void reloadEntries() {
            final int currentReloadCount = ++reloadCount;
            clearEntries();
            for (final var plugin : WorldHost.getPlugins()) {
                plugin.plugin().listFriends(friend -> Minecraft.getInstance().execute(() -> {
                    if (reloadCount == currentReloadCount) {
                        addEntry(new FriendsEntry(friend));
                    }
                }));
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

        private long clickTime;

        public FriendsEntry(FriendListFriend friend) {
            minecraft = Minecraft.getInstance();
            this.friend = friend;
            profile = friend.fallbackProfileInfo();
            friend.profileInfo()
                .thenAccept(ready -> profile = ready)
                .exceptionally(t -> {
                    WorldHost.LOGGER.error("Failed to request profile info for {}", friend, t);
                    return null;
                });
        }

        @NotNull
        @Override
        public Component getNarration() {
            return Components.translatable("narrator.select", getNameWithTag());
        }

        public Component getNameWithTag() {
            return UserListWidget.getNameWithTag(friend, profile);
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
            drawCenteredString(context, minecraft.font, getNameWithTag(), x + 110, y + 16 - minecraft.font.lineHeight / 2, 0xffffff);
        }

        public void maybeRemove() {
            assert minecraft != null;
            minecraft.setScreen(new ConfirmScreen(
                yes -> {
                    if (yes) {
                        friend.removeFriend(refresher);
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
            if (Util.getMillis() - clickTime < 250L) {
                friend.showFriendInfo(FriendsScreen.this);
                clickTime = Util.getMillis();
                return true;
            }
            clickTime = Util.getMillis();
            return false;
        }
    }
}
