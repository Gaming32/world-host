package io.github.gaming32.worldhost.protocol.punch;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.compat.simplevoicechat.WorldHostSimpleVoiceChatCompat;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record PunchReason(
    String id,
    VerificationType verificationType,
    TransmitterFinder transmitterFinder
) {
    public static final String SIMPLE_VOICE_CHAT_ID = "simple_voice_chat";

    public static final PunchReason SIMPLE_VOICE_CHAT = new PunchReason(
        SIMPLE_VOICE_CHAT_ID,
        VerificationType.IN_WORLD,
        TransmitterFinder.SIMPLE_VOICE_CHAT
    );

    @Nullable
    public static PunchReason byId(String id) {
        return switch (id) {
            case SIMPLE_VOICE_CHAT_ID -> SIMPLE_VOICE_CHAT;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return "PunchReason[" + id + "]";
    }

    @FunctionalInterface
    public interface VerificationType {
        VerificationType SELF = (uuid) -> uuid.equals(WorldHost.getUserId());
        VerificationType IS_FRIEND = WorldHost::isFriend;
        VerificationType IN_WORLD = (uuid) -> {
            final Minecraft minecraft = Minecraft.getInstance();
            return minecraft.submit(minecraft::getSingleplayerServer)
                .thenCompose(server -> {
                    if (server == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return server.submit(() -> server.getPlayerList().getPlayer(uuid) != null);
                })
                .join();
        };

        Map<VerificationType, String> TYPE_NAMES = Map.of(
            SELF, "SELF",
            IS_FRIEND, "IS_FRIEND",
            IN_WORLD, "IN_WORLD"
        );

        boolean verify(UUID uuid);

        static String getName(VerificationType type) {
            final var result = TYPE_NAMES.get(type);
            return result != null ? result : type.toString();
        }
    }

    @FunctionalInterface
    public interface TransmitterFinder {
        TransmitterFinder SIMPLE_VOICE_CHAT = () -> {
            if (!WorldHost.isModLoaded("voicechat")) {
                return null;
            }
            return WorldHostSimpleVoiceChatCompat.getServerTransmitter().orElse(null);
        };

        Map<TransmitterFinder, String> TYPE_NAMES = Map.of(
            SIMPLE_VOICE_CHAT, "SIMPLE_VOICE_CHAT"
        );

        @Nullable
        PunchTransmitter findServerTransmitter();

        static String getName(TransmitterFinder finder) {
            final var result = TYPE_NAMES.get(finder);
            return result != null ? result : finder.toString();
        }
    }
}
