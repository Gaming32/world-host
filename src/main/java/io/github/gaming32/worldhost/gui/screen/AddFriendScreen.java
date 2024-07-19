package io.github.gaming32.worldhost.gui.screen;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.widget.FriendAdderSelectorButton;
import io.github.gaming32.worldhost.gui.widget.UserListWidget;
import io.github.gaming32.worldhost.plugin.FriendAdder;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.GameProfileCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public class AddFriendScreen extends WorldHostScreen {
    private static final Component FRIEND_USERNAME_TEXT = Components.translatable("world-host.add_friend.enter_username");

    private final Screen parent;
    private final Consumer<FriendListFriend> addAction;
    private FriendListFriend prefilledFriend;

    private final List<FriendAdder> friendAdders = WorldHost.getFriendAdders();
    private FriendAdder friendAdder = friendAdders.getFirst();

    private int maxFriends;
    private UserListWidget userList;
    private EditBox nameField;
    private long lastTyping;
    private boolean delayedFriendUpdate;
    private int reloadCount;

    public AddFriendScreen(
        Screen parent,
        Component title,
        @Nullable FriendListFriend prefilledFriend,
        Consumer<FriendListFriend> addAction
    ) {
        super(title);
        this.parent = parent;
        this.addAction = addAction;
        this.prefilledFriend = prefilledFriend;
    }

    private void resolveFriends(String name) {
        final int currentReloadCount = ++reloadCount;
        friendAdder.searchFriends(name, maxFriends)
            .thenAcceptAsync(friends -> {
                if (reloadCount == currentReloadCount && userList != null) {
                    userList.setUsers(friends);
                }
            }, Minecraft.getInstance())
            .exceptionally(t -> {
                WorldHost.LOGGER.error("Failed to request friend with name '{}'", name, t);
                return null;
            });
    }

    @Override
    protected void init() {
        assert minecraft != null;
        sendRepeatEvents(true);
        GameProfileCache.setUsesAuthentication(true); // This makes non-existent users return an empty value instead of an offline mode fallback.

        nameField = addRenderableWidget(new EditBox(font, width / 2 - 100, 66, 200, 20, nameField, FRIEND_USERNAME_TEXT));
        nameField.setMaxLength(36);
        //#if MC >= 1.19.4
        setInitialFocus(nameField);
        //#else
        //$$ nameField.setFocus(true);
        //#endif
        nameField.setResponder(name -> {
            lastTyping = Util.getMillis();
            if (friendAdder.delayLookup(name)) {
                delayedFriendUpdate = true;
            } else {
                delayedFriendUpdate = false;
                resolveFriends(name);
            }
        });

        final int widgetsX = width / 2 - 100;
        final int widgetsWidth = 200;

        int topWidgetsY = 90;
        if (showFriendAddersButton()) {
            addRenderableWidget(new FriendAdderSelectorButton(
                widgetsX, topWidgetsY, widgetsWidth, 20,
                Components.translatable("world-host.add_friend.friend_adder"),
                button -> {
                    friendAdder = button.getValue();
                    resolveFriends(nameField.getValue());
                },
                friendAdders.toArray(FriendAdder[]::new)
            ));
            topWidgetsY += 24;
        }

        int cancelY = 216;
        while (cancelY + 24 < height / 4 + 156) {
            cancelY += 24;
        }
        addRenderableWidget(
            button(CommonComponents.GUI_CANCEL, button -> minecraft.setScreen(parent))
                .pos(widgetsX, cancelY)
                .width(widgetsWidth)
                .build()
        );

        maxFriends = (cancelY - 90) / 24;
        if (showFriendAddersButton()) {
            maxFriends--;
        }

        userList = addRenderableWidget(new UserListWidget(
            font,
            widgetsX, topWidgetsY, widgetsWidth, cancelY - topWidgetsY,
            friend -> {
                addAction.accept(friend);
                minecraft.setScreen(parent);
            },
            userList
        ));
        if (prefilledFriend != null) {
            userList.setUsers(List.of(prefilledFriend));
            prefilledFriend = null;
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        final int oldMaxFriends = maxFriends;
        super.resize(minecraft, width, height);
        if (maxFriends != oldMaxFriends) {
            resolveFriends(nameField.getValue());
        }
    }

    private boolean showFriendAddersButton() {
        return friendAdders.size() > 1;
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        sendRepeatEvents(false);
    }

    @Override
    public void tick() {
        //#if MC < 1.20.2
        //$$ nameField.tick();
        //#endif
        if (Util.getMillis() - 300 > lastTyping && delayedFriendUpdate) {
            delayedFriendUpdate = false;
            resolveFriends(nameField.getValue());
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
        assert minecraft != null;
        final int labelColor = minecraft.level == null ? 0xa0a0a0 : 0xc0c0c0;
        whRenderBackground(context, mouseX, mouseY, delta);
        drawCenteredString(context, font, title, width / 2, 20, 0xffffff);
        drawString(context, font, FRIEND_USERNAME_TEXT, width / 2 - 100, 50, labelColor);
        super.render(context, mouseX, mouseY, delta);
    }
}
