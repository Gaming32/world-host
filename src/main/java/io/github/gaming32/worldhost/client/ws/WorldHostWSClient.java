package io.github.gaming32.worldhost.client.ws;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Future;

public class WorldHostWSClient implements AutoCloseable {
    private final Session session;

    private boolean authenticated;

    public WorldHostWSClient(URI serverUri) throws DeploymentException, IOException {
        session = ContainerProvider.getWebSocketContainer().connectToServer(WorldHostClientEndpoint.class, serverUri);
    }

    public Future<Void> authenticate(UUID uuid) {
        authenticated = true;
        return session.getAsyncRemote().sendObject(uuid);
    }

    private void ensureAuthenticated() {
        if (!authenticated) {
            throw new IllegalStateException("Attempted to communicate with server before authenticating.");
        }
    }

    @Override
    public void close() throws Exception {
        session.close();
    }
}
