package io.github.gaming32.worldhost.gui.screen;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.widget.UserListWidget;
import io.github.gaming32.worldhost.plugin.FriendAdder;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.Util;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.GameProfileCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public class AddFriendScreen extends WorldHostScreen {
    private static final Component FRIEND_USERNAME_TEXT = Components.translatable("world-host.add_friend.enter_username");
    private static final Component ADD_FRIEND_TEXT = Components.literal("+");
    //#if MC >= 1.20.0
    @VisibleForTesting
    public static final Component ADD_FRIEND_SILENT_TEXT = Components.literal("+\ud83d\udd08");
    private static final Component ADD_FRIEND_NOTIFY_TEXT = Components.literal("+\ud83d\udd0a");
    //#else
    //$$ @VisibleForTesting
    //$$ public static final Component ADD_FRIEND_SILENT_TEXT = Components.literal("+Q");
    //$$ private static final Component ADD_FRIEND_NOTIFY_TEXT = Components.literal("+N");
    //#endif
    private static final Component ADD_FRIEND_SILENT_TOOLTIP = Components.translatable("world-host.friends.add_silently.tooltip");
    private static final Component ADD_FRIEND_NOTIFY_TOOLTIP = Components.translatable("world-host.add_friend.tooltip");

    private final Screen parent;
    private final BiConsumer<FriendListFriend, Boolean> addAction;
    private FriendListFriend prefilledFriend;

    private final List<FriendAdder> friendAdders = WorldHost.getFriendAdders();

    private int maxFriends;
    private UserListWidget userList;
    private EditBox nameField;
    private long lastTyping;
    @Nullable
    private Runnable delayedLookup;

    public AddFriendScreen(
        Screen parent,
        Component title,
        @Nullable FriendListFriend prefilledFriend,
        BiConsumer<FriendListFriend, Boolean> addAction
    ) {
        super(title);
        this.parent = parent;
        this.addAction = addAction;
        this.prefilledFriend = prefilledFriend;
    }

    private void resolveFriends(FriendAdder adder, String name) {
        assert minecraft != null;
        adder.searchFriends(
            name,
            maxFriends - userList.getUsersCount(),
            friend -> minecraft.execute(() -> {
                if (nameField.getValue().equals(name)) {
                    userList.addUser(friend);
                }
            })
        );
    }

    @Override
    protected void init() {
        assert minecraft != null;
        sendRepeatEvents(true);
        GameProfileCache.setUsesAuthentication(true); // This makes non-existent users return an empty value instead of an offline mode fallback.

        nameField = addRenderableWidget(new EditBox(font, width / 2 - 100, 66, 200, 20, nameField, FRIEND_USERNAME_TEXT));
        friendAdders.stream().mapToInt(FriendAdder::maxValidNameLength).max().ifPresent(nameField::setMaxLength);
        //#if MC >= 1.19.4
        setInitialFocus(nameField);
        //#else
        //$$ nameField.setFocus(true);
        //#endif
        nameField.setResponder(name -> {
            lastTyping = Util.getMillis();
            userList.clearUsers();
            final List<FriendAdder> delayedAdders = new ArrayList<>();
            for (final FriendAdder adder : friendAdders) {
                if (name.length() > adder.maxValidNameLength()) continue;
                if (adder.delayLookup(name)) {
                    delayedAdders.add(adder);
                } else {
                    resolveFriends(adder, name);
                }
            }
            if (!delayedAdders.isEmpty()) {
                delayedLookup = () -> {
                    for (final FriendAdder adder : delayedAdders) {
                        resolveFriends(adder, name);
                    }
                };
            } else {
                delayedLookup = null;
            }
        });

        final int widgetsX = width / 2 - 100;
        final int widgetsWidth = 200;

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

        userList = addRenderableWidget(new UserListWidget(
            font,
            widgetsX, 90, widgetsWidth, cancelY - 90,
            this::getActions,
            userList
        ));
        if (prefilledFriend != null) {
            userList.setUsers(List.of(prefilledFriend));
            prefilledFriend = null;
        }
    }

    private List<UserListWidget.Action> getActions(FriendListFriend user) {
        if (user.supportsNotifyAdd()) {
            return List.of(
                new UserListWidget.Action(
                    ADD_FRIEND_NOTIFY_TEXT, ADD_FRIEND_NOTIFY_TOOLTIP,
                    getAddRunnable(user, true)
                ),
                new UserListWidget.Action(
                    ADD_FRIEND_SILENT_TEXT, ADD_FRIEND_SILENT_TOOLTIP,
                    getAddRunnable(user, false)
                )
            );
        } else {
            return List.of(
                new UserListWidget.Action(ADD_FRIEND_TEXT, getAddRunnable(user, false))
            );
        }
    }

    private Runnable getAddRunnable(FriendListFriend user, boolean notify) {
        assert minecraft != null;
        return () -> {
            addAction.accept(user, notify);
            minecraft.setScreen(parent);
        };
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
        if (Util.getMillis() - 300 > lastTyping && delayedLookup != null) {
            delayedLookup.run();
            delayedLookup = null;
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
