package io.github.gaming32.worldhost;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import io.github.gaming32.worldhost.config.WorldHostConfig;
import io.github.gaming32.worldhost.gui.screen.JoiningWorldHostScreen;
import io.github.gaming32.worldhost.gui.screen.WorldHostScreen;
import io.github.gaming32.worldhost.protocol.ProtocolClient;
import io.github.gaming32.worldhost.protocol.proxy.ProxyPassthrough;
import io.github.gaming32.worldhost.protocol.proxy.ProxyProtocolClient;
import io.github.gaming32.worldhost.proxy.ProxyClient;
import io.github.gaming32.worldhost.toast.WHToast;
import io.github.gaming32.worldhost.upnp.Gateway;
import io.github.gaming32.worldhost.upnp.GatewayFinder;
import io.github.gaming32.worldhost.versions.Components;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.players.GameProfileCache;
import org.apache.commons.io.function.IOFunction;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.JsonWriter;
import org.semver4j.Semver;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.literal;

//#if MC >= 1.19.2
import io.github.gaming32.worldhost.mixin.MinecraftAccessor;
//#else
//$$ import com.mojang.authlib.minecraft.MinecraftProfileTexture;
//$$ import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
//$$ import net.minecraft.world.entity.player.Player;
//#endif

//#if MC >= 1.20.2
import net.minecraft.client.resources.PlayerSkin;
//#endif

//#if FABRIC
import dev.isxander.mainmenucredits.MainMenuCredits;
import io.github.gaming32.worldhost.gui.OnlineStatusLocation;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
//#else
//$$ import io.github.gaming32.worldhost.gui.screen.WorldHostConfigScreen;
//#if FORGE
//$$ import net.minecraftforge.api.distmarker.Dist;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import net.minecraftforge.fml.ModList;
//$$ import net.minecraftforge.fml.ModLoadingContext;
//$$ import net.minecraftforge.fml.common.Mod;
//$$ import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//$$ import net.minecraftforge.fml.loading.FMLPaths;
//#else
//$$ import net.neoforged.api.distmarker.Dist;
//$$ import net.neoforged.bus.api.SubscribeEvent;
//$$ import net.neoforged.fml.ModList;
//$$ import net.neoforged.fml.ModLoadingContext;
//$$ import net.neoforged.fml.common.Mod;
//$$ import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
//$$ import net.neoforged.fml.loading.FMLPaths;
//#endif
//$$ import java.util.function.BiFunction;
//#if MC >= 1.20.5
//$$ import net.neoforged.fml.common.EventBusSubscriber;
//$$ import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
//#elseif NEOFORGE
//$$ import net.neoforged.neoforge.client.ConfigScreenHandler;
//#elseif MC >= 1.19.2
//$$ import net.minecraftforge.client.ConfigScreenHandler;
//#else
//$$ import net.minecraftforge.client.ConfigGuiHandler;
//#endif
//#endif

