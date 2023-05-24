package io.github.gaming32.worldhost.protocol.punch;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.screen.JoiningWorldHostScreen;
import io.github.gaming32.worldhost.versions.Components;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RateKickingConnection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.LegacyQueryHandler;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class PunchClient extends Thread {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger();

    private final String host;
    private final int port;
    private final boolean isServer;
    private final long myConnectionId;
    private final long targetConnectionId;

    public PunchClient(String host, int port, boolean isServer, long myConnectionId, long targetConnectionId) {
        setName("PunchClient-" + ID_COUNTER.getAndIncrement());
        setDaemon(true);
        this.host = host;
        this.port = port;
        this.isServer = isServer;
        this.myConnectionId = myConnectionId;
        this.targetConnectionId = targetConnectionId;
    }

    @Override
    public void run() {
        Socket clientSocket = null;
        try {
            final Socket socket = new Socket(host, port);
            final DataInputStream socketIs = new DataInputStream(socket.getInputStream());
            final DataOutputStream socketOs = new DataOutputStream(socket.getOutputStream());

            socketOs.writeBoolean(isServer);
            socketOs.writeLong(myConnectionId);
            socketOs.writeLong(targetConnectionId);

            final InetAddress addrToConnect = InetAddress.getByAddress(socketIs.readNBytes(socketIs.readUnsignedByte()));
            final int portToConnect = socketIs.readUnsignedShort();
            final int localPort = socketIs.readUnsignedShort();

            final ServerSocket serverSocket = new ServerSocket(localPort);
            clientSocket = serverSocket.accept();

            clientSocket.setReuseAddress(true);
            clientSocket.close();

            clientSocket = new Socket();
            clientSocket.setReuseAddress(true);

            clientSocket.bind(new InetSocketAddress(localPort));

            clientSocket.connect(new InetSocketAddress(addrToConnect, portToConnect));

            // Now the client socket is the socket to use
            final Minecraft minecraft = Minecraft.getInstance();
            @SuppressWarnings("deprecation") final Channel channel = new OioSocketChannel(clientSocket);
            if (isServer) {
                final MinecraftServer server = minecraft.getSingleplayerServer();
                if (server == null) {
                    throw new IllegalStateException("Server closed while punching");
                }
                final ServerConnectionListener listener = server.getConnection();
                assert listener != null;

                // The following is from ServerConnectionListener.startTcpServerListener
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException ignored) {
                }
                final ChannelPipeline pipeline = channel.pipeline()
                    .addLast("timeout", new ReadTimeoutHandler(30))
                    .addLast("legacy_query", new LegacyQueryHandler(listener));
                Connection.configureSerialization(pipeline, PacketFlow.SERVERBOUND);
                final int pps = server.getRateLimitPacketsPerSecond();
                final Connection connection = pps > 0 ? new RateKickingConnection(pps) : new Connection(PacketFlow.SERVERBOUND);
                listener.getConnections().add(connection);
                pipeline.addLast("packet_handler", connection);
                connection.setListener(new ServerHandshakePacketListenerImpl(server, connection));
            } else {
                final Screen screen = minecraft.screen;
                if (!(screen instanceof JoiningWorldHostScreen joinScreen)) {
                    throw new IllegalStateException("Unexpected screen " + screen + ". Expected JoiningWorldHostScreen.");
                }

                // The following is from ConnectScreen.startConnecting
                minecraft.clearLevel();
                minecraft.prepareForMultiplayer();
                minecraft.updateReportEnvironment(ReportEnvironment.thirdParty(addrToConnect.getHostName()));

                // The following is from Connection.connectToServer
                final Connection connection = new Connection(PacketFlow.CLIENTBOUND);
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException ignored) {
                }
                final ChannelPipeline pipeline = channel.pipeline()
                    .addLast("timeout", new ReadTimeoutHandler(30));
                Connection.configureSerialization(pipeline, PacketFlow.CLIENTBOUND);
                pipeline.addLast("packet_handler", connection);

                // The following is from Connection.connect
                joinScreen.setConnection(connection);
                connection.setListener(new ClientHandshakePacketListenerImpl(
                    connection, minecraft, null, joinScreen.parent, false, null, joinScreen::setStatus
                ));
                connection.send(new ClientIntentionPacket(addrToConnect.getHostName(), portToConnect, ConnectionProtocol.LOGIN));
                connection.send(new ServerboundHelloPacket(minecraft.getUser().getName(), Optional.ofNullable(minecraft.getUser().getProfileId())));
            }
        } catch (Exception e) {
            WorldHost.LOGGER.error("Error in punch client", e);
            if (!isServer) {
                final Minecraft minecraft = Minecraft.getInstance();
                //noinspection DataFlowIssue
                minecraft.setScreen(new DisconnectedScreen(
                    minecraft.screen instanceof JoiningWorldHostScreen joinScreen ? joinScreen.parent : minecraft.screen,
                    CommonComponents.CONNECT_FAILED,
                    Components.translatable("disconnect.genericReason", e)
                ));
            }
        } finally {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e1) {
                    WorldHost.LOGGER.error("Error closing clientSocket", e1);
                }
            }
        }
    }
}
