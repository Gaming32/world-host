package io.github.gaming32.worldhost;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.gaming32.worldhost.gui.screen.JoiningWorldHostScreen;
import io.github.gaming32.worldhost.protocol.ProtocolClient;
import io.github.gaming32.worldhost.protocol.proxy.ProxyPassthrough;
import io.github.gaming32.worldhost.protocol.proxy.ProxyProtocolClient;
import io.github.gaming32.worldhost.toast.WHToast;
import io.github.gaming32.worldhost.upnp.Gateway;
import io.github.gaming32.worldhost.upnp.GatewayFinder;
import io.github.gaming32.worldhost.upnp.UPnPErrors;
import io.github.gaming32.worldhost.versions.Components;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.players.GameProfileCache;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonWriter;

import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.literal;

//#if MC >= 11800
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
//#else
//$$ import org.apache.logging.log4j.LogManager;
//$$ import org.apache.logging.log4j.Logger;
//#endif

//#if MC >= 11902
import io.github.gaming32.worldhost.mixin.MinecraftAccessor;
import net.minecraft.server.Services;
//#else
//$$ import com.mojang.authlib.minecraft.MinecraftProfileTexture;
//$$ import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
//$$ import net.minecraft.client.resources.DefaultPlayerSkin;
//$$ import net.minecraft.world.entity.player.Player;
//#endif

//#if FABRIC
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
//#if MC >= 11802
import dev.isxander.mainmenucredits.MainMenuCredits;
import dev.isxander.mainmenucredits.config.MMCConfig;
import dev.isxander.mainmenucredits.config.MMCConfigEntry;
import io.github.gaming32.worldhost.gui.OnlineStatusLocation;
//#endif
//#else
//$$ import io.github.gaming32.worldhost.gui.screen.WorldHostConfigScreen;
//$$ import net.minecraft.client.gui.screens.Screen;
//$$ import net.minecraft.server.packs.PackType;
//$$ import net.minecraftforge.api.distmarker.Dist;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import net.minecraftforge.fml.ModList;
//$$ import net.minecraftforge.fml.ModLoadingContext;
//$$ import net.minecraftforge.fml.common.Mod;
//$$ import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//$$ import java.util.function.BiFunction;
//#if MC >= 11902
//$$ import net.minecraftforge.client.ConfigScreenHandler;
//#elseif MC >= 11802
//$$ import net.minecraftforge.client.ConfigGuiHandler;
//#else
//$$ import net.minecraftforge.fml.ExtensionPoint;
//#endif
//#if MC > 11605
//$$ import net.minecraftforge.resource.ResourcePackLoader;
//#else
//$$ import net.minecraftforge.fml.packs.ResourcePackLoader;
//#endif
//#endif

