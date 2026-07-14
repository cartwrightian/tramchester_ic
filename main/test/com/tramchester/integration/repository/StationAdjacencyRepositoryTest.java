package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TramStationAdjacenyRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        repository = componentContainer.get(TramStationAdjacenyRepository.class);
        TramchesterConfig config = componentContainer.get(TramchesterConfig.class);

        LocalDateTime localDateTime = TestEnv.LocalNow();
        TramTime time = TramTime.ofHourMins(localDateTime.toLocalTime());
        date = TramDate.of(localDateTime.toLocalDate());
        timeRange = TimeRange.of(time, time.plusMinutes(config.getMaxWait()));
    }

    @Test
    void shouldGiveCorrectCostForAdjacencyAltyNavigationRoad() {

        assertMinutesEquals(7, repository.getAdjacent(StationIdPair.of(Altrincham, NavigationRoad), date, timeRange));

        assertTrue(repository.getAdjacent(StationIdPair.of(NavigationRoad, Cornbrook), date, timeRange).invalid());
    }

    @Test
    void shouldGiveCorrectCostForAdjacencyCornbrookDeansgate() {

        StationIdPair stationPair = StationIdPair.of(Cornbrook, Deansgate);
        TramDuration duration = repository.getAdjacent(stationPair, date, timeRange);
        assertEquals(TramDuration.ofMinutes(8), duration.truncateToMinutes());
    }

    @Test
    void shouldGiveCorrectCostForAdjacencyDeansgateCornbrook() {
        StationIdPair stationPair = StationIdPair.of(Deansgate, Cornbrook);
        TramDuration duration = repository.getAdjacent(stationPair, date, timeRange);
        assertEquals(TramDuration.ofMinutes(8), duration.truncateToMinutes());
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

}
