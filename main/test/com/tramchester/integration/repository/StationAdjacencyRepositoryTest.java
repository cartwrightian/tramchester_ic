package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TramStationAdjacenyRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationAdjacencyRepositoryTest {
    private static ComponentContainer componentContainer;

    private TramStationAdjacenyRepository repository;
    private TransportData transportDataSource;
    private TramDate date;
    private TimeRange timeRange;

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
        TramchesterConfig config = componentContainer.get(TramchesterConfig.class);

        LocalDateTime localDateTime = TestEnv.LocalNow();
        TramTime time = TramTime.ofHourMins(localDateTime.toLocalTime());
        date = TramDate.of(localDateTime.toLocalDate());
        timeRange = TimeRange.of(time, time.plusMinutes(config.getMaxWait()));
    }

    @Test
    void shouldGiveCorrectCostForAdjacencyAltyNavigationRoad() {
        assertMinutesEquals(3, getAdjacent(Altrincham, NavigationRoad));
        assertTrue(getAdjacent(NavigationRoad, Cornbrook).isNegative());
    }

    @Test
    void shouldGiveCorrectCostForAdjacencyCornbrookDeansgate() {
        assertEquals(Duration.ofMinutes(3).plusSeconds(37), getAdjacent(Cornbrook, Deansgate));
    }

    @Test
    void shouldGiveCorrectCostForAdjacencyDeansgateCornbrook() {
        assertMinutesEquals(3, getAdjacent(Deansgate, Cornbrook));
    }

    @Test
    void shouldHavePairsOfStations() {
        Set<StationPair> pairs = repository.getTramStationParis(date);
        IdFor<Station> stationId = NavigationRoad.getId();

        List<StationPair> results = pairs.stream().
                filter(pair -> pair.getBegin().getId().equals(stationId) ||
                        pair.getEnd().getId().equals(stationId)).
                toList();
        assertEquals(4, results.size(), pairs.toString());
    }

    private Duration getAdjacent(TramStations first, TramStations second) {
        StationPair pair = StationPair.of(transportDataSource.getStationById(first.getId()),
                transportDataSource.getStationById(second.getId()));

        return repository.getAdjacent(pair, date, timeRange);
    }
}