//#if FORGE
//$$ @Mod(WorldHost.MOD_ID)
//#endif
public class WorldHost
    //#if FABRIC
    implements ClientModInitializer
    //#endif
{
    public static final String MOD_ID =
        //#if FORGE
        //$$ "world_host";
        //#else
        "world-host";
        //#endif

    public static final Logger LOGGER =
        //#if MC >= 11800
        LogUtils.getLogger();
        //#else
        //$$ LogManager.getLogger();
        //#endif

    public static final Loader MOD_LOADER =
        //#if FORGE
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

    public static final File GAME_DIR = Minecraft.getInstance().gameDirectory;
    public static final File CACHE_DIR = new File(GAME_DIR, ".world-host-cache");

    public static final File CONFIG_DIR = new File(GAME_DIR, "config");
    public static final Path CONFIG_FILE = new File(CONFIG_DIR, "world-host.json5").toPath();
    public static final Path OLD_CONFIG_FILE = new File(CONFIG_DIR, "world-host.json").toPath();
    public static final WorldHostConfig CONFIG = new WorldHostConfig();

    private static List<String> wordsForCid;
    private static Map<String, Integer> wordsForCidInverse;

    public static final long MAX_CONNECTION_IDS = 1L << 42;

    public static final Map<UUID, Long> ONLINE_FRIENDS = new LinkedHashMap<>();
    public static final Map<UUID, ServerStatus> ONLINE_FRIEND_PINGS = new HashMap<>();
    public static final Set<FriendsListUpdate> ONLINE_FRIEND_UPDATES = Collections.newSetFromMap(new WeakHashMap<>());

    public static final Long2ObjectMap<ProxyClient> CONNECTED_PROXY_CLIENTS = new Long2ObjectOpenHashMap<>();

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

    //#if FABRIC
    @Override
    public void onInitializeClient() {
        init();
    }
    //#endif

    private static void init() {
        wordsForCid =
            //#if FABRIC
            FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .flatMap(c -> c.findPath("assets/world-host/16k.txt"))
                .map(path -> {
                    try {
                        return Files.lines(path, StandardCharsets.US_ASCII);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
            //#else
            //$$ ResourcePackLoader
                //#if MC > 11605
                //$$ .getPackFor(MOD_ID)
                //#else
                //$$ .getResourcePackFor(MOD_ID)
                //#endif
            //$$     .map(c -> {
                    //#if MC <= 11902
                    //$$ try {
                    //#endif
            //$$             return c.getResource(PackType.CLIENT_RESOURCES, new ResourceLocation("world-host", "16k.txt"));
                    //#if MC <= 11902
                    //$$ } catch (IOException e) {
                    //$$     throw new UncheckedIOException(e);
                    //$$ }
                    //#endif
            //$$     })
                //#if MC > 11902
                //$$ .map(i -> {
                //$$     try {
                //$$         return i.get();
                //$$     } catch (IOException e) {
                //$$         throw new UncheckedIOException(e);
                //$$     }
                //$$ })
                //#endif
            //$$     .map(is -> new InputStreamReader(is, StandardCharsets.US_ASCII))
            //$$     .map(BufferedReader::new)
            //$$     .map(BufferedReader::lines)
            //#endif
                .orElseThrow(() -> new IllegalStateException("Unable to find 16k.txt"))
                .filter(s -> !s.startsWith("//"))
                .toList();

        if (wordsForCid.size() != (1 << 14)) {
            throw new RuntimeException("Expected WORDS_FOR_CID to have " + (1 << 14) + " elements, but it has " + wordsForCid.size() + " elements.");
        }
        wordsForCidInverse = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < wordsForCid.size(); i++) {
            wordsForCidInverse.put(wordsForCid.get(i), i);
        }

        LOGGER.info("Using client-generated connection ID {}", connectionIdToString(CONNECTION_ID));

        loadConfig();

        //noinspection ResultOfMethodCallIgnored
        CACHE_DIR.mkdirs();
        //#if MC >= 11902
        profileCache = Services.create(
            ((MinecraftAccessor)Minecraft.getInstance()).getAuthenticationService(),
            CACHE_DIR
        ).profileCache();
        //#else
        //$$ profileCache = new GameProfileCache(
        //$$     new YggdrasilAuthenticationService(
        //$$         Minecraft.getInstance().getProxy()
                //#if MC <= 11601
                //$$ , UUID.randomUUID().toString()
                //#endif
        //$$     )
        //$$         .createProfileRepository(),
        //$$     new File(CACHE_DIR, "usercache.json")
        //$$ );
        //#endif
        //#if MC > 11605
        profileCache.setExecutor(Util.backgroundExecutor());
        //#endif

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
        } catch (IOException e) {
            LOGGER.error("Failed to load {}.", CONFIG_FILE.getFileName(), e);
        }
        saveConfig();
    }

    public static void saveConfig() {
        try (JsonWriter writer = JsonWriter.json5(CONFIG_FILE)) {
            CONFIG.write(writer);
        } catch (IOException e) {
            LOGGER.error("Failed to write {}.", CONFIG_FILE.getFileName(), e);
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
            LOGGER.info("Finished authenticating with WH server. Requesting friends list.");
            ONLINE_FRIENDS.clear();
            protoClient.listOnline(CONFIG.getFriends());
            final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null && server.isPublished()) {
                protoClient.publishedWorld(CONFIG.getFriends());
            }
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
                    !CONFIG.isNoUPnP() &&
                        s.getServer().isPublished() &&
                        upnpGateway != null &&
                        protoClient != null &&
                        !protoClient.getUserIp().isEmpty()
                )
                .executes(ctx -> {
                    try {
                        final int port = ctx.getSource().getServer().getPort();
                        final UPnPErrors.AddPortMappingErrors error = upnpGateway.openPort(port, 60, false);
                        if (error == null) {
                            ctx.getSource().sendSuccess(
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
                    ctx.getSource().sendFailure(Components.translatable("world-host.worldhost.tempip.failure"));
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
        final UUID uuid = Minecraft.getInstance().getUser().getGameProfile().getId();
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
        return getIfBlank(profile.getName(), () -> profile.getId().toString());
    }

    // From Apache Commons Lang StringUtils 3.10+
    public static <T extends CharSequence> T getIfBlank(final T str, final Supplier<T> defaultSupplier) {
        return StringUtils.isBlank(str) ? defaultSupplier == null ? null : defaultSupplier.get() : str;
    }

    public static GameProfileCache getProfileCache() {
        return profileCache;
    }

    public static ResourceLocation getInsecureSkinLocation(GameProfile gameProfile) {
        final SkinManager skinManager = Minecraft.getInstance().getSkinManager();
        //#if MC >= 11902
        return skinManager.getInsecureSkinLocation(gameProfile);
        //#else
        //$$ final MinecraftProfileTexture texture = skinManager.getInsecureSkinInformation(gameProfile)
        //$$     .get(MinecraftProfileTexture.Type.SKIN);
        //$$ return texture != null
        //$$     ? skinManager.registerTexture(texture, MinecraftProfileTexture.Type.SKIN)
        //$$     : DefaultPlayerSkin.getDefaultSkin(Player.createPlayerUUID(gameProfile));
        //#endif
    }

    public static void getMaybeAsync(GameProfileCache cache, String name, Consumer<Optional<GameProfile>> action) {
        //#if MC > 11605
        cache.getAsync(name, action);
        //#else
        //$$ action.accept(Optional.ofNullable(cache.get(name)));
        //#endif
    }

    public static void positionTexShader() {
        //#if MC > 11605
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        //#endif
    }

    public static void texture(ResourceLocation texture) {
        //#if MC > 11605
        RenderSystem.setShaderTexture(0, texture);
        //#else
        //$$ Minecraft.getInstance().getTextureManager().bind(texture);
        //#endif
    }

    //#if MC <= 11605
    //$$ @SuppressWarnings("deprecation")
    //#endif
    public static void color(float r, float g, float b, float a) {
        RenderSystem.
            //#if MC > 11605
            setShaderColor
            //#else
            //$$ color4f
            //#endif
                (r, g, b, a);
    }

    public static boolean isFriend(UUID user) {
        return CONFIG.isEnableFriends() && CONFIG.getFriends().contains(user);
    }

    public static void showFriendOrOnlineToast(UUID user, String title, String description, int ticks, Runnable clickAction) {
        Util.backgroundExecutor().execute(() -> {
            final GameProfile profile = Minecraft.getInstance()
                .getMinecraftSessionService()
                .fillProfileProperties(new GameProfile(user, null), false);
            Minecraft.getInstance().execute(() -> {
                final ResourceLocation skinTexture = getInsecureSkinLocation(profile);
                WHToast.builder(Components.translatable(title, getName(profile)))
                    .description(Components.translatable(description))
                    .icon((matrices, x, y, width, height) -> {
                        texture(skinTexture);
                        RenderSystem.enableBlend();
                        GuiComponent.blit(matrices, x, y, width, height, 8, 8, 8, 8, 64, 64);
                        GuiComponent.blit(matrices, x, y, width, height, 40, 8, 8, 8, 64, 64);
                    })
                    .clickAction(clickAction)
                    .ticks(ticks)
                    .important()
                    .show();
            });
        });
    }

    public static FriendlyByteBuf createByteBuf() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    @SuppressWarnings("RedundantThrows")
    public static ServerStatus parseServerStatus(FriendlyByteBuf buf) throws IOException {
        //#if MC > 11605
        return new ClientboundStatusResponsePacket(buf)
            //#if MC >= 11904
            .status();
            //#else
            //$$ .getStatus();
            //#endif
        //#else
        //$$ final ClientboundStatusResponsePacket packet = new ClientboundStatusResponsePacket();
        //$$ packet.read(buf);
        //$$ return packet.getStatus();
        //#endif
    }

    public static ServerStatus createEmptyServerStatus() {
        //#if MC >= 11904
        return new ServerStatus(
            Components.EMPTY, Optional.empty(), Optional.empty(), Optional.empty(), false
            //#if FORGE
            //$$ , Optional.empty()
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
            LOGGER.info("Using external proxy for external IP");
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
            final Integer part = wordsForCidInverse.get(word);
            if (part == null) {
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
            Components.translatable(
                "world-host.worldhost.ip.success",
                Components.copyOnClickText(externalIp)
            ),
            false
        );
        return Command.SINGLE_SUCCESS;
    }

    public static void proxyConnect(long connectionId, InetAddress remoteAddr, Supplier<ProxyPassthrough> proxy) {
        final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null || !server.isPublished()) {
            if (protoClient != null) {
                protoClient.proxyDisconnect(connectionId);
            }
            return;
        }
        try {
            final ProxyClient proxyClient = new ProxyClient(server.getPort(), remoteAddr, connectionId, proxy);
            WorldHost.CONNECTED_PROXY_CLIENTS.put(connectionId, proxyClient);
            proxyClient.start();
        } catch (IOException e) {
            WorldHost.LOGGER.error("Failed to start ProxyClient", e);
        }
    }

    // TODO: Implement using a proper Netty channel to introduce packets directly to the Netty pipeline somehow.
    public static void proxyPacket(long connectionId, byte[] data) {
        final ProxyClient proxyClient = WorldHost.CONNECTED_PROXY_CLIENTS.get(connectionId);
        if (proxyClient != null) {
            try {
                proxyClient.getOutputStream().write(data);
            } catch (IOException e) {
                WorldHost.LOGGER.error("Failed to write to ProxyClient", e);
            }
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

    //#if FABRIC && MC >= 11802
    public static int getMMCLines(boolean isPause) {
        if (FabricLoader.getInstance().isModLoaded("isxander-main-menu-credits")) {
            final MMCConfig baseConfig = MainMenuCredits.getInstance().getConfig();
            final MMCConfigEntry config = isPause ? baseConfig.PAUSE_MENU : baseConfig.MAIN_MENU;
            return (CONFIG.getOnlineStatusLocation() == OnlineStatusLocation.RIGHT ? config.getBottomRight() : config.getBottomLeft()).size();
        }
        return 0;
    }
    //#endif

    //#if FORGE
    //$$ @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    //$$ public static class ClientModEvents {
    //$$     @SubscribeEvent
    //$$     public static void onClientSetup(FMLClientSetupEvent event) {
    //$$         init();
    //$$         final BiFunction<Minecraft, Screen, Screen> screenFunction =
    //$$             (mc, screen) -> new WorldHostConfigScreen(screen);
    //$$         ModLoadingContext.get().registerExtensionPoint(
                //#if MC >= 11902
                //$$ ConfigScreenHandler.ConfigScreenFactory.class,
                //$$ () -> new ConfigScreenHandler.ConfigScreenFactory(screenFunction)
                //#elseif MC >= 11802
                //$$ ConfigGuiHandler.ConfigGuiFactory.class,
                //$$ () -> new ConfigGuiHandler.ConfigGuiFactory(screenFunction)
                //#else
                //$$ ExtensionPoint.CONFIGGUIFACTORY, () -> screenFunction
                //#endif
    //$$         );
    //$$     }
    //$$ }
    //#endif
}
