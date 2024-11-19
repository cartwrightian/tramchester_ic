package com.tramchester.unit.repository;

import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.TramStationAdjacenyRepository;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationAdjacencyRepositoryTest {

    private TramStationAdjacenyRepository repository;
    private TramTransportDataForTestFactory.TramTransportDataForTest transportDataSource;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        ProvidesLocalNow providesNow = new ProvidesLocalNow();

        TramTransportDataForTestFactory dataForTestProvider = new TramTransportDataForTestFactory(providesNow);
        dataForTestProvider.start();
        transportDataSource = dataForTestProvider.getTestData();

        repository = new TramStationAdjacenyRepository(transportDataSource, new GraphFilterActive(false));
    }

    @Test
    void shouldGiveCorrectCostForAdjaceny() {

        assertMinutesEquals(11, getAdjacent(transportDataSource.getFirst(), transportDataSource.getSecond()));
        assertMinutesEquals(9, getAdjacent(transportDataSource.getSecond(), transportDataSource.getInterchange()));

        assertTrue(getAdjacent(transportDataSource.getFirst(), transportDataSource.getInterchange()).isNegative());
    }

    private Duration getAdjacent(Station first, Station second) {
        // date and timerange here need to line up with TramTransportDataForTestFactory
        TimeRange timeRange = TimeRange.of(TramTime.of(6,0), TramTime.of(13,0));
        TramDate date = TramTransportDataForTestFactory.getValidDate();
        return repository.getAdjacent(StationIdPair.of(first.getId(), second.getId()), date, timeRange);
    }
 }
