package io.github.gaming32.worldhost.protocol.punch;

import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record PunchReason(String id, VerificationType verificationType, TransmitterFinder transmitterFinder) {
    public static final String SIMPLE_VOICE_CHAT_ID = "simple_voice_chat";

    public static final PunchReason SIMPLE_VOICE_CHAT = new PunchReason(
        SIMPLE_VOICE_CHAT_ID,
        VerificationType.SELF,
        TransmitterFinder.SIMPLE_VOICE_CHAT
    );

    @Nullable
    public static PunchReason byId(String id) {
        return switch (id) {
            case SIMPLE_VOICE_CHAT_ID -> SIMPLE_VOICE_CHAT;
            default -> null;
        };
    }

    public enum VerificationType {
        SELF {
            @Override
            public boolean verify(UUID uuid) {
                return uuid.equals(WorldHost.getUserId());
            }
        },
        IS_FRIEND {
            @Override
            public boolean verify(UUID uuid) {
                return WorldHost.isFriend(uuid);
            }
        },
        IN_WORLD {
            @Override
            public boolean verify(UUID uuid) {
                final Minecraft minecraft = Minecraft.getInstance();
                return minecraft.submit(minecraft::getSingleplayerServer)
                    .thenCompose(server -> {
                        if (server == null) {
                            return CompletableFuture.completedFuture(false);
                        }
                        return server.submit(() -> server.getPlayerList().getPlayer(uuid) != null);
                    })
                    .join();
            }
        };

        public abstract boolean verify(UUID uuid);
    }

    public enum TransmitterFinder {
        SIMPLE_VOICE_CHAT {
            @Override
            public PunchTransmitter findTransmitter() {
//                if (!WorldHost.isModLoaded("voicechat")) {
                    return null;
//                }
//                return WorldHostSimpleVoiceChatCompat.getTransmitter().orElse(null);
            }
        };

        @Nullable
        public abstract PunchTransmitter findTransmitter();
    }
}
