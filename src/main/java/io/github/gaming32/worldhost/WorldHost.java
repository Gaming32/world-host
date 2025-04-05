package io.github.gaming32.worldhost;

import com.demonwav.mcdev.annotations.Translatable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import io.github.gaming32.worldhost.config.WorldHostConfig;
import io.github.gaming32.worldhost.ext.ServerDataExt;
import io.github.gaming32.worldhost.gui.OnlineStatusLocation;
import io.github.gaming32.worldhost.gui.screen.FriendsScreen;
import io.github.gaming32.worldhost.gui.screen.JoiningWorldHostScreen;
import io.github.gaming32.worldhost.gui.screen.OnlineFriendsScreen;
import io.github.gaming32.worldhost.mixin.MinecraftAccessor;
import io.github.gaming32.worldhost.origincheck.OriginCheckers;
import io.github.gaming32.worldhost.plugin.FriendAdder;
import io.github.gaming32.worldhost.plugin.InfoTextsCategory;
import io.github.gaming32.worldhost.plugin.OnlineFriend;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.plugin.WorldHostPlugin;
import io.github.gaming32.worldhost.plugin.vanilla.GameProfileProfileInfo;
import io.github.gaming32.worldhost.protocol.ProtocolClient;
import io.github.gaming32.worldhost.protocol.proxy.ProxyPassthrough;
import io.github.gaming32.worldhost.protocol.proxy.ProxyProtocolClient;
import io.github.gaming32.worldhost.protocol.punch.PunchManager;
import io.github.gaming32.worldhost.proxy.ProxyClient;
import io.github.gaming32.worldhost.toast.WHToast;
import io.github.gaming32.worldhost.upnp.Gateway;
import io.github.gaming32.worldhost.upnp.GatewayFinder;
import io.github.gaming32.worldhost.versions.Components;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.players.GameProfileCache;
import org.apache.commons.io.function.IOConsumer;
import org.apache.commons.io.function.IOFunction;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.JsonWriter;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.literal;

