package com.tramchester.integration.graph.diversions;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.TemporaryStationWalk;
import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.integration.testSupport.config.TemporaryStationsWalkConfigForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramStationWalksTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TemporaryStationWalksRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemporaryStationsWalkRepositoryTest {


    private static TramDate when;
    private static GuiceContainerDependencies componentContainer;
    private static StationIdPair stationIdPairA;
    private TemporaryStationWalksRepository repository;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        when = TestEnv.testDay();

        stationIdPairA = new StationIdPair(TramStations.PiccadillyGardens.getId(), TramStations.Piccadilly.getId());
        StationIdPair stationIdPairB = new StationIdPair(TramStations.Altrincham.getId(), TramStations.NavigationRoad.getId());

        TemporaryStationsWalkIds temporaryStationsWalkA = new TemporaryStationsWalkConfigForTest(stationIdPairA,
                DateRange.of(when.minusDays(3), when.plusDays(3)));
        TemporaryStationsWalkIds temporaryStationsWalkB = new TemporaryStationsWalkConfigForTest(stationIdPairB,
                DateRange.of(when.minusWeeks(2), when.minusWeeks(1)));

        List<TemporaryStationsWalkIds> walks = List.of(temporaryStationsWalkA, temporaryStationsWalkB);

        TramchesterConfig config = new IntegrationTramStationWalksTestConfig(walks);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = componentContainer.get(TemporaryStationWalksRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldGetCurrentWalks() {
        Set<TemporaryStationWalk> walks = getWalksFOrDate(when);

        assertEquals(1, walks.size());

        List<TemporaryStationWalk> results = new ArrayList<>(walks);

        StationPair result = results.get(0).getStationPair();

        StationPair stationPair = stationRepository.getStationPair(stationIdPairA);

        assertEquals(stationPair, result);

    }

    private @NotNull Set<TemporaryStationWalk> getWalksFOrDate(TramDate date) {
        return repository.getTemporaryWalksFor(DataSourceID.tfgm).stream().
                filter(walk -> walk.getDateRange().contains(date)).collect(Collectors.toSet());
    }

    @Test
    void shouldGetWalksForSource() {
        Set<TemporaryStationWalk> walks = repository.getTemporaryWalksFor(DataSourceID.tfgm);

        assertEquals(2, walks.size());
    }

    @Test
    void shouldGetNoWalksIfNotWithinRange() {
        Set<TemporaryStationWalk> walks = getWalksFOrDate(when.plusWeeks(4));

        assertEquals(0, walks.size());
    }
}
