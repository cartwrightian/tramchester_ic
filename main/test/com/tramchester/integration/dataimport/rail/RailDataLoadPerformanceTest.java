package com.tramchester.integration.dataimport.rail;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.rail.RailTransportDataFromFiles;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RailDataLoadPerformanceTest {

    private static GuiceContainerDependencies componentContainer;
    private RailTransportDataFromFiles fromFiles;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationRailTestConfig config = new IntegrationRailTestConfig(IntegrationRailTestConfig.Scope.GreaterManchester);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        fromFiles = componentContainer.get(RailTransportDataFromFiles.class);
    }


    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @Test
    void shouldLoadData() {
        ProvidesNow providesNow = componentContainer.get(ProvidesNow.class);

        for (int i = 0; i < 100; i++) {
            TransportDataContainer container = new TransportDataContainer(providesNow, "testSourceName");
            fromFiles.loadInto(container);
            container.dispose();
        }

    }


}
