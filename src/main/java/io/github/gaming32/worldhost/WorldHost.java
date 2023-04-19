package io.github.gaming32.worldhost;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.upnp.Gateway;
import io.github.gaming32.worldhost.upnp.GatewayFinder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.players.GameProfileCache;
import org.apache.commons.lang3.StringUtils;
import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

//#if MC >= 11700
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
//#endif

//#if FORGE
//$$ import net.minecraftforge.fml.common.Mod;
//$$ import net.minecraftforge.api.distmarker.Dist;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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
        //#if MC >= 11700
        LogUtils.getLogger();
        //#else
        //$$ LogManager.getLogger();
        //#endif

    public static final File GAME_DIR = Minecraft.getInstance().gameDirectory;
    public static final File CACHE_DIR = new File(GAME_DIR, ".world-host-cache");

    public static final File CONFIG_DIR = new File(GAME_DIR, "config");
    public static final Path CONFIG_FILE = new File(CONFIG_DIR, "world-host.json5").toPath();
    public static final Path OLD_CONFIG_FILE = new File(CONFIG_DIR, "world-host.json").toPath();
    public static final WorldHostConfig CONFIG = new WorldHostConfig();

    public static final Set<UUID> ONLINE_FRIENDS = new HashSet<>();
    public static final Map<UUID, ServerStatus> ONLINE_FRIEND_PINGS = new HashMap<>();
    public static final Set<FriendsListUpdate> ONLINE_FRIEND_UPDATES = Collections.newSetFromMap(new WeakHashMap<>());

    public static final Long2ObjectMap<ProxyClient> CONNECTED_PROXY_CLIENTS = new Long2ObjectOpenHashMap<>();

    public static final UUID CONNECTION_ID = UUID.randomUUID();

    public static Gateway upnpGateway;

    private static GameProfileCache profileCache;

    //#if FABRIC
    @Override
    public void onInitializeClient() {
        init();
    }
    //#endif

    private static void init() {
        LOGGER.info("Using client-generated connection ID {}", CONNECTION_ID);

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

        new GatewayFinder(gateway -> {
            upnpGateway = gateway;
            LOGGER.info("Found UPnP gateway: {}", gateway.getGatewayIP());
        });
    }

    public static void loadConfig() {
        try (JsonReader reader = JsonReader.json5(CONFIG_FILE)) {
            CONFIG.read(reader);
            if (Files.exists(OLD_CONFIG_FILE)) {
                LOGGER.info("Old {} still exists. Maybe consider removing it?", OLD_CONFIG_FILE.getFileName());
            }
        } catch (NoSuchFileException e) {
            try (JsonReader reader = JsonReader.json(OLD_CONFIG_FILE)) {
                CONFIG.read(reader);
                LOGGER.info(
                    "Found and read old {} into new {}. Maybe consider deleting the old {}?",
                    OLD_CONFIG_FILE.getFileName(), CONFIG_FILE.getFileName(), OLD_CONFIG_FILE.getFileName()
                );
            } catch (NoSuchFileException e1) {
                LOGGER.info("Old {} not found. Writing default.", CONFIG_FILE.getFileName());
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

    //#if FORGE
    //$$ @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    //$$ public static class ClientModEvents {
    //$$     @SubscribeEvent
    //$$     public static void onClientSetup(FMLClientSetupEvent event) {
    //$$         init();
    //$$     }
    //$$ }
    //#endif

    public static ResourceLocation getInsecureSkinLocation(SkinManager skinManager, GameProfile gameProfile) {
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
}
