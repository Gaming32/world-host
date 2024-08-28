package io.github.gaming32.worldhost.compat.simplevoicechat;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.voice.server.Server;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.ext.ServerDataExt;
import io.github.gaming32.worldhost.protocol.punch.PunchManager;
import io.github.gaming32.worldhost.protocol.punch.PunchReason;
import io.github.gaming32.worldhost.protocol.punch.PunchTransmitter;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;

@ForgeVoicechatPlugin
public class WorldHostSimpleVoiceChatCompat implements VoicechatPlugin {
    public static final WorldHostClientVoicechatSocket CLIENT_SOCKET = new WorldHostClientVoicechatSocket();

    @Override
    public String getPluginId() {
        return WorldHost.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientVoicechatInitializationEvent.class, event -> {
            final var serverData = (ServerDataExt)Minecraft.getInstance().getCurrentServer();
            if (serverData == null) return;
            final var connectionId = serverData.wh$getConnectionId();
            if (connectionId == null) return;
            PunchManager.punch(
                connectionId, PunchReason.SIMPLE_VOICE_CHAT, CLIENT_SOCKET::sendDirect,
                hostAndPort -> CLIENT_SOCKET.setTargetAddress(new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort())),
                () -> WorldHost.LOGGER.info("Failed to punch for Simple Voice Chat. Host probably doesn't have it installed.")
            );
            event.setSocketImplementation(CLIENT_SOCKET);
        });
    }

    public static Optional<PunchTransmitter> getServerTransmitter() {
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
