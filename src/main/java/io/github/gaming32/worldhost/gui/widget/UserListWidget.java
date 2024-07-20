package io.github.gaming32.worldhost.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.toast.IconRenderer;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static io.github.gaming32.worldhost.gui.screen.WorldHostScreen.button;
import static io.github.gaming32.worldhost.gui.screen.WorldHostScreen.drawString;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public final class UserListWidget extends AbstractContainerWidget {
    private final List<UserInfo> users = new ArrayList<>();
    private final List<Button> actionButtons = new ArrayList<>();
    private final Font font;
    private final Function<FriendListFriend, List<Action>> getApplicableActions;

    public UserListWidget(
        Font font,
        int x, int y, int width, int height,
        Function<FriendListFriend, List<Action>> getApplicableActions
    ) {
        this(font, x, y, width, height, getApplicableActions, null);
    }

    public UserListWidget(
        Font font,
        int x, int y, int width, int height,
        Function<FriendListFriend, List<Action>> getApplicableActions,
        @Nullable UserListWidget old
    ) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.getApplicableActions = getApplicableActions;
        if (old != null && !old.users.isEmpty()) {
            users.addAll(old.users);
            addButtons(0);
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
            final var user = users.get(i);
            user.getIcon().draw(context, x, y, 20, 20);
            RenderSystem.disableBlend();
            drawString(context, font, user.getName(), x + 24, y + textYOffset, 0xffffff, true);
            y += 24;
        }
        for (final Button button : actionButtons) {
            button.render(context, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public int getUsersCount() {
        return users.size();
    }

    public void clearUsers() {
        users.clear();
        actionButtons.clear();
    }

    public void setUsers(List<? extends FriendListFriend> users) {
        this.users.clear();
        actionButtons.clear();
        addUsers(users);
    }

    public void addUsers(List<? extends FriendListFriend> users) {
        final int oldSize = this.users.size();
        for (final FriendListFriend user : users) {
            this.users.add(new UserInfo(user));
        }
        addButtons(oldSize);
    }

    public void addUser(FriendListFriend user) {
        users.add(new UserInfo(user));
        addButtons(users.size() - 1);
    }

    private void addButtons(int fromI) {
        int y = getY() + fromI * 24;
        for (int i = fromI; i < getVisibleCount(); i++) {
            final UserInfo user = users.get(i);
            int x = getRight() - 24 * user.actions.size() + 4;
            for (final Action action : user.actions) {
                actionButtons.add(
                    button(action.text, b -> action.apply.run())
                        .tooltip(action.tooltip)
                        .pos(x, y)
                        .size(20, 20)
                        .build()
                );
                x += 24;
            }
            y += 24;
        }
    }

    public int getVisibleCount() {
        return Math.min(users.size(), getHeight() / 24);
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return actionButtons;
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

    public static Component getNameWithTag(FriendListFriend user, ProfileInfo profile) {
        return user.tag()
            .map(component -> Components.translatable(
                "world-host.friends.tagged_friend",
                profile.name(), component
            ))
            .orElseGet(() -> Components.literal(profile.name()));
    }

    private final class UserInfo {
        final FriendListFriend user;
        final List<Action> actions;
        private ProfileInfo profile;

        UserInfo(FriendListFriend user) {
            this.user = user;
            actions = getApplicableActions.apply(user);
            profile = user.fallbackProfileInfo();
            user.profileInfo()
                .thenAcceptAsync(ready -> profile = ready, Minecraft.getInstance())
                .exceptionally(t -> {
                    WorldHost.LOGGER.error("Failed to request profile info for {}", user, t);
                    return null;
                });
        }

        FormattedCharSequence getName() {
            final Component name = getNameWithTag(user, profile);
            final int maxWidth = width - 24 - 24 * actions.size();
            if (font.width(name) <= maxWidth) {
                return name.getVisualOrderText();
            }
            final FormattedText clipped = font.substrByWidth(name, maxWidth - font.width(CommonComponents.ELLIPSIS));
            return Language.getInstance().getVisualOrder(FormattedText.composite(clipped, CommonComponents.ELLIPSIS));
        }

        IconRenderer getIcon() {
            return profile.iconRenderer();
        }
    }

    public record Action(Component text, @Nullable Component tooltip, Runnable apply) {
        public Action(Component text, Runnable apply) {
            this(text, null, apply);
        }
    }
}
