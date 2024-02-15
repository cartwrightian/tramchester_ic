package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.collections.RequestStopStream;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.FastestRoutesForBoxes;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.testSupport.TestEnv.Modes.BusesOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BusTest
class FastestRoutesForBoxesBusTest {

    private static ComponentContainer componentContainer;
    private FastestRoutesForBoxes calculator;
    private StationGroupsRepository stationGroupsRepository;

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
        calculator = componentContainer.get(FastestRoutesForBoxes.class);
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
    }

    @Disabled("peformance")
    @Test
    void shouldReproIssueWithMappingResults() {
        StationGroup destination = KnownLocality.ManchesterCityCentre.from(stationGroupsRepository);

        TramTime time = TramTime.of(7,30);
        TramDate date = TramDate.of(2024,1,28);

        JourneyRequest journeyRequest = new JourneyRequest(
                date, time, false, 2,
                Duration.ofMinutes(60), 2, BusesOnly);

        RequestStopStream<BoundingBoxWithCost> result = calculator.findForGrid(destination, 1000, journeyRequest);

        Stream<BoundingBoxWithCost> results = result.getStream();

        List<BoundingBoxWithCost> destinationBox = results.
                filter(boundingBoxWithCost -> boundingBoxWithCost.getDuration().isZero()).
                toList();

        assertEquals(1, destinationBox.size());
        assertTrue(destinationBox.get(0).contained(destination.getGridPosition()));
    }
}
