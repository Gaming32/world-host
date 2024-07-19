package io.github.gaming32.worldhost.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.toast.IconRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static io.github.gaming32.worldhost.gui.screen.WorldHostScreen.button;
import static io.github.gaming32.worldhost.gui.screen.WorldHostScreen.drawString;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public final class UserListWidget extends AbstractContainerWidget {
    private final List<FriendListFriend> users = new ArrayList<>();
    private final List<Button> addButtons = new ArrayList<>();
    private final List<CachedProfile> userProfiles = new ArrayList<>();
    private final Font font;
    private final Consumer<FriendListFriend> doneAction;

    public UserListWidget(Font font, int x, int y, int width, int height, Consumer<FriendListFriend> doneAction) {
        this(font, x, y, width, height, doneAction, null);
    }

    public UserListWidget(
        Font font, int x, int y, int width, int height,
        Consumer<FriendListFriend> doneAction,
        @Nullable UserListWidget old
    ) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.doneAction = doneAction;
        if (old != null && !old.users.isEmpty()) {
            users.addAll(old.users);
            initButtons();
            userProfiles.addAll(old.userProfiles);
        }
    }

    @Override
    protected void renderWidget(
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int mouseX, int mouseY, float partialTick
    ) {
        final int textYOffset = 10 - font.lineHeight / 2;
        final int x = getX();
        int y = getY();
        for (int i = 0; i < getVisibleCount(); i++) {
            final Button addButton = addButtons.get(i);
            final CachedProfile profile = userProfiles.get(i);
            profile.getIcon().draw(context, x, y, 20, 20);
            RenderSystem.disableBlend();
            drawString(context, font, profile.getName(), x + 24, y + textYOffset, 0xffffff, true);
            addButton.render(context, mouseX, mouseY, partialTick);
            y += 24;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public List<FriendListFriend> getUsers() {
        return users;
    }

    public void setUsers(List<? extends FriendListFriend> users) {
        this.users.clear();
        this.users.addAll(users);
        initButtons();
        initUserProfiles();
    }

    private void initButtons() {
        addButtons.clear();
        int y = getY();
        for (int i = 0; i < getVisibleCount(); i++) {
            final FriendListFriend user = users.get(i);
            addButtons.add(
                button(Component.literal("+"), b -> doneAction.accept(user))
                    .pos(getRight() - 20, y)
                    .size(20, 20)
                    .build()
            );
            y += 24;
        }
    }

    private void initUserProfiles() {
        userProfiles.clear();
        for (final FriendListFriend user : users) {
            userProfiles.add(new CachedProfile(user));
        }
    }

    public int getVisibleCount() {
        return Math.min(users.size(), getHeight() / 24);
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return addButtons;
    }

    //#if MC < 1.19.4
    //$$ private int getX() {
    //$$     return x;
    //$$ }
    //$$
    //$$ private int getY() {
    //$$     return y;
    //$$ }
    //#endif

    private final class CachedProfile {
        private ProfileInfo profile;

        CachedProfile(FriendListFriend user) {
            profile = user.fallbackProfileInfo();
            user.profileInfo()
                .thenAcceptAsync(ready -> profile = ready, Minecraft.getInstance())
                .exceptionally(t -> {
                    WorldHost.LOGGER.error("Failed to request profile info for {}", user, t);
                    return null;
                });
        }

        String getName() {
            return fitName(profile.name());
        }

        IconRenderer getIcon() {
            return profile.iconRenderer();
        }

        private String fitName(String name) {
            final int nameWidth = font.width(name);
            final int maxWidth = width - 48;
            if (nameWidth <= maxWidth) {
                return name;
            }
            final String ellipses = "...";
            final String clipped = font.plainSubstrByWidth(name, maxWidth - font.width(ellipses));
            return clipped + ellipses;
        }
    }
}
