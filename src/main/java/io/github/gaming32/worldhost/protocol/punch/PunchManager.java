package io.github.gaming32.worldhost.protocol.punch;

import com.google.common.net.HostAndPort;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.protocol.ProtocolClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class PunchManager {
    private static final Map<PunchCookie, PendingClientPunch> PENDING_CLIENT_PUNCHES = new HashMap<>();
    private static final Map<PunchCookie, PendingServerPunch> PENDING_SERVER_PUNCHES = new HashMap<>();

    private PunchManager() {
    }

    public static void punch(
        long connectionId,
        PunchReason reason,
        Consumer<HostAndPort> successAction,
        Runnable cancelledAction
    ) {
        final PunchCookie cookie = PunchCookie.random();
        PENDING_CLIENT_PUNCHES.put(cookie, new PendingClientPunch(successAction, cancelledAction));
        if (WorldHost.protoClient != null) {
            WorldHost.protoClient.requestPunchOpen(connectionId, reason.id(), cookie);
        }
    }

    public static void transmitPunches() {
        final HostAndPort signalling = getSignallingServer();
        if (signalling == null) return;
        final List<PendingServerPunch> punches = new ArrayList<>(PENDING_SERVER_PUNCHES.values());
        Thread.ofVirtual().name("PunchManager-Retransmit").start(() -> {
            for (final PendingServerPunch punch : punches) {
                punch.transmit(signalling);
            }
        });
    }

    public static void openPunchRequest(PunchCookie cookie, PunchTransmitter transmitter) {
        final PendingServerPunch punch = new PendingServerPunch(cookie, transmitter);
        final PendingServerPunch old = PENDING_SERVER_PUNCHES.put(cookie, punch);
        if (old != null) {
            WorldHost.LOGGER.warn("New punch request {} replaced old request {}", punch, old);
        }

        final HostAndPort signalling = getSignallingServer();
        if (signalling == null) return;
        Thread.ofVirtual()
            .name("PunchManager-Transmit-" + cookie)
            .start(() -> punch.transmit(signalling));
    }

    private static HostAndPort getSignallingServer() {
        final ProtocolClient client = WorldHost.protoClient;
        if (client == null) {
            return null;
        }
        return client.getHostAndPort();
    }

    public static void stopTransmit(PunchCookie cookie) {
        if (PENDING_SERVER_PUNCHES.remove(cookie) == null) {
            WorldHost.LOGGER.warn("Requested to stop transmitting unknown punch {}", cookie);
        }
    }

    public static void punchSuccess(PunchCookie cookie, HostAndPort hostAndPort) {
        final PendingClientPunch punch = PENDING_CLIENT_PUNCHES.remove(cookie);
        if (punch == null) {
            WorldHost.LOGGER.warn("Success received for unknown punch {}", cookie);
            return;
        }
        punch.successAction.accept(hostAndPort);
    }

    public static void punchCancelled(PunchCookie cookie) {
        final PendingClientPunch punch = PENDING_CLIENT_PUNCHES.remove(cookie);
        if (punch == null) {
            WorldHost.LOGGER.warn("Cancellation received for unknown punch {}", cookie);
            return;
        }
        punch.cancelledAction.run();
    }

    private record PendingClientPunch(Consumer<HostAndPort> successAction, Runnable cancelledAction) {
    }

    private record PendingServerPunch(PunchCookie cookie, PunchTransmitter transmitter) {
        void transmit(HostAndPort target) {
//            try (DatagramSocket socket = new DatagramSocket(localPort)) {
//                socket.send(new DatagramPacket(
//                    cookie.toBytes(), PunchCookie.BYTES,
//                    new InetSocketAddress(target.getHost(), target.getPort())
//                ));
            try {
                transmitter.transmit(cookie.toBytes(), new InetSocketAddress(target.getHost(), target.getPort()));
            } catch (IOException e) {
                WorldHost.LOGGER.error("Failed to transmit {}", this, e);
            }
        }
    }
}
