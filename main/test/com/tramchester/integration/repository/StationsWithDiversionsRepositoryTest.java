package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.StationClosuresConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationsWithDiversionRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.ShudehillMarketStreetClosedTestCategory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class StationsWithDiversionsRepositoryTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private StationsWithDiversionRepository diversionRepository;
    private StationRepository stationsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        Set<String> closed = Collections.singleton("9400ZZMAEXS");

        StationClosuresConfig exchangeSquareBrokenRail = new StationClosuresConfig(closed, LocalDate.of(2024, 2, 12),
                LocalDate.of(2024, 5, 7), false,  Collections.emptySet(), Collections.singleton("9400ZZMAVIC"));
        List<StationClosures> closures = Collections.singletonList(exchangeSquareBrokenRail);

        config = new IntegrationTramTestConfig(closures, IntegrationTramTestConfig.Caching.Enabled);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        diversionRepository = componentContainer.get(StationsWithDiversionRepository.class);
        stationsRepository = componentContainer.get(StationRepository.class);

        // trigger full build of graph DB
        componentContainer.get(StagedTransportGraphBuilder.Ready.class);

    }

    @Test
    void shouldHaveDiversionsForExchangeSquareBrokenRail() {
        // todo once fixed will need to inject test closed station above

        boolean result = diversionRepository.hasDiversions(TramStations.Victoria.from(stationsRepository));
        assertTrue(result);
    }

    @ShudehillMarketStreetClosedTestCategory
    @Test
    void shouldHaveDiversionsMatchingConfig() {
        IdFor<Station> stationWithClosureId = TramStations.ExchangeSquare.getId();

        Optional<GTFSSourceConfig> findSourceConfig = config.getGTFSDataSource().stream().
                filter(sourceConfig -> sourceConfig.getDataSourceId().equals(DataSourceID.tfgm)).findFirst();
        assertTrue(findSourceConfig.isPresent());
        List<StationClosures> allClosed = findSourceConfig.get().getStationClosures();

        List<StationClosures> findStationClosed = allClosed.stream().filter(closure -> closure.getStations().contains(stationWithClosureId)).toList();
        assertEquals(1, findStationClosed.size());
        StationClosures stationClosure = findStationClosed.get(0);

        Set<Station> diversions = stationClosure.getDiversionsAroundClosure().stream().map(id -> stationsRepository.getStationById(id)).collect(Collectors.toSet());

        assertFalse(diversions.isEmpty());

        diversions.forEach(diversionStation ->
                assertTrue(diversionRepository.hasDiversions(diversionStation), "missing for " + diversionStation.getId()));

        diversions.forEach(diversionStation ->
                assertTrue(diversionRepository.getDateRangesFor(diversionStation).contains(stationClosure.getDateRange()),
                        "wrong date rangee for " + diversionStation.getId()));

    }
}
