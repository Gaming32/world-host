package io.github.gaming32.worldhost.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.players.GameProfileCache;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.regex.Pattern;

// TODO: Fix the face going beneath the screen
public class AddFriendScreen extends WorldHostScreen {
    public static final Pattern VALID_USERNAME = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
    private static final Component FRIEND_USERNAME_TEXT = Components.translatable("world-host.add_friend.enter_username");

    private final Screen parent;
    private final Consumer<GameProfile> addAction;

    private Button addFriendButton;
    private EditBox usernameField;
    private long lastTyping;
    private boolean usernameUpdate;
    private GameProfile friendProfile;

    public AddFriendScreen(Screen parent, Component title, Consumer<GameProfile> addAction) {
        super(title);
        this.parent = parent;
        this.addAction = addAction;
    }

    @Override
    protected void init() {
        assert minecraft != null;
        sendRepeatEvents(true);
        GameProfileCache.setUsesAuthentication(true); // This makes non-existent users return an empty value instead of an offline mode fallback.

        addFriendButton = addRenderableWidget(
            button(Components.translatable("world-host.add_friend"), button -> {
                if (friendProfile != null) { // Just in case the user somehow clicks the button with this null
                    addAction.accept(friendProfile);
                }
                minecraft.setScreen(parent);
            }).pos(width / 2 - 100, 288)
                .width(200)
                .tooltip(Components.translatable("world-host.add_friend.tooltip"))
                .build()
        );
        addFriendButton.active = false;

        addRenderableWidget(
            button(CommonComponents.GUI_CANCEL, button -> minecraft.setScreen(parent))
                .pos(width / 2 - 100, 312)
                .width(200)
                .build()
        );

        usernameField = addWidget(new EditBox(font, width / 2 - 100, 116, 200, 20, FRIEND_USERNAME_TEXT));
        usernameField.setMaxLength(16);
        usernameField.setFocused(true);
        usernameField.setResponder(text -> {
            lastTyping = Util.getMillis();
            usernameUpdate = true;
            friendProfile = null;
            addFriendButton.active = false;
        });
    }

    @Override
    public void resize(@NotNull Minecraft client, int width, int height) {
        final String oldUsername = usernameField.getValue();
        super.resize(client, width, height);
        usernameField.setValue(oldUsername);
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
                getFocused() == usernameField &&
                (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
        ) {
            addFriendButton.onPress();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (Util.getMillis() - 300 > lastTyping && usernameUpdate) {
            usernameUpdate = false;
            final String username = usernameField.getValue();
            if (VALID_USERNAME.matcher(username).matches()) {
                WorldHost.getMaybeAsync(WorldHost.getProfileCache(), username, p -> {
                    if (p.isPresent()) {
                        assert minecraft != null;
                        friendProfile = minecraft.getMinecraftSessionService().fillProfileProperties(p.get(), false);
                        addFriendButton.active = true;
                    } else {
                        friendProfile = null;
                    }
                });
            }
        }
    }

    @Override
    public void render(@NotNull PoseStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredString(matrices, font, title, width / 2, 20, 16777215);
        drawString(matrices, font, FRIEND_USERNAME_TEXT, width / 2 - 100, 100, 10526880);
        usernameField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);

        if (friendProfile != null) {
            assert minecraft != null;
            final ResourceLocation skinTexture = WorldHost.getInsecureSkinLocation(friendProfile);
            WorldHost.positionTexShader();
            WorldHost.color(1f, 1f, 1f, 1f);
            WorldHost.texture(skinTexture);
            RenderSystem.enableBlend();
            GuiComponent.blit(matrices, width / 2 - 64, 148, 128, 128, 8, 8, 8, 8, 64, 64);
            GuiComponent.blit(matrices, width / 2 - 64, 148, 128, 128, 40, 8, 8, 8, 64, 64);
            RenderSystem.disableBlend();
        }
    }
}
