package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.graphbuild.StationGroupsGraphBuilder;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.integration.testSupport.TestGroupType;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@BusTest
@Disabled("For performance testing")
class GraphBuildForBusPerformanceTest {

    private static ComponentContainer componentContainer;
    private static IntegrationBusTestConfig testConfig;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        testConfig = new IntegrationBusTestConfig(TestGroupType.performance);
        TestEnv.deleteDBIfPresent(testConfig);

        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(testConfig);
    }

    @Test
    void shouldTriggerMainGraphBuild() {
        componentContainer.get(StagedTransportGraphBuilder.Ready.class);
    }

    @Test
    void shouldTriggerGroupBuild() {
        componentContainer.get(StationGroupsGraphBuilder.Ready.class);
    }

    @Test
    void shouldTriggerLinksBuild() {
        componentContainer.get(StationsAndLinksGraphBuilder.Ready.class);
    }

}


