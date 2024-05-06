package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TramStationAdjacenyRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationAdjacencyRepositoryTest {
    private static ComponentContainer componentContainer;

    private TramStationAdjacenyRepository repository;
    private TransportData transportDataSource;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        transportDataSource = componentContainer.get(TransportData.class);
        repository = componentContainer.get(TramStationAdjacenyRepository.class);
    }

    @Test
    void shouldGiveCorrectCostForAdjacencyAltyNavigationRoad() {
        assertMinutesEquals(3, getAdjacent(Altrincham, NavigationRoad));
        assertTrue(getAdjacent(NavigationRoad, Cornbrook).isNegative());
    }

    @Test
    void shouldGiveCorrectCostForAdjacencyCornbrookDeansgate() {
        assertMinutesEquals(4, getAdjacent(Cornbrook, Deansgate));
    }

    @Test
    void shouldGiveCorrectCostForAdjacencyDeansgateCornbrook() {
        assertMinutesEquals(3, getAdjacent(Deansgate, Cornbrook));
    }

    @Test
    void shouldHavePairsOfStations() {
        Set<StationPair> pairs = repository.getTramStationParis();
        IdFor<Station> stationId = NavigationRoad.getId();

        List<StationPair> results = pairs.stream().
                filter(pair -> pair.getBegin().getId().equals(stationId) ||
                        pair.getEnd().getId().equals(stationId)).
                toList();
        Assertions.assertEquals(4, results.size(), pairs.toString());
    }

    private Duration getAdjacent(TramStations first, TramStations second) {
        StationPair pair = StationPair.of(transportDataSource.getStationById(first.getId()),
                transportDataSource.getStationById(second.getId()));
        return repository.getAdjacent(pair);
    }
}
