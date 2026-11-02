package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.FindLinkedStations;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig.Scope.GreaterManchester;
import static com.tramchester.repository.Interchanges.WEEKS_AHEAD_FOR_LINKS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TrainTest
class FindStationsByNumberLinksTrainTest {
    private static ComponentContainer componentContainer;
    private FindLinkedStations discoverer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationRailTestConfig(GreaterManchester);

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        discoverer = componentContainer.get(FindLinkedStations.class);
    }

    @Test
    void shouldIdForkPointsFromTrainNetwork() {
        int threshhold = 3;
        TramDate start = TramDate.of(LocalDate.now());
        DateRange dateRange = DateRange.of(start, start.plusWeeks(WEEKS_AHEAD_FOR_LINKS));

        IdSet<Station> found = discoverer.atLeastNLinkedStations(TransportMode.Train, threshhold, dateRange);

        assertTrue(found.contains(RailStationIds.ManchesterPiccadilly.getId()));
        //assertTrue(found.contains(RailStationIds.LondonEuston.getId()));
        assertTrue(found.contains(RailStationIds.Stockport.getId()));

        assertFalse(found.contains(RailStationIds.Mobberley.getId()));
        assertFalse(found.contains(RailStationIds.Hale.getId()));

    }

}
