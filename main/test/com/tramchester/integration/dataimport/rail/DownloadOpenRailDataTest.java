package com.tramchester.integration.dataimport.rail;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.rail.download.AuthenticateOpenRailData;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Disabled("for debug only due to rate limits")
public class DownloadOpenRailDataTest {

    /* for debug only, don't run normally to respect rate limits on server */

    private static GuiceContainerDependencies componentContainer;
    private AuthenticateOpenRailData authenticator;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationRailTestConfig config = new IntegrationRailTestConfig(false);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        authenticator = componentContainer.get(AuthenticateOpenRailData.class);
    }

    @Test
    void shouldObtainAToken() {
        String token = authenticator.getToken();
        assertFalse(token.isEmpty());
    }
}
