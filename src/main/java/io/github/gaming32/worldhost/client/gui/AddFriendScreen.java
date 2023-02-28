package io.github.gaming32.worldhost.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.client.WorldHostClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.regex.Pattern;

public class AddFriendScreen extends Screen {
    public static final Pattern VALID_USERNAME = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
    private static final Text FRIEND_USERNAME_TEXT = Text.translatable("world-host.add_friend.enter_username");

    private final Screen parent;
    private final Consumer<GameProfile> addAction;

    private ButtonWidget addFriendButton;
    private TextFieldWidget usernameField;
    private long lastTyping;
    private boolean usernameUpdate;
    private GameProfile friendProfile;

    protected AddFriendScreen(Screen parent, Text title, Consumer<GameProfile> addAction) {
        super(title);
        this.parent = parent;
        this.addAction = addAction;
    }

    @Override
    protected void init() {
        assert client != null;
        client.keyboard.setRepeatEvents(true);
        UserCache.setUseRemote(true); // This makes non-existent users return an empty value instead of an offline mode fallback.

        addFriendButton = addDrawableChild(new ButtonWidget(width / 2 - 100, 288, 200, 20, Text.translatable("world-host.add_friend"), button -> {
            if (friendProfile != null) { // Just in case the user somehow clicks the button with this null
                addAction.accept(friendProfile);
            }
            client.setScreen(parent);
        }));
        addFriendButton.active = false;

        addDrawableChild(new ButtonWidget(width / 2 - 100, 312, 200, 20, ScreenTexts.CANCEL, button -> client.setScreen(parent)));

        usernameField = addSelectableChild(new TextFieldWidget(textRenderer, width / 2 - 100, 116, 200, 20, FRIEND_USERNAME_TEXT));
        usernameField.setMaxLength(16);
        usernameField.setTextFieldFocused(true);
        usernameField.setChangedListener(text -> {
            lastTyping = Util.getMeasuringTimeMs();
            usernameUpdate = true;
            friendProfile = null;
            addFriendButton.active = false;
        });
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        final String oldUsername = usernameField.getText();
        super.resize(client, width, height);
        usernameField.setText(oldUsername);
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        assert client != null;
        client.keyboard.setRepeatEvents(false);
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
        if (Util.getMeasuringTimeMs() - 300 > lastTyping && usernameUpdate) {
            usernameUpdate = false;
            final String username = usernameField.getText();
            if (VALID_USERNAME.matcher(username).matches()) {
                WorldHostClient.API_SERVICES.userCache().findByNameAsync(username, p -> {
                    if (p.isPresent()) {
                        assert client != null;
                        friendProfile = client.getSessionService().fillProfileProperties(p.get(), false);
                        addFriendButton.active = true;
                    } else {
                        friendProfile = null;
                    }
                });
            }
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, textRenderer, title, width / 2, 20, 16777215);
        drawTextWithShadow(matrices, textRenderer, FRIEND_USERNAME_TEXT, width / 2 - 100, 100, 10526880);
        usernameField.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);

        if (friendProfile != null) {
            assert client != null;
            final Identifier skinTexture = client.getSkinProvider().loadSkin(friendProfile);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.setShaderTexture(0, skinTexture);
            RenderSystem.enableBlend();
            DrawableHelper.drawTexture(matrices, width / 2 - 64, 148, 128, 128, 8, 8, 8, 8, 64, 64);
            DrawableHelper.drawTexture(matrices, width / 2 - 64, 148, 128, 128, 40, 8, 8, 8, 64, 64);
            RenderSystem.disableBlend();
        }
    }
}
