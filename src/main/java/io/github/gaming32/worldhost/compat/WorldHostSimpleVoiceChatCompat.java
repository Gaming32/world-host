package io.github.gaming32.worldhost.compat;

import com.google.common.net.HostAndPort;
import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoiceHostEvent;
import de.maxhenkel.voicechat.voice.server.Server;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.protocol.punch.PunchManager;
import io.github.gaming32.worldhost.protocol.punch.PunchReason;
import io.github.gaming32.worldhost.protocol.punch.PunchTransmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.util.Optional;

@ForgeVoicechatPlugin
public class WorldHostSimpleVoiceChatCompat implements VoicechatPlugin {
    private static final int RE_PUNCH_RATE = 10 * 20;

    private static HostAndPort externalHost;
    private static int prevPort = -1;

    @Override
    public String getPluginId() {
        return WorldHost.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoiceHostEvent.class, event -> {
            if (externalHost != null) {
                event.setVoiceHost(externalHost.toString());
            }
        });
    }

    public static void tick(MinecraftServer mcServer) {
        if (!mcServer.isPublished()) {
            externalHost = null;
            prevPort = -1;
            return;
        }
        final var vcServer = Voicechat.SERVER.getServer();
        if (vcServer == null) return;
        final int port = vcServer.getPort();
        if (port != prevPort || mcServer.getTickCount() % RE_PUNCH_RATE == 0) {
            prevPort = port;
            Minecraft.getInstance().execute(() -> PunchManager.punch(
                WorldHost.CONNECTION_ID,
                PunchReason.SIMPLE_VOICE_CHAT,
                result -> {
                    if (!result.equals(externalHost)) {
                        WorldHost.LOGGER.info("Found new SVC host {}", result);
                        externalHost = result;
                    }
                },
                () -> WorldHost.LOGGER.warn("Failed to punch self for Simple Voice Chat compat")
            ));
        }
    }

    public static Optional<PunchTransmitter> getTransmitter() {
        return Optional.ofNullable(Voicechat.SERVER.getServer())
            .map(Server::getSocket)
            .map(socket -> (packet, address) -> {
                try {
                    socket.send(packet, address);
                } catch (IOException | RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
    }
}
