package io.github.gaming32.worldhost;

import io.github.gaming32.worldhost.upnp.Gateway;
import io.github.gaming32.worldhost.upnp.GatewayFinder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.status.ServerStatus;
import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

//#if MC >= 11700
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
//#else
//$$ import org.apache.logging.log4j.LogManager;
//$$ import org.apache.logging.log4j.Logger;
//#endif

//#if FABRIC
import net.fabricmc.api.ClientModInitializer;
//#endif

//#if FORGE
//$$ import net.minecraftforge.fml.common.Mod;
//#if MC > 11202
//$$ import net.minecraftforge.api.distmarker.Dist;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//#else
//$$ import net.minecraftforge.fml.common.event.FMLInitializationEvent;
//#endif
//#endif

//#if FORGE
//#if MC > 11202
//$$ @Mod(WorldHost.MOD_ID)
//#else
//$$ @Mod(modid = WorldHost.MOD_ID, useMetadata = true, clientSideOnly = true)
//#endif
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

    //#if FABRIC
    @Override
    public void onInitializeClient() {
        init();
    }
    //#endif

    private static void init() {
        LOGGER.info("Using client-generated connection ID {}", CONNECTION_ID);

        loadConfig();

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

    //#if FORGE
    //#if MC > 11202
    //$$ @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    //$$ public static class ClientModEvents {
    //$$     @SubscribeEvent
    //$$     public static void onClientSetup(FMLClientSetupEvent event) {
    //$$         init();
    //$$     }
    //$$ }
    //#else
    //$$ @Mod.EventHandler
    //$$ public void init(FMLInitializationEvent event) {
    //$$     init();
    //$$ }
    //#endif
    //#endif
}