//#if FABRIC
import dev.isxander.mainmenucredits.MainMenuCredits;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
//#else
//$$ import io.github.gaming32.worldhost.gui.screen.WorldHostConfigScreen;
//$$ import java.lang.annotation.ElementType;
//$$ import java.lang.reflect.InvocationTargetException;
//$$ import java.util.Objects;
//$$ import java.util.function.BiConsumer;
//$$ import org.objectweb.asm.Type;
//#if FORGE
//$$ import net.minecraftforge.fml.ModContainer;
//$$ import net.minecraftforge.fml.ModList;
//$$ import net.minecraftforge.fml.ModLoadingContext;
//$$ import net.minecraftforge.fml.common.Mod;
//$$ import net.minecraftforge.fml.loading.FMLPaths;
//$$ import net.minecraftforge.fml.loading.LoadingModList;
//$$ import net.minecraftforge.internal.BrandingControl;
//#else
//$$ import net.neoforged.fml.ModContainer;
//$$ import net.neoforged.fml.ModList;
//$$ import net.neoforged.fml.common.Mod;
//$$ import net.neoforged.fml.loading.FMLPaths;
//$$ import net.neoforged.neoforge.internal.BrandingControl;
//#endif
//#if MC >= 1.20.5
//$$ import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
//#elseif NEOFORGE
//$$ import net.neoforged.neoforge.client.ConfigScreenHandler;
//#else
//$$ import net.minecraftforge.client.ConfigScreenHandler;
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
    public static final Path GLOBAL_CONFIG_DIR = locateGlobalConfigDir();

    public static final Path CONFIG_FILE = CONFIG_DIR.resolve("world-host.json5");
    public static final Path OLD_CONFIG_FILE = CONFIG_DIR.resolve("world-host.json");

    public static final Path FRIENDS_FILE = GLOBAL_CONFIG_DIR.resolve("friends.json");
    public static final Path OLD_FRIENDS_FILE = CONFIG_DIR.resolve("world-host-friends.json");

    public static final WorldHostConfig CONFIG = new WorldHostConfig();

    private static List<String> wordsForCid;
    private static Object2IntMap<String> wordsForCidInverse;

    public static final long MAX_CONNECTION_IDS = 1L << 42;

    public static final Map<UUID, OnlineFriend> ONLINE_FRIENDS = new LinkedHashMap<>();
    public static final Map<UUID, ServerStatus> ONLINE_FRIEND_PINGS = new HashMap<>();
    public static final Set<FriendsListUpdate> ONLINE_FRIEND_UPDATES = Collections.newSetFromMap(new WeakHashMap<>());

    public static final Long2ObjectMap<ProxyClient> CONNECTED_PROXY_CLIENTS = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    public static final long CONNECTION_ID = new SecureRandom().nextLong(MAX_CONNECTION_IDS);

    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .executor(Util.ioPool())
        .build();

    private static boolean hasScannedForUpnp;
    @Nullable
    public static Gateway upnpGateway;

    private static GameProfileCache profileCache;

    @Nullable
    public static ProtocolClient protoClient;
    @Nullable
    public static ProxyProtocolClient proxyProtocolClient;
    public static int reconnectDelay = 0;
    private static int delayIndex = 0;
    @Nullable
    private static Future<Void> connectingFuture;

    public static boolean shareWorldOnLoad;

    @Nullable
    public static SocketAddress proxySocketAddress;

    public static boolean clientLoadedFully;
    public static long tickCount;

    private static List<LoadedWorldHostPlugin> plugins;

    //#if FABRIC
    @Override
    public void onInitializeClient() {
        final var container = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        init(
            path -> container.findPath(path).orElseThrow(() -> new NoSuchFileException(path)),
            container.getOrigin().getPaths().getFirst()
        );
    }
    //#else
    //$$ public WorldHost(
        //#if NEOFORGE
        //$$ ModContainer container
        //#endif
    //$$ ) {
        //#if FORGE
        //$$ final ModContainer container = ModLoadingContext.get().getActiveContainer();
        //#endif
    //$$     final var modFile = container.getModInfo().getOwningFile().getFile();
    //$$     init(path -> modFile.findResource(path.split("/")), modFile.getFilePath());
    //$$     container.registerExtensionPoint(
            //#if MC >= 1.20.5
            //$$ IConfigScreenFactory.class, (ignored, screen) -> new WorldHostConfigScreen(screen)
            //#else
            //$$ ConfigScreenHandler.ConfigScreenFactory.class,
            //$$ () -> new ConfigScreenHandler.ConfigScreenFactory((ignored, screen) -> new WorldHostConfigScreen(screen))
            //#endif
    //$$     );
    //$$ }
    //#endif

    private static void init(IOFunction<String, Path> assetGetter, Path modPath) {
        try (BufferedReader reader = Files.newBufferedReader(
            assetGetter.apply("16k.txt"), StandardCharsets.US_ASCII
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
        prepareFileWatcher();

        final var nonstandardOrigins = OriginCheckers.getNonstandardOriginsOnce(OriginCheckers.NATIVE_CHECKER, modPath);
        if (!nonstandardOrigins.isEmpty()) {
            LOGGER.warn("Found nonstandard download origins: {}", nonstandardOrigins);
            WHToast.builder("world-host.nonstandard_origin")
                .description(Component.translatable(
                    "world-host.nonstandard_origin.desc",
                    nonstandardOrigins.stream()
                        .map(URI::getHost)
                        .distinct()
                        .collect(Collectors.joining(", "))
                ))
                .important()
                .ticks(200)
                .show();
        }

        try {
            Files.createDirectories(CACHE_DIR);
        } catch (IOException e) {
            LOGGER.error("Failed to create cache directory", e);
        }
        profileCache = new GameProfileCache(
            ((MinecraftAccessor)Minecraft.getInstance()).getAuthenticationService().createProfileRepository(),
            CACHE_DIR.resolve("usercache.json").toFile()
        );
        profileCache.setExecutor(Minecraft.getInstance());

        plugins = ImmutableList.sortedCopyOf(collectPlugins());
        LOGGER.info(
            "Found {} World Host plugin(s): {}",
            plugins.size(),
            plugins.stream()
                .map(plugin -> plugin.modId() + " (" + plugin.plugin() + ")")
                .collect(Collectors.joining(", "))
        );

        for (final LoadedWorldHostPlugin plugin : plugins) {
            plugin.plugin().init();
        }

        if (CONFIG.isUPnP()) {
            scanUpnp();
        }

        Runtime.getRuntime().addShutdownHook(
            Thread.ofPlatform()
                .name("World Host Shutdown Thread")
                .unstarted(WorldHost::shutdownClients)
        );

        reconnect(false, true);
    }

    private static Path locateGlobalConfigDir() {
        return switch (Util.getPlatform()) {
            case WINDOWS -> Path.of(System.getenv("APPDATA"), "World Host Mod");
            case OSX -> Path.of(System.getProperty("user.home"), "Library/Application Support/World Host Mod");
            default -> {
                var configHome = System.getenv("XDG_CONFIG_HOME");
                if (configHome == null) {
                    configHome = System.getProperty("user.home") + "/.config";
                }
                yield Path.of(configHome, "world-host-mod");
            }
        };
    }

    public static void loadConfig() {
        loadConfigFile(CONFIG_FILE, JsonReader::json5, OLD_CONFIG_FILE, JsonReader::json, CONFIG::read);
        loadFriendsOnly();
        saveConfig();
    }

    private static void loadFriendsOnly() {
        loadConfigFile(FRIENDS_FILE, JsonReader::json, OLD_FRIENDS_FILE, JsonReader::json, CONFIG::readFriends);
    }

    @Contract("_, _, !null, null, _ -> fail")
    private static void loadConfigFile(
        Path configFile, IOFunction<Path, JsonReader> configFormat,
        @Nullable Path oldConfigFile, @Nullable IOFunction<Path, JsonReader> oldConfigFormat,
        IOConsumer<JsonReader> configReader
    ) {
        try (JsonReader reader = configFormat.apply(configFile)) {
            configReader.accept(reader);
            if (oldConfigFile != null && Files.exists(oldConfigFile)) {
                LOGGER.info("Old {} still exists. Consider removing it.", oldConfigFile.getFileName());
            }
        } catch (NoSuchFileException e) {
            if (oldConfigFile != null) {
                assert oldConfigFormat != null;
                LOGGER.info("{} not found. Trying to load old {}.", configFile.getFileName(), oldConfigFile.getFileName());
                try (JsonReader reader = oldConfigFormat.apply(oldConfigFile)) {
                    configReader.accept(reader);
                    LOGGER.info(
                        "Found and read old {} into new {}. Consider removing the old {}.",
                        oldConfigFile.getFileName(), configFile.getFileName(), oldConfigFile.getFileName()
                    );
                } catch (NoSuchFileException e1) {
                    LOGGER.info("Old {} not found. Writing default config.", oldConfigFile.getFileName());
                } catch (IOException e1) {
                    LOGGER.error("Failed to load old {}.", oldConfigFile.getFileName(), e1);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load {}.", configFile.getFileName(), e);
        }
    }

    public static void saveConfig() {
        saveConfigFile(CONFIG_FILE, JsonWriter::json5, CONFIG::write);
        saveConfigFile(FRIENDS_FILE, JsonWriter::json, CONFIG::writeFriends);
    }

    private static void saveConfigFile(
        Path configFile, IOFunction<Path, JsonWriter> configFormat,
        IOConsumer<JsonWriter> configWriter
    ) {
        try {
            Files.createDirectories(configFile.getParent());
            try (JsonWriter writer = configFormat.apply(configFile)) {
                configWriter.accept(writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write {}.", configFile.getFileName(), e);
        }
    }

    private static void prepareFileWatcher() {
        try {
            final var watchService = GLOBAL_CONFIG_DIR.getFileSystem().newWatchService();
            GLOBAL_CONFIG_DIR.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            Thread.ofVirtual().name("WorldHostFileWatcher").start(() -> {
                try {
                    while (Minecraft.getInstance().isRunning() || !clientLoadedFully) {
                        final var key = watchService.take();
                        for (final var event : key.pollEvents()) {
                            final var eventPath = GLOBAL_CONFIG_DIR.resolve((Path)event.context());
                            if (eventPath.equals(FRIENDS_FILE)) {
                                LOGGER.info("Friends file modified. Reloading...");
                                Minecraft.getInstance().execute(() -> {
                                    final var oldFriends = Set.copyOf(CONFIG.getFriends());
                                    loadFriendsOnly();
                                    fullFriendsRefresh(oldFriends);
                                });
                            }
                        }
                        key.reset();
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception in file watcher. Stopping.", e);
                }
                try {
                    watchService.close();
                } catch (IOException e) {
                    LOGGER.error("Exception closing file watcher", e);
                }
            });
        } catch (Exception e) {
            LOGGER.warn(
                "Failed to setup watch service for {}. Friends setup in other instances will not be dynamically refreshed.",
                FRIENDS_FILE, e
            );
        }
    }

    public static void setFriends(Set<UUID> friends) {
        final var oldFriends = Set.copyOf(CONFIG.getFriends());
        CONFIG.getFriends().clear();
        CONFIG.getFriends().addAll(friends);
        fullFriendsRefresh(oldFriends);
    }

    public static void fullFriendsRefresh(Set<UUID> oldFriends) {
        if (!CONFIG.isEnableFriends()) return;
        final var friends = CONFIG.getFriends();
        final var removedFriends = Sets.difference(oldFriends, friends);
        final var newFriends = Sets.difference(oldFriends, friends);
        if (protoClient != null) {
            final var server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null && server.isPublished()) {
                if (!removedFriends.isEmpty()) {
                    protoClient.closedWorld(removedFriends);
                }
                if (!newFriends.isEmpty()) {
                    protoClient.publishedWorld(newFriends);
                }
            }
            refreshFriendsList();
            if (Minecraft.getInstance().screen instanceof FriendsScreen friendsScreen) {
                friendsScreen.refresh();
            }
        }
    }

    private static List<LoadedWorldHostPlugin> collectPlugins() {
        //#if FABRIC
        return FabricLoader.getInstance()
            .getEntrypointContainers("worldhost", WorldHostPlugin.class)
            .stream()
            .map(container -> new LoadedWorldHostPlugin(
                container.getProvider().getMetadata().getId(),
                container.getEntrypoint()
            ))
            .toList();
        //#else
        //$$ final Type entrypointAnnotationType = Type.getType(WorldHostPlugin.Entrypoint.class);
        //$$ return ModList.get()
        //$$     .getModFiles()
        //$$     .stream()
        //$$     .flatMap(modFile -> modFile
        //$$         .getFile()
        //$$         .getScanResult()
        //$$         .getAnnotations()
        //$$         .stream()
        //$$         .filter(ad -> ad.targetType() == ElementType.TYPE && ad.annotationType().equals(entrypointAnnotationType))
        //$$         .map(ad -> {
        //$$             try {
        //$$                 return (WorldHostPlugin)Class.forName(ad.clazz().getClassName()).getDeclaredConstructor().newInstance();
        //$$             } catch (ReflectiveOperationException e) {
        //$$                 throw new IllegalStateException(
        //$$                     "World Host plugin from " + modFile.moduleName() + " failed to load",
        //$$                     e instanceof InvocationTargetException target ? target.getTargetException() : e
        //$$                 );
        //$$             }
        //$$         })
        //$$         .filter(Objects::nonNull)
        //$$         .map(plugin -> new LoadedWorldHostPlugin(modFile.getMods().getFirst().getModId(), plugin))
        //$$     )
        //$$     .toList();
        //#endif
    }

    public static List<LoadedWorldHostPlugin> getPlugins() {
        return plugins;
    }

    public static List<Component> getInfoTexts(InfoTextsCategory category) {
        final List<Component> result = new ArrayList<>();
        for (final LoadedWorldHostPlugin plugin : plugins) {
            result.addAll(plugin.plugin().getInfoTexts(category));
        }
        return result;
    }

    public static List<FriendAdder> getFriendAdders() {
        return plugins.stream()
            .map(LoadedWorldHostPlugin::plugin)
            .map(WorldHostPlugin::friendAdder)
            .flatMap(Optional::stream)
            .toList();
    }

    public static void friendWentOnline(OnlineFriend friend) {
        ONLINE_FRIENDS.put(friend.uuid(), friend);
        ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
        if (!CONFIG.isAnnounceFriendsOnline()) return;
        if (Minecraft.getInstance().screen instanceof OnlineFriendsScreen) return;
        showFriendOrOnlineToast(
            friend.profileInfo(), "world-host.went_online", "world-host.went_online.desc", 200,
            !friend.joinability().canJoin() ? null : () -> friend.joinWorld(Minecraft.getInstance().screen)
        );
    }

    public static void tickHandler() {
        tickCount++;
        PunchManager.retransmitAll();
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
                assert protoClient != null;
                protoClient.publishedWorld(CONFIG.getFriends());
            }
        }
    }

    public static void refreshFriendsList() {
        LOGGER.info("Refreshing friends list...");
        ONLINE_FRIENDS.clear();
        WorldHost.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
        for (final LoadedWorldHostPlugin plugin : plugins) {
            plugin.plugin().refreshOnlineFriends();
        }
    }

    public static void commandRegistrationHandler(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("worldhost")
            .then(literal("ip")
                .requires(s -> s.getServer().isPublished())
                .executes(WorldHost::ipCommand)
            )
            .then(literal("tempip")
                .requires(s ->
                    CONFIG.isUPnP() &&
                    s.getServer().isPublished() &&
                    upnpGateway != null &&
                    protoClient != null &&
                    !protoClient.getUserIp().isEmpty()
                )
                .executes(ctx -> {
                    // TODO: Improve this command to maybe use less requires
                    assert upnpGateway != null;
                    assert protoClient != null;
                    try {
                        final int port = ctx.getSource().getServer().getPort();
                        final var error = upnpGateway.openPort(port, 60, false);
                        if (error == null) {
                            ctx.getSource().sendSuccess(
                                //#if MC >= 1.20.0
                                () ->
                                //#endif
                                Component.translatable(
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
                    ctx.getSource().sendFailure(Component.translatable(
                        "world-host.worldhost.tempip.failure",
                        ComponentUtils.wrapInSquareBrackets(Component.literal("/worldhost ip")).withStyle(style -> style
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(
                                //#if MC >= 1.21.5
                                new ClickEvent.SuggestCommand("/worldhost ip")
                                //#else
                                //$$ new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/worldhost ip")
                                //#endif
                            )
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

    public static boolean hasScannedForUpnp() {
        return hasScannedForUpnp;
    }

    public static void reconnect(boolean successToast, boolean failureToast) {
        shutdownClients();
        LOGGER.info("Attempting to connect to WH server at {}", CONFIG.getServerIp());
        protoClient = new ProtocolClient(CONFIG.getServerIp(), successToast, failureToast);
        connectingFuture = protoClient.getConnectingFuture();
        protoClient.authenticate(Minecraft.getInstance().getUser());
    }

    public static void shutdownClients() {
        if (protoClient != null) {
            protoClient.close();
        }
        if (proxyProtocolClient != null) {
            proxyProtocolClient.close();
        }
        if (protoClient != null) {
            try {
                protoClient.getShutdownFuture().get(5L, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOGGER.error("Failed to wait for protocol client shutdown", e);
            }
        }
        protoClient = null;
        if (proxyProtocolClient != null) {
            try {
                proxyProtocolClient.getShutdownFuture().get(5L, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOGGER.error("Failed to wait for proxy protocol client shutdown", e);
            }
        }
        proxyProtocolClient = null;
    }

    public static GameProfileCache getProfileCache() {
        return profileCache;
    }

    public static WHPlayerSkin getInsecureSkin(GameProfile profile) {
        return WHPlayerSkin.fromSkinManager(Minecraft.getInstance().getSkinManager(), profile);
    }

    public static ResourceLocation getSkinLocationNow(GameProfile profile) {
        return getInsecureSkin(profile).texture();
    }

    public static void getMaybeAsync(GameProfileCache cache, String name, Consumer<Optional<GameProfile>> action) {
        //#if MC >= 1.20.2
        cache.getAsync(name).thenAccept(action);
        //#else
        //$$ cache.getAsync(name, action);
        //#endif
    }

    public static GameProfile fetchProfile(MinecraftSessionService sessionService, UUID uuid, @Nullable GameProfile fallback) {
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

    public static CompletableFuture<GameProfile> resolveGameProfile(GameProfile profile) {
        if (profile.getId().version() != 4) {
            return CompletableFuture.completedFuture(profile);
        }
        return CompletableFuture.supplyAsync(
            () -> WorldHost.fetchProfile(Minecraft.getInstance().getMinecraftSessionService(), profile),
            //#if MC >= 1.20.4
            Util.nonCriticalIoPool()
            //#else
            //$$ Util.ioPool()
            //#endif
        );
    }

    public static CompletableFuture<ProfileInfo> resolveProfileInfo(GameProfile profile) {
        return resolveGameProfile(profile).thenApply(GameProfileProfileInfo::new);
    }

    public static boolean isFriend(UUID user) {
        return CONFIG.isEnableFriends() && CONFIG.getFriends().contains(user);
    }

    public static void addFriends(UUID... friends) {
        addFriends(List.of(friends));
    }

    public static void addFriends(Collection<UUID> friends) {
        CONFIG.getFriends().addAll(friends);
        saveConfig();
        final var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null && server.isPublished() && protoClient != null) {
            protoClient.publishedWorld(friends);
        }
    }

    public static void showFriendOrOnlineToast(
        CompletableFuture<ProfileInfo> profileFuture,
        @Translatable String title,
        @Translatable String description,
        int ticks,
        @Nullable Runnable clickAction
    ) {
        profileFuture.thenAccept(profile ->
            WHToast.builder(Component.translatable(title, profile.name()))
                .description(Component.translatable(description))
                .icon(profile.iconRenderer())
                .clickAction(clickAction)
                .ticks(ticks)
                .important()
                .show()
        );
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

    public static FriendlyByteBuf writeServerStatus(@Nullable ServerStatus metadata) {
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
            CommonComponents.EMPTY, Optional.empty(), Optional.empty(), Optional.empty(), false
            //#if FORGELIKE && MC < 1.20.4
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
        assert protoClient != null;
        String ip = connectionIdToString(protoClient.getConnectionId()) + '.' + baseIp;
        if (basePort != 25565) {
            ip += ":" + basePort;
        }
        return ip;
    }

    public static void pingFriends() {
        ONLINE_FRIEND_PINGS.clear();
        if (ONLINE_FRIENDS.isEmpty()) return;
        for (final LoadedWorldHostPlugin plugin : plugins) {
            plugin.plugin().pingFriends(ONLINE_FRIENDS.values());
        }
    }

    public static void pingFriends(Collection<OnlineFriend> friends) {
        if (friends.isEmpty()) return;
        for (final OnlineFriend friend : friends) {
            ONLINE_FRIEND_PINGS.remove(friend.uuid());
        }
        for (final LoadedWorldHostPlugin plugin : plugins) {
            plugin.plugin().pingFriends(friends);
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

    public static void join(long connectionId, Screen parentScreen) {
        if (protoClient == null) {
            LOGGER.error("Tried to join {}, but protoClient == null!", connectionIdToString(connectionId));
            return;
        }
        protoClient.setAttemptingToJoin(connectionId);
        Minecraft.getInstance().setScreen(new JoiningWorldHostScreen(parentScreen));
        protoClient.requestDirectJoin(connectionId);
    }

    public static void connect(Screen parentScreen, long cid) {
        if (protoClient == null) {
            LOGGER.error("Tried to connect to {}, but protoClient == null!", connectionIdToString(cid));
            return;
        }
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
        final var serverData = new ServerData(
            WorldHost.connectionIdToString(cid), serverAddress.toString(),
            //#if MC < 1.20.2
            //$$ false
            //#else
            ServerData.Type.OTHER
            //#endif
        );
        ((ServerDataExt)serverData).wh$setConnectionId(cid);
        ConnectScreen.startConnecting(
            parentScreen, minecraft, serverAddress, serverData
            //#if MC >= 1.20.1
            , false
            //#endif
            //#if MC >= 1.20.5
            , null
            //#endif
        );
    }

    private static int ipCommand(CommandContext<CommandSourceStack> ctx) {
        if (protoClient == null) {
            ctx.getSource().sendFailure(Component.translatable("world-host.worldhost.ip.not_connected"));
            return 0;
        }
        final String externalIp = getExternalIp();
        if (externalIp == null) {
            ctx.getSource().sendFailure(Component.translatable("world-host.worldhost.ip.no_server_support"));
            return 0;
        }
        ctx.getSource().sendSuccess(
            //#if MC >= 1.20.0
            () ->
            //#endif
            Component.translatable(
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

    public static boolean isModLoaded(String modId) {
        //#if FABRIC
        return FabricLoader.getInstance().isModLoaded(modId);
        //#else
        //$$ return ModList.get().isLoaded(modId);
        //#endif
    }

    public static int getMenuLines(boolean isPause, OnlineStatusLocation side) {
        //#if FABRIC
        if (!isModLoaded("isxander-main-menu-credits")) {
            return 0;
        }
        final var baseConfig = MainMenuCredits.getInstance().getConfig();
        final var config = isPause ? baseConfig.PAUSE_MENU : baseConfig.MAIN_MENU;
        return (side == OnlineStatusLocation.RIGHT ? config.getBottomRight() : config.getBottomLeft()).size();
        //#else
        //$$ if (isPause) {
        //$$     return 0;
        //$$ }
        //$$ int[] forgeLineCount = {-1};
        //$$ final BiConsumer<Integer, String> lineConsumer = (i, s) -> forgeLineCount[0]++;
        //$$ if (side == OnlineStatusLocation.LEFT) {
        //$$     BrandingControl.forEachLine(true, true, lineConsumer);
        //$$ } else {
        //$$     BrandingControl.forEachAboveCopyrightLine(lineConsumer);
        //$$     forgeLineCount[0]++;
        //$$ }
        //$$ return forgeLineCount[0];
        //#endif
    }

    public static int getMenuLineSpacing() {
        //#if FABRIC
        return 12;
        //#else
        //$$ return 10;
        //#endif
    }

    public static <T> CompletableFuture<T> httpGet(
        String baseUri,
        Consumer<URIBuilder> buildAction,
        IOFunction<InputStream, T> handler
    ) {
        final URI uri;
        try {
            final URIBuilder uriBuilder = new URIBuilder(baseUri);
            buildAction.accept(uriBuilder);
            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .header("User-Agent", "World Host/" + getModVersion(MOD_ID))
            .GET()
            .build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
            .thenComposeAsync(response -> {
                if (response.statusCode() != 200) {
                    final String reason = EnglishReasonPhraseCatalog.INSTANCE.getReason(response.statusCode(), null);
                    return CompletableFuture.failedFuture(new IOException(
                        "Failed to GET " + response.request().uri() + ": " + response.statusCode() + " " + reason
                    ));
                }
                try {
                    return CompletableFuture.completedFuture(handler.apply(response.body()));
                } catch (Throwable t) {
                    return CompletableFuture.failedFuture(t);
                }
            }, Util.ioPool());
    }

    private static Path getGameDir() {
        //#if FABRIC
        return FabricLoader.getInstance().getGameDir();
        //#else
        //$$ return FMLPaths.GAMEDIR.get();
        //#endif
    }

    public static UUID getUserId() {
        final var user = Minecraft.getInstance().getUser();
        //#if MC >= 1.20.4
        return user.getProfileId();
        //#else
        //$$ return user.getGameProfile().getId();
        //#endif
    }
}
