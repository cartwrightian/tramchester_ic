package com.tramchester.integration.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.geo.RouteAverageVelocity;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@BusTest
public class RouteAverageVelocityTest {

    private static ComponentContainer componentContainer;
    private RouteAverageVelocity averageVelocity;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        averageVelocity = componentContainer.get(RouteAverageVelocity.class);

    }

    @Test
    void getExpectedResultForBusesSPIKE() {

        double result = averageVelocity.getVelocityInMetersPerSecond();

        assertNotEquals(-1, result);
        assertEquals(10.83, result, 0.01);
    }


}
