package com.tramchester.integration.network;

import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConnectivityTest {

    // Here to assist in diagnosing connectivity issues on the CI machines

    @EnabledIfEnvironmentVariable(named = TestEnv.SERVER_URL_ENV_VAR, matches = ".*")
    @Test
    void checkCanReachDevServer() {

        String serverURL = System.getenv(TestEnv.SERVER_URL_ENV_VAR);
        assertNotNull(serverURL);

        URI uri = URI.create(serverURL);
        String host = uri.getHost();

        boolean reachable = isReachable(host, 443);
        Assertions.assertTrue(reachable, "Could not reach " + serverURL + " from " + TestEnv.SERVER_URL_ENV_VAR);
    }

    @Test
    void shouldReachWellKnownServer() {
        boolean reachable = isReachable("google.co.uk", 443);
        Assertions.assertTrue(reachable);
    }

    private boolean isReachable(final String host, int port) {
        boolean opened = false;
        try {
            Socket socket = new Socket(host, port);
            opened = true;
            socket.close();
        } catch (IOException e) {
            // can't open socket
        }
        return opened;
    }
}