//#if FORGELIKE
//$$ @Mod(WorldHost.MOD_ID)
//#endif
public class WorldHost
    //#if FABRIC
    implements ClientModInitializer
    //#endif
{
    public static final String MOD_ID =
        //#if FORGELIKE
        //$$ "world_host";
        //#else
        "world-host";
        //#endif

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Loader MOD_LOADER =
        //#if NEOFORGE
        //$$ Loader.NEOFORGE;
        //#elseif FORGE
        //$$ Loader.FORGE;
        //#else
        Loader.FABRIC;
        //#endif

    @SuppressWarnings("PointlessArithmeticExpression")
    private static final int[] RECONNECT_DELAYS = {
        1 * 20,
        5 * 20,
        10 * 20,
        15 * 20,
        30 * 20,
        60 * 20,
        90 * 20,
        120 * 20,
        300 * 20
    };

    public static final Path GAME_DIR = getGameDir();
    public static final Path CACHE_DIR = GAME_DIR.resolve(".world-host-cache");

    public static final Path CONFIG_DIR = GAME_DIR.resolve("config");
    public static final Path CONFIG_FILE = CONFIG_DIR.resolve("world-host.json5");
    public static final Path FRIENDS_FILE = CONFIG_DIR.resolve("world-host-friends.json");
    public static final Path OLD_CONFIG_FILE = CONFIG_DIR.resolve("world-host.json");
    public static final WorldHostConfig CONFIG = new WorldHostConfig();

    private static List<String> wordsForCid;
    private static Object2IntMap<String> wordsForCidInverse;

    public static final long MAX_CONNECTION_IDS = 1L << 42;

    public static final Object2LongMap<UUID> ONLINE_FRIENDS = new Object2LongLinkedOpenHashMap<>();
    public static final Map<UUID, ServerStatus> ONLINE_FRIEND_PINGS = new HashMap<>();
    public static final Set<FriendsListUpdate> ONLINE_FRIEND_UPDATES = Collections.newSetFromMap(new WeakHashMap<>());

    public static final Long2ObjectMap<ProxyClient> CONNECTED_PROXY_CLIENTS = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    public static final long CONNECTION_ID = new SecureRandom().nextLong(MAX_CONNECTION_IDS);

    public static final boolean BEDROCK_SUPPORT =
        //#if FABRIC
        FabricLoader.getInstance().isModLoaded("world-host-bedrock");
        //#else
        //$$ false;
        //#endif

    private static boolean hasScannedForUpnp;
    public static Gateway upnpGateway;

    private static GameProfileCache profileCache;

    public static ProtocolClient protoClient;
    public static ProxyProtocolClient proxyProtocolClient;
    public static int reconnectDelay = 0;
    private static int delayIndex = 0;
    private static Future<Void> connectingFuture;

    public static boolean shareWorldOnLoad;

    public static SocketAddress proxySocketAddress;
    public static Constructor<? extends ChannelInitializer<Channel>> channelInitializerConstructor;

    //#if FABRIC
    @Override
    public void onInitializeClient() {
        init();
    }
    //#endif

    private static void init() {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                WorldHost.class.getResourceAsStream("/assets/world-host/16k.txt"),
                StandardCharsets.US_ASCII
            )
        )) {
            wordsForCid = reader.lines().filter(s -> !s.startsWith("//")).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (wordsForCid.size() != (1 << 14)) {
            throw new RuntimeException("Expected WORDS_FOR_CID to have " + (1 << 14) + " elements, but it has " + wordsForCid.size() + " elements.");
        }
        wordsForCidInverse = new Object2IntAVLTreeMap<>(String.CASE_INSENSITIVE_ORDER);
        wordsForCidInverse.defaultReturnValue(-1);
        for (int i = 0; i < wordsForCid.size(); i++) {
            wordsForCidInverse.put(wordsForCid.get(i), i);
        }

        LOGGER.info("Using client-generated connection ID {}", connectionIdToString(CONNECTION_ID));

        loadConfig();

        try {
            Files.createDirectories(CACHE_DIR);
        } catch (IOException e) {
            LOGGER.error("Failed to create cache directory", e);
        }
        profileCache = new GameProfileCache(
            //#if MC >= 1.19.2
            ((MinecraftAccessor)Minecraft.getInstance()).getAuthenticationService().createProfileRepository(),
            //#else
            //$$ new YggdrasilAuthenticationService(Minecraft.getInstance().getProxy()).createProfileRepository(),
            //#endif
            CACHE_DIR.resolve("usercache.json").toFile()
        );
        profileCache.setExecutor(Minecraft.getInstance());

        reconnect(false, true);

        if (!CONFIG.isNoUPnP()) {
            scanUpnp();
        }
    }

    public static void loadConfig() {
        try (JsonReader reader = JsonReader.json5(CONFIG_FILE)) {
            CONFIG.read(reader);
            if (Files.exists(OLD_CONFIG_FILE)) {
                LOGGER.info("Old {} still exists. Maybe consider removing it?", OLD_CONFIG_FILE.getFileName());
            }
        } catch (NoSuchFileException e) {
            LOGGER.info("{} not found. Trying to load old {}.", CONFIG_FILE.getFileName(), OLD_CONFIG_FILE.getFileName());
            try (JsonReader reader = JsonReader.json(OLD_CONFIG_FILE)) {
                CONFIG.read(reader);
                LOGGER.info(
                    "Found and read old {} into new {}. Maybe consider deleting the old {}?",
                    OLD_CONFIG_FILE.getFileName(), CONFIG_FILE.getFileName(), OLD_CONFIG_FILE.getFileName()
                );
            } catch (NoSuchFileException e1) {
                LOGGER.info("Old {} not found. Writing default config.", OLD_CONFIG_FILE.getFileName());
            } catch (IOException e1) {
                LOGGER.error("Failed to load old {}.", OLD_CONFIG_FILE.getFileName(), e1);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load {}.", CONFIG_FILE.getFileName(), e);
        }
        try (JsonReader reader = JsonReader.json(FRIENDS_FILE)) {
            CONFIG.readFriends(reader);
        } catch (NoSuchFileException ignored) {
        } catch (Exception e) {
            LOGGER.error("Failed to load {}.", FRIENDS_FILE.getFileName(), e);
        }
        saveConfig();
    }

    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (JsonWriter writer = JsonWriter.json5(CONFIG_FILE)) {
                CONFIG.write(writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write {}.", CONFIG_FILE.getFileName(), e);
        }
        try {
            Files.createDirectories(FRIENDS_FILE.getParent());
            try (JsonWriter writer = JsonWriter.json(FRIENDS_FILE)) {
                CONFIG.writeFriends(writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write {}.", FRIENDS_FILE.getFileName(), e);
        }
    }

    public static void tickHandler() {
        if (protoClient == null || protoClient.isClosed()) {
            protoClient = null;
            if (proxyProtocolClient != null) {
                proxyProtocolClient.close();
                proxyProtocolClient = null;
            }
            connectingFuture = null;
            if (reconnectDelay == 0) {
                if (delayIndex == RECONNECT_DELAYS.length) {
                    reconnectDelay = RECONNECT_DELAYS[delayIndex - 1];
                } else {
                    reconnectDelay = RECONNECT_DELAYS[delayIndex++];
                }
            } else if (--reconnectDelay == 0) {
                reconnect(CONFIG.isEnableReconnectionToasts(), false);
            }
        }
        if (proxyProtocolClient != null && proxyProtocolClient.isClosed()) {
            proxyProtocolClient = null;
        }
        if (connectingFuture != null && connectingFuture.isDone()) {
            connectingFuture = null;
            delayIndex = 0;
            refreshFriendsList();
            final var server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null && server.isPublished()) {
                protoClient.publishedWorld(CONFIG.getFriends());
            }
        }
    }

    public static void refreshFriendsList() {
        LOGGER.info("Refreshing friends list...");
        ONLINE_FRIENDS.clear();
        WorldHost.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
        protoClient.listOnline(CONFIG.getFriends());
    }

    public static void commandRegistrationHandler(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("worldhost")
            .then(literal("ip")
                .requires(s -> s.getServer().isPublished())
                .executes(WorldHost::ipCommand)
            )
            .then(literal("tempip")
                .requires(s ->
                    !CONFIG.isNoUPnP() &&
                        s.getServer().isPublished() &&
                        upnpGateway != null &&
                        protoClient != null &&
                        !protoClient.getUserIp().isEmpty()
                )
                .executes(ctx -> {
                    try {
                        final int port = ctx.getSource().getServer().getPort();
                        final var error = upnpGateway.openPort(port, 60, false);
                        if (error == null) {
                            ctx.getSource().sendSuccess(
                                //#if MC >= 1.20.0
                                () ->
                                //#endif
                                Components.translatable(
                                    "world-host.worldhost.tempip.success",
                                    Components.copyOnClickText(protoClient.getUserIp() + ':' + port)
                                ),
                                false
                            );
                            return Command.SINGLE_SUCCESS;
                        }
                        WorldHost.LOGGER.info("Failed to use UPnP mode due to {}. tempip not supported.", error);
                    } catch (Exception e) {
                        WorldHost.LOGGER.error("Failed to open UPnP due to exception", e);
                    }
                    ctx.getSource().sendFailure(Components.translatable(
                        "world-host.worldhost.tempip.failure",
                        ComponentUtils.wrapInSquareBrackets(Components.literal("/worldhost ip")).withStyle(style -> style
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/worldhost ip"))
                        )
                    ));
                    return 0;
                })
            )
        );
    }

    public static void scanUpnp() {
        if (hasScannedForUpnp) return;
        hasScannedForUpnp = true;
        LOGGER.info("Scanning for UPnP gateway");
        new GatewayFinder(gateway -> {
            upnpGateway = gateway;
            LOGGER.info("Found UPnP gateway: {}", gateway.getGatewayIP());
        });
    }

    public static void reconnect(boolean successToast, boolean failureToast) {
        if (protoClient != null) {
            protoClient.close();
            protoClient = null;
        }
        if (proxyProtocolClient != null) {
            proxyProtocolClient.close();
            proxyProtocolClient = null;
        }
        final var user = Minecraft.getInstance().getUser();
        //#if MC >= 1.19.2
        final UUID uuid = user.getProfileId();
        //#else
        //$$ final UUID uuid = user.getGameProfile().getId();
        //#endif
        //noinspection ConstantValue
        if (uuid == null) {
            LOGGER.warn("Failed to get player UUID. Unable to use World Host.");
            if (failureToast) {
                WHToast.builder("world-host.wh_connect.not_available").show();
            }
            return;
        }
        LOGGER.info("Attempting to connect to WH server at {}", CONFIG.getServerIp());
        protoClient = new ProtocolClient(CONFIG.getServerIp(), successToast, failureToast);
        connectingFuture = protoClient.getConnectingFuture();
        protoClient.authenticate(uuid);
    }

    public static String getName(GameProfile profile) {
        return StringUtils.getIfBlank(profile.getName(), () -> profile.getId().toString());
    }

    public static GameProfileCache getProfileCache() {
        return profileCache;
    }

    public static CompletableFuture<ResourceLocation> getInsecureSkinLocation(GameProfile gameProfile) {
        final var skinManager = Minecraft.getInstance().getSkinManager();
        //#if MC >= 1.20.2
        return skinManager.getOrLoad(gameProfile).thenApply(PlayerSkin::texture);
        //#elseif MC >= 1.19.2
        //$$ return CompletableFuture.completedFuture(skinManager.getInsecureSkinLocation(gameProfile));
        //#else
        //$$ final MinecraftProfileTexture texture = skinManager.getInsecureSkinInformation(gameProfile)
        //$$     .get(MinecraftProfileTexture.Type.SKIN);
        //$$ return CompletableFuture.completedFuture(
        //$$     texture != null
        //$$         ? skinManager.registerTexture(texture, MinecraftProfileTexture.Type.SKIN)
        //$$         : DefaultPlayerSkin.getDefaultSkin(Player.createPlayerUUID(gameProfile))
        //$$ );
        //#endif
    }

    public static ResourceLocation getSkinLocationNow(GameProfile gameProfile) {
        final ResourceLocation location = getInsecureSkinLocation(gameProfile).getNow(null);
        if (location == null) {
            //#if MC >= 1.20.2
            return DefaultPlayerSkin.get(gameProfile).texture();
            //#else
            //$$ return DefaultPlayerSkin.getDefaultSkin(gameProfile.getId());
            //#endif
        }
        return location;
    }

    public static void getMaybeAsync(GameProfileCache cache, String name, Consumer<Optional<GameProfile>> action) {
        //#if MC >= 1.20.2
        cache.getAsync(name).thenAccept(action);
        //#else
        //$$ cache.getAsync(name, action);
        //#endif
    }

    public static GameProfile fetchProfile(MinecraftSessionService sessionService, UUID uuid, GameProfile fallback) {
        //#if MC < 1.20.2
        //$$ return sessionService.fillProfileProperties(fallback != null ? fallback : new GameProfile(uuid, null), false);
        //#else
        final var result = sessionService.fetchProfile(uuid, false);
        if (result == null) {
            return fallback != null ? fallback : new GameProfile(uuid, "");
        }
        return result.profile();
        //#endif
    }

    public static GameProfile fetchProfile(MinecraftSessionService sessionService, UUID uuid) {
        return fetchProfile(sessionService, uuid, null);
    }

    public static GameProfile fetchProfile(MinecraftSessionService sessionService, GameProfile profile) {
        return fetchProfile(sessionService, profile.getId(), profile);
    }

    public static boolean isFriend(UUID user) {
        return CONFIG.isEnableFriends() && CONFIG.getFriends().contains(user);
    }

    public static void showFriendOrOnlineToast(UUID user, String title, String description, int ticks, Runnable clickAction) {
        Util.backgroundExecutor().execute(() -> {
            final GameProfile profile = fetchProfile(Minecraft.getInstance().getMinecraftSessionService(), user);
            getInsecureSkinLocation(profile).thenAcceptAsync(skinTexture -> {
                WHToast.builder(Components.translatable(title, getName(profile)))
                    .description(Components.translatable(description))
                    .icon((context, x, y, width, height) -> {
                        RenderSystem.enableBlend();
                        WorldHostScreen.blit(context, skinTexture, x, y, width, height, 8, 8, 8, 8, 64, 64);
                        WorldHostScreen.blit(context, skinTexture, x, y, width, height, 40, 8, 8, 8, 64, 64);
                    })
                    .clickAction(clickAction)
                    .ticks(ticks)
                    .important()
                    .show();
            }, Minecraft.getInstance());
        });
    }

    public static FriendlyByteBuf createByteBuf() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    @SuppressWarnings("RedundantThrows")
    public static ServerStatus parseServerStatus(FriendlyByteBuf buf) throws IOException {
        //#if MC >= 1.20.5
        return ClientboundStatusResponsePacket.STREAM_CODEC.decode(buf).status();
        //#elseif MC >= 1.19.4
        //$$ return new ClientboundStatusResponsePacket(buf).status();
        //#else
        //$$ return new ClientboundStatusResponsePacket(buf).getStatus();
        //#endif
    }

    public static FriendlyByteBuf writeServerStatus(ServerStatus metadata) {
        if (metadata == null) {
            metadata = WorldHost.createEmptyServerStatus();
        }
        final FriendlyByteBuf buf = WorldHost.createByteBuf();
        //#if MC < 1.20.5
        //$$ new ClientboundStatusResponsePacket(metadata).write(buf);
        //#else
        ClientboundStatusResponsePacket.STREAM_CODEC.encode(buf, new ClientboundStatusResponsePacket(metadata));
        //#endif
        return buf;
    }

    public static ServerStatus createEmptyServerStatus() {
        //#if MC >= 1.19.4
        return new ServerStatus(
            Components.EMPTY, Optional.empty(), Optional.empty(), Optional.empty(), false
            //#if FORGELIKE && MC < 1.20.5
            //$$ , Optional.empty()
            //#elseif NEOFORGE
            //$$ , false
            //#endif
        );
        //#else
        //$$ return new ServerStatus();
        //#endif
    }

    @Nullable
    public static String getExternalIp() {
        if (protoClient == null) {
            return null;
        }
        if (proxyProtocolClient != null) {
            return getExternalIp0(proxyProtocolClient.getBaseAddr(), proxyProtocolClient.getMcPort());
        }
        if (protoClient.getBaseIp().isEmpty()) {
            return null;
        }
        return getExternalIp0(protoClient.getBaseIp(), protoClient.getBasePort());
    }

    private static String getExternalIp0(String baseIp, int basePort) {
        String ip = connectionIdToString(protoClient.getConnectionId()) + '.' + baseIp;
        if (basePort != 25565) {
            ip += ":" + basePort;
        }
        return ip;
    }

    public static void pingFriends() {
        ONLINE_FRIEND_PINGS.clear();
        if (protoClient != null) {
            protoClient.queryRequest(CONFIG.getFriends());
        }
    }

    public static String connectionIdToString(long connectionId) {
        if (connectionId < 0 || connectionId >= MAX_CONNECTION_IDS) {
            throw new IllegalArgumentException("Invalid connection ID " + connectionId);
        }
        if (CONFIG.isUseShortIp()) {
            return StringUtils.leftPad(Long.toString(connectionId, 36), 9, '0');
        }
        final int first = (int)(connectionId & 0x3fff);
        final int second = (int)(connectionId >>> 14) & 0x3fff;
        final int third = (int)(connectionId >>> 28) & 0x3fff;
        return wordsForCid.get(first) + '-' +
            wordsForCid.get(second) + '-' +
            wordsForCid.get(third);
    }

    @Nullable
    public static Long tryParseConnectionId(String str) {
        final String[] words = str.split("-");
        if (words.length != 3) {
            if (words.length == 1) {
                final String word = words[0];
                if (word.length() != 9) {
                    return null;
                }
                return Long.parseLong(word, 36);
            }
            return null;
        }
        long result = 0L;
        int shift = 0;
        for (final String word : words) {
            final int part = wordsForCidInverse.getInt(word);
            if (part == -1) {
                return null;
            }
            result |= (long)part << shift;
            shift += 14;
        }
        return result;
    }

    public static void join(long connectionId, @Nullable Screen parentScreen) {
        if (protoClient == null) {
            LOGGER.error("Tried to join {}, but protoClient == null!", connectionIdToString(connectionId));
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (parentScreen == null) {
            parentScreen = minecraft.screen;
        }
        protoClient.setAttemptingToJoin(connectionId);
        minecraft.setScreen(new JoiningWorldHostScreen(parentScreen));
        protoClient.requestDirectJoin(connectionId);
    }

    public static void connect(Screen parentScreen, long cid) {
        connect(
            parentScreen, cid,
            connectionIdToString(cid) + '.' + protoClient.getBaseIp(),
            protoClient.getBasePort()
        );
    }

    public static void connect(Screen parentScreen, long cid, String host, int port) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            minecraft.getSingleplayerServer().halt(false);
        }
        final ServerAddress serverAddress = new ServerAddress(host, port);
        ConnectScreen.startConnecting(
            parentScreen, minecraft, serverAddress,
            //#if MC < 1.20.0
            //$$ null
            //#else
            new ServerData(
                WorldHost.connectionIdToString(cid), serverAddress.toString(),
                //#if MC < 1.20.2
                //$$ false
                //#else
                ServerData.Type.OTHER
                //#endif
            ), false
            //#endif
            //#if MC >= 1.20.5
            , null
            //#endif
        );
    }

    private static int ipCommand(CommandContext<CommandSourceStack> ctx) {
        if (protoClient == null) {
            ctx.getSource().sendFailure(Components.translatable("world-host.worldhost.ip.not_connected"));
            return 0;
        }
        final String externalIp = getExternalIp();
        if (externalIp == null) {
            ctx.getSource().sendFailure(Components.translatable("world-host.worldhost.ip.no_server_support"));
            return 0;
        }
        ctx.getSource().sendSuccess(
            //#if MC >= 1.20.0
            () ->
            //#endif
            Components.translatable(
                "world-host.worldhost.ip.success",
                Components.copyOnClickText(externalIp)
            ),
            false
        );
        return Command.SINGLE_SUCCESS;
    }

    public static void proxyConnect(long connectionId, InetAddress remoteAddr, Supplier<ProxyPassthrough> proxy) {
        final var server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null || !server.isPublished()) {
            if (protoClient != null) {
                protoClient.proxyDisconnect(connectionId);
            }
            return;
        }
        try {
            final ProxyClient proxyClient = new ProxyClient(remoteAddr, connectionId, proxy);
            WorldHost.CONNECTED_PROXY_CLIENTS.put(connectionId, proxyClient);
            proxyClient.start();
        } catch (IOException e) {
            WorldHost.LOGGER.error("Failed to start ProxyClient", e);
        }
    }

    public static void proxyPacket(long connectionId, byte[] data) {
        final ProxyClient proxyClient = WorldHost.CONNECTED_PROXY_CLIENTS.get(connectionId);
        if (proxyClient != null) {
            proxyClient.send(data);
        } else {
            WorldHost.LOGGER.warn("Received packet for unknown connection {}", connectionId);
        }
    }

    public static void proxyDisconnect(long connectionId) {
        final ProxyClient proxyClient = WorldHost.CONNECTED_PROXY_CLIENTS.remove(connectionId);
        if (proxyClient != null) {
            proxyClient.close();
        } else {
            WorldHost.LOGGER.warn("Received disconnect from unknown connection {}", connectionId);
        }
    }

    public static String getModVersion(String modId) {
        //#if FABRIC
        return FabricLoader.getInstance()
            .getModContainer(modId)
            .orElseThrow(() -> new IllegalStateException("Couldn't find mod " + modId))
            .getMetadata()
            .getVersion()
            .getFriendlyString();
        //#else
        //$$ return ModList.get()
        //$$     .getModContainerById(modId)
        //$$     .orElseThrow(() -> new IllegalStateException("Couldn't find mod " + modId))
        //$$     .getModInfo()
        //$$     .getVersion()
        //$$     .toString();
        //#endif
    }

    public static int getMMCLines(boolean isPause) {
        //#if FABRIC
        if (FabricLoader.getInstance().isModLoaded("isxander-main-menu-credits")) {
            final var baseConfig = MainMenuCredits.getInstance().getConfig();
            final var config = isPause ? baseConfig.PAUSE_MENU : baseConfig.MAIN_MENU;
            return (CONFIG.getOnlineStatusLocation() == OnlineStatusLocation.RIGHT ? config.getBottomRight() : config.getBottomLeft()).size();
        }
        //#endif
        return 0;
    }

    public static <T> T httpGet(
        CloseableHttpClient client,
        String baseUri,
        Consumer<URIBuilder> buildAction,
        IOFunction<InputStream, T> handler
    ) throws IOException {
        final URI uri;
        try {
            final URIBuilder uriBuilder = new URIBuilder(baseUri);
            buildAction.accept(uriBuilder);
            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        try (var response = client.execute(new HttpGet(uri))) {
            final var status = response.getStatusLine();
            if (status.getStatusCode() != 200) {
                throw new IOException("Failed to GET " + uri + ": " + status.getStatusCode() + " " + status.getReasonPhrase());
            }
            final var entity = response.getEntity();
            if (entity == null) {
                throw new IOException("GET " + uri + " returned no body.");
            }
            try (InputStream is = response.getEntity().getContent()) {
                return handler.apply(is);
            }
        }
    }

    public static CompletableFuture<Optional<String>> checkForUpdates() {
        return CompletableFuture.<Optional<String>>supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                final String latestVersion = httpGet(
                    client, "https://api.modrinth.com/v2/project/world-host/version",
                    builder -> builder
                        .addParameter("game_versions", "[\"" + getModVersion("minecraft") + "\"]")
                        .addParameter("loaders", "[\"" + MOD_LOADER + "\"]"),
                    input -> {
                        try (JsonReader reader = JsonReader.json(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                            reader.beginArray();
                            if (!reader.hasNext()) {
                                return null;
                            }
                            reader.beginObject();
                            while (reader.hasNext()) {
                                final String key = reader.nextName();
                                if (!key.equals("version_number")) {
                                    reader.skipValue();
                                    continue;
                                }
                                return reader.nextString();
                            }
                            return null;
                        }
                    }
                );
                if (latestVersion == null) {
                    return Optional.empty();
                }
                if (new Semver(getModVersion(MOD_ID)).compareTo(new Semver(latestVersion)) >= 0) {
                    return Optional.empty();
                }
                return Optional.of(latestVersion);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, Util.ioPool()).exceptionally(t -> {
            LOGGER.error("Failed to check for updates", t);
            return Optional.empty();
        });
    }

    private static Path getGameDir() {
        //#if FABRIC
        return FabricLoader.getInstance().getGameDir();
        //#else
        //$$ return FMLPaths.GAMEDIR.get();
        //#endif
    }

    public static ChannelInitializer<Channel> createChannelInitializer(ServerConnectionListener listener) {
        try {
            return channelInitializerConstructor.newInstance(listener);
        } catch (ReflectiveOperationException e) {
            // TODO: UncheckedReflectiveOperationException when 1.20.4+ becomes the minimum
            throw new RuntimeException(e);
        }
    }

    //#if FORGELIKE
    //#if MC >= 1.20.5
    //$$ @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    //#else
    //$$ @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    //#endif
    //$$ public static class ClientModEvents {
    //$$     // Forge 47.1.3 can call FMLClientSetupEvent twice.
    //$$     private static boolean initialized = false;
    //$$
    //$$     @SubscribeEvent
    //$$     public static synchronized void onClientSetup(FMLClientSetupEvent event) {
    //$$         if (initialized) return;
    //$$         initialized = true;
    //$$         init();
    //$$         final BiFunction<Minecraft, Screen, Screen> screenFunction =
    //$$             (mc, screen) -> new WorldHostConfigScreen(screen);
    //$$         ModLoadingContext.get().registerExtensionPoint(
                //#if MC >= 1.20.5
                //$$ IConfigScreenFactory.class, () -> screenFunction::apply
                //#elseif MC >= 1.19.2
                //$$ ConfigScreenHandler.ConfigScreenFactory.class,
                //$$ () -> new ConfigScreenHandler.ConfigScreenFactory(screenFunction)
                //#else
                //$$ ConfigGuiHandler.ConfigGuiFactory.class,
                //$$ () -> new ConfigGuiHandler.ConfigGuiFactory(screenFunction)
                //#endif
    //$$         );
    //$$     }
    //$$ }
    //#endif
}
