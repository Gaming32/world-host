package io.github.gaming32.worldhost.protocol.punch;

import com.google.common.net.HostAndPort;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.protocol.WorldHostC2SMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PunchManager {
    private static final int PORT_LOOKUP_RETRANSMIT_PERIOD = 3;
    private static final int SERVER_PUNCH_EXPIRY = 10 * 20;

    private static final Map<UUID, PendingClientPunch> PENDING_CLIENT_PUNCHES = new HashMap<>();
    private static final Map<UUID, PendingServerPunch> PENDING_SERVER_PUNCHES = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingPortLookup> PENDING_PORT_LOOKUPS = new ConcurrentHashMap<>();

    private PunchManager() {
    }

    public static void lookupPort(
        PunchTransmitter transmitter, Consumer<HostAndPort> successAction, Runnable cancelledAction
    ) {
        final var lookupId = UUID.randomUUID();
        final var lookup = new PendingPortLookup(lookupId, transmitter, successAction, cancelledAction);
        PENDING_PORT_LOOKUPS.put(lookupId, lookup);
        if (WorldHost.protoClient != null) {
            WorldHost.protoClient.beginPortLookup(lookupId);
            final var signalling = WorldHost.protoClient.getHostAndPort();
            Thread.ofVirtual()
                .name("PunchManager-TransmitLookup-" + lookup)
                .start(() -> lookup.transmit(signalling));
        }
    }

    public static void punch(
        long connectionId,
        PunchReason reason,
        PunchTransmitter transmitter,
        Consumer<HostAndPort> successAction,
        Runnable cancelledAction
    ) {
        lookupPort(transmitter, myHostAndPort -> {
            final var punchId = UUID.randomUUID();
            PENDING_CLIENT_PUNCHES.put(punchId, new PendingClientPunch(successAction, cancelledAction));
            if (WorldHost.protoClient != null) {
                WorldHost.protoClient.requestPunchOpen(
                    connectionId, reason.id(), punchId,
                    myHostAndPort.getHost(), myHostAndPort.getPort(),
                    "", 0
                );
            }
        }, cancelledAction);
    }

    public static void retransmitAll() {
        final long tickCount = WorldHost.tickCount;

        if (tickCount % PORT_LOOKUP_RETRANSMIT_PERIOD == 0 && WorldHost.protoClient != null) {
            final var signalling = WorldHost.protoClient.getHostAndPort();
            Thread.ofVirtual().name("PunchManager-RetransmitLookups").start(() -> {
                for (final PendingPortLookup lookup : PENDING_PORT_LOOKUPS.values()) {
                    lookup.transmit(signalling);
                }
            });
        }

        Thread.ofVirtual().name("PunchManager-RetransmitPunches").start(() -> {
            final var iter = PENDING_SERVER_PUNCHES.values().iterator();
            while (iter.hasNext()) {
                final var punch = iter.next();
                punch.transmit();
                if (tickCount > punch.expiryTick) {
                    iter.remove();
                }
            }
        });
    }

    public static void openPunchRequest(
        UUID punchId, PunchTransmitter transmitter, String host, int port, long connectionId
    ) {
        final var punch = new PendingServerPunch(
            punchId, connectionId, host, port, transmitter,
            WorldHost.tickCount + SERVER_PUNCH_EXPIRY
        );
        final var old = PENDING_SERVER_PUNCHES.put(punchId, punch);
        if (old != null) {
            WorldHost.LOGGER.warn("New punch request {} replaced old request {} (ID {})", punch, old, punchId);
        }

        Thread.ofVirtual()
            .name("PunchManager-TransmitPunch-" + punchId)
            .start(punch::transmit);

        lookupPort(
            transmitter,
            myAddr -> {
                PENDING_SERVER_PUNCHES.remove(punchId);
                if (WorldHost.protoClient != null) {
                    WorldHost.protoClient.punchSuccess(connectionId, punchId, myAddr.getHost(), myAddr.getPort());
                }
            },
            () -> {
                PENDING_SERVER_PUNCHES.remove(punchId);
                if (WorldHost.protoClient != null) {
                    WorldHost.protoClient.punchFailed(connectionId, punchId);
                }
            }
        );
    }

    public static void portLookupSuccess(UUID lookupId, HostAndPort hostAndPort) {
        final var lookup = PENDING_PORT_LOOKUPS.remove(lookupId);
        if (lookup == null) {
            WorldHost.LOGGER.warn("Success received for unknown port lookup {}", lookupId);
            return;
        }
        lookup.successAction.accept(hostAndPort);
    }

    public static void cancelPortLookup(UUID lookupId) {
        final var lookup = PENDING_PORT_LOOKUPS.remove(lookupId);
        if (lookup == null) {
            WorldHost.LOGGER.warn("Cancellation received for unknown port lookup {}", lookupId);
            return;
        }
        lookup.cancelledAction.run();
    }

    public static void punchSuccess(UUID punchId, HostAndPort hostAndPort) {
        final PendingClientPunch punch = PENDING_CLIENT_PUNCHES.remove(punchId);
        if (punch == null) {
            WorldHost.LOGGER.warn("Success received for unknown punch {}", punchId);
            return;
        }
        punch.successAction.accept(hostAndPort);
    }

    public static void cancelPunch(UUID punchId) {
        final PendingClientPunch punch = PENDING_CLIENT_PUNCHES.remove(punchId);
        if (punch == null) {
            WorldHost.LOGGER.warn("Cancellation received for unknown punch {}", punchId);
            return;
        }
        punch.cancelledAction.run();
    }

    private record PendingClientPunch(Consumer<HostAndPort> successAction, Runnable cancelledAction) {
    }

    private record PendingServerPunch(
        UUID punchId, long connectionId, String host, int port, PunchTransmitter transmitter, long expiryTick
    ) {
        void transmit() {
            try {
                transmitter.transmit(new byte[0], new InetSocketAddress(host, port));
            } catch (IOException e) {
                WorldHost.LOGGER.error("Failed to transmit {}", this, e);
            }
        }
    }

    private record PendingPortLookup(
        UUID lookupId, PunchTransmitter transmitter,
        Consumer<HostAndPort> successAction, Runnable cancelledAction
    ) {
        void transmit(HostAndPort target) {
            try {
                final var packet = new ByteArrayOutputStream();
                WorldHostC2SMessage.writeUuid(new DataOutputStream(packet), lookupId);
                transmitter.transmit(packet.toByteArray(), new InetSocketAddress(target.getHost(), target.getPort()));
            } catch (IOException e) {
                WorldHost.LOGGER.error("Failed to transmit {}", this, e);
            }
        }
    }
}
