package com.tramchester.integration.repository.naptan;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.repository.naptan.NaptanRepositoryContainer;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

@Disabled("Performance testing only")
public class NaptanRepositoryPerformanceTest {

    private static GuiceContainerDependencies componentContainer;
    private NaptanRepositoryContainer respository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfigWithNaptan(
                ImmutableEnumSet.of(TransportMode.Bus, TransportMode.Tram, TransportMode.Train));
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        respository = componentContainer.get(NaptanRepositoryContainer.class);
    }

    @Test
    void shouldRestartRepository() {
        respository.stop(); // will already be started

        for (int i = 0; i < 20; i++) {
            respository.start();
            respository.stop();
        }
    }
}
