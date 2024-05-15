package io.github.gaming32.worldhost.testing;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.screen.AddFriendScreen;
import io.github.gaming32.worldhost.gui.screen.FriendsScreen;
import io.github.gaming32.worldhost.gui.screen.JoiningWorldHostScreen;
import io.github.gaming32.worldhost.gui.screen.OnlineFriendsScreen;
import io.github.gaming32.worldhost.gui.screen.WorldHostConfigScreen;
import io.github.gaming32.worldhost.mixin.DisconnectedScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import java.util.function.Supplier;

import static io.github.gaming32.worldhost.testing.MinecraftApi.*;

public class WorldHostTesting {
    public static final boolean ENABLED = Boolean.getBoolean("world-host-testing.enabled");
    public static final TestingUser USER = ENABLED
        ? TestingUser.valueOf(System.getProperty("world-host-testing.user"))
        : null;

    public static final Supplier<ScreenChain> SCREEN_CHAIN = Suppliers.memoize(() -> switch (USER) {
        case HOST -> createHost();
        case JOINER -> createJoiner();
        case null -> throw new AssertionError("Shouldn't initialize WorldHostTesting.SCREEN_CHAIN outside of testing");
    });

    private static final int TIMEOUT = 60_000;

    private static ScreenChain createHost() {
        return ScreenChain.start()
            .then(TitleScreen.class, () -> click(findWidgetByTranslation("menu.singleplayer")))
            .maybe(SafetyScreen.class, () -> click(findWidgetByTranslation("gui.proceed")))
            .maybe(SelectWorldScreen.class, () -> click(findWidgetByTranslation("world-host.create_world")))
            .then(CreateWorldScreen.class, () -> click(findWidgetByTranslation("selectWorld.create")))
            .skip(LevelLoadingScreen.class, ReceivingLevelScreen.class)
            .then(null, () -> press(InputConstants.KEY_ESCAPE))
            .then(PauseScreen.class, () -> click(findWidgetByTranslation("world-host.online_status")))
            .then(addFriend(TestingUser.JOINER))
            .then(PauseScreen.class, () -> click(findWidgetByTranslation("world-host.open_world")))
            .then(ShareToLanScreen.class, () -> click(findWidgetByTranslation("world-host.open_world")))
            .then(null, () -> waitForJoinerToJoin(0))
            .then(PauseScreen.class, () -> click(findWidgetByTranslation("menu.returnToMenu")))
            .then(TitleScreen.class, () -> click(findWidgetByTranslation("menu.quit")));
    }

    private static void waitForJoinerToJoin(long timeSpent) {
        WorldHost.LOGGER.info("Checking for joins...");
        final var server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            throw new IllegalStateException("Server shutdown before joiner joined");
        }
        if (server.getPlayerCount() < 2) {
            if (timeSpent > TIMEOUT) {
                throw new IllegalStateException("Timed out waiting for joiner to join");
            }
            sleep(2000, () -> waitForJoinerToJoin(timeSpent + 2000));
        } else {
            sleep(15_000, () -> press(InputConstants.KEY_ESCAPE));
        }
    }

    private static ScreenChain createJoiner() {
        return ScreenChain.start()
            .then(TitleScreen.class, () -> click(findWidgetByTranslation("world-host.online_status")))
            .then(addFriend(TestingUser.HOST))
            .then(TitleScreen.class, () -> click(findWidgetByTranslation("menu.multiplayer")))
            .maybe(SafetyScreen.class, () -> click(findWidgetByTranslation("gui.proceed")))
            .then(JoinMultiplayerScreen.class, () -> click(findWidgetByTranslation("world-host.friends")))
            .then(OnlineFriendsScreen.class, () -> waitForHostToHost(0L))
            .skip(JoiningWorldHostScreen.class)
            .then(tryToJoinServer())
            .skipAbsentScreen()
            .then(DisconnectedScreen.class, () -> click(findWidgetByTranslation("gui.toMenu")))
            .then(OnlineFriendsScreen.class, () -> click(findWidgetByTranslation("gui.cancel")))
            .then(TitleScreen.class, () -> click(findWidgetByTranslation("menu.quit")));
    }

    private static ScreenChain tryToJoinServer() {
        return ScreenChain.start()
            .skip(ConnectScreen.class)
            .maybe(DisconnectedScreen.class, () -> {
                final var screen = (DisconnectedScreenAccessor)Minecraft.getInstance().screen;
                assert screen != null;
                throw new IllegalStateException("Unexpected disconnect trying to join server: " + screen.getReason().getString());
            })
            .skip(ReceivingLevelScreen.class);
    }

    private static void waitForHostToHost(long timeSpent) {
        WorldHost.LOGGER.info("Checking for host...");
        final var list = findGuiElements(OnlineFriendsScreen.OnlineFriendsList.class).findFirst().orElseThrow();
        if (list.children().isEmpty()) {
            if (timeSpent > TIMEOUT) {
                throw new IllegalStateException("Timed out waiting for host to host");
            }
            press(InputConstants.KEY_F5);
            sleep(2000, () -> waitForHostToHost(timeSpent + 2000));
        } else {
            click(
                list.getRowLeft() + list.getRowWidth() / 2.0,
                list.getRowTop(0) + list.getItemHeight() / 2.0
            );
            click(findWidgetByTranslation("selectServer.select"));
        }
    }

    private static ScreenChain addFriend(TestingUser user) {
        return ScreenChain.start()
            .then(WorldHostConfigScreen.class, () -> click(findWidgetByTranslation("world-host.friends")))
            .then(FriendsScreen.class, () -> click(findWidgetByTranslation("world-host.add_friend")))
            .then(AddFriendScreen.class, () -> {
                enterText(findWidgetByTranslation("world-host.add_friend.enter_username"), "o:" + user + getUsernameSuffix());
                click(findWidgetByTranslation("world-host.add_friend"));
            })
            .then(FriendsScreen.class, () -> click(findWidgetByTranslation("gui.done")))
            .then(WorldHostConfigScreen.class, () -> click(findWidgetByTranslation("gui.done")));
    }

    private static String getUsernameSuffix() {
        final String username = Minecraft.getInstance().getUser().getName();
        if (!username.startsWith(USER.name())) {
            throw new IllegalStateException("Username " + username + " doesn't start with " + USER);
        }
        return username.substring(USER.name().length());
    }

    public enum TestingUser {
        HOST, JOINER
    }
}
