package io.github.gaming32.worldhost.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.widget.FriendAdderSelectorButton;
import io.github.gaming32.worldhost.plugin.FriendAdder;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.GameProfileCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

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

    private final List<FriendAdder> friendAdders = WorldHost.getFriendAdders();
    private FriendAdder friendAdder = friendAdders.getFirst();

    private Button addFriendButton;
    private EditBox nameField;
    private long lastTyping;
    private boolean delayedFriendUpdate;

    private FriendListFriend friend;
    private int reloadCount;
    private ProfileInfo friendProfile;

    public AddFriendScreen(
        Screen parent,
        Component title,
        @Nullable FriendListFriend prefilledFriend,
        Consumer<FriendListFriend> addAction
    ) {
        super(title);
        this.parent = parent;
        this.addAction = addAction;
        if (prefilledFriend != null) {
            setFriend(prefilledFriend);
        }
    }

    private void setFriend(FriendListFriend friend) {
        this.friend = friend;
        addFriendButton.active = friend != null;
        final int currentReloadCount = ++reloadCount;
        if (friend == null) {
            friendProfile = null;
            return;
        }
        friendProfile = friend.fallbackProfileInfo();
        friend.profileInfo()
            .thenAcceptAsync(ready -> {
                if (reloadCount == currentReloadCount) {
                    friendProfile = ready;
                }
            }, Minecraft.getInstance())
            .exceptionally(t -> {
                WorldHost.LOGGER.error("Failed to request profile info for {}", friend, t);
                return null;
            });
    }

    private void resolveFriend(String name) {
        final int currentReloadCount = ++reloadCount;
        friendAdder.resolveFriend(name)
            .thenAcceptAsync(ready -> {
                if (reloadCount == currentReloadCount) {
                    setFriend(ready.orElse(null));
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
        //$$ usernameField.setFocus(true);
        //#endif
        nameField.setResponder(name -> {
            lastTyping = Util.getMillis();
            setFriend(null);
            addFriendButton.active = false;
            if (friendAdder.rateLimit(name)) {
                delayedFriendUpdate = true;
            } else {
                delayedFriendUpdate = false;
                resolveFriend(name);
            }
        });

        int extraY = 0;
        if (showFriendAddersButton()) {
            addRenderableWidget(new FriendAdderSelectorButton(
                width / 2 - 100, 90, 200, 20,
                Components.translatable("world-host.add_friend.friend_adder"),
                button -> {
                    friendAdder = button.getValue();
                    setFriend(null);
                    addFriendButton.active = false;
                    resolveFriend(nameField.getValue());
                },
                friendAdders.toArray(FriendAdder[]::new)
            ));
            extraY += 24;
        }

        addFriendButton = addRenderableWidget(
            button(Components.translatable("world-host.add_friend"), button -> {
                if (friend != null) { // Just in case the user somehow clicks the button with this null
                    addAction.accept(friend);
                }
                minecraft.setScreen(parent);
            }).pos(width / 2 - 100, height / 4 + 108 + extraY)
                .width(200)
                .build()
        );
        addFriendButton.active = friend != null;

        addRenderableWidget(
            button(CommonComponents.GUI_CANCEL, button -> minecraft.setScreen(parent))
                .pos(width / 2 - 100, height / 4 + 132 + extraY)
                .width(200)
                .build()
        );
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (
            addFriendButton.active &&
            getFocused() == nameField &&
            (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
        ) {
            addFriendButton.onPress();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        //#if MC < 1.20.2
        //$$ usernameField.tick();
        //#endif
        if (Util.getMillis() - 300 > lastTyping && delayedFriendUpdate) {
            delayedFriendUpdate = false;
            resolveFriend(nameField.getValue());
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

        if (friendProfile != null) {
            assert minecraft != null;
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            final int yShift = showFriendAddersButton() ? 24 : 0;
            //#if MC >= 1.19.4
            final int addFriendY = addFriendButton.getY();
            //#else
            //$$ final int addFriendY = addFriendButton.y;
            //#endif
            final int size = addFriendY - 110 - yShift;
            final int x = width / 2 - size / 2;
            friendProfile.iconRenderer().draw(context, x, 98 + yShift, size, size);
            RenderSystem.disableBlend();
        }
    }
}
