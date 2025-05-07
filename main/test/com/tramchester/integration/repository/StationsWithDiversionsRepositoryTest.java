package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.*;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.config.IntegrationTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationsWithDiversionRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StationsWithDiversionsRepositoryTest {
    private static ComponentContainer componentContainer;
    private static LocalDate begin;
    private static LocalDate end;

    private StationsWithDiversionRepository diversionRepository;
    private StationRepository stationsRepository;

    private static final TramStations withDiversion = TramStations.ExchangeSquare;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        begin = LocalDate.of(2024, 2, 12);
        end = LocalDate.of(2024, 5, 7);

        TramchesterConfig config = new DiversionsConfig();

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
    void shouldHaveDiversionAtVictoria() {
        // NOTE: if integration test config changes for closures need to cleanGraph

        Station victoria = TramStations.Victoria.from(stationsRepository);
        assertTrue(diversionRepository.hasDiversions(victoria));

        List<DateTimeRange> ranges = new ArrayList<>(diversionRepository.getDateTimeRangesFor(victoria));

        assertEquals(1, ranges.size());

        DateTimeRange dateTimeRange = ranges.getFirst();

        DateRange dateRange = dateTimeRange.getDateRange();
        assertEquals(TramDate.of(begin), dateRange.getStartDate());
        assertEquals(TramDate.of(end), dateRange.getEndDate());
        assertEquals(TimeRange.AllDay(), dateTimeRange.getTimeRange());


    }

    private static class DiversionsConfig extends IntegrationTramClosedStationsTestConfig {

        public DiversionsConfig() {
            this(getClosures());
        }

        public DiversionsConfig(List<StationClosures> closures) {
            super(closures, true, IntegrationTestConfig.CurrentStationWalks);
        }

        @Override
        public TfgmTramLiveDataConfig getLiveDataConfig() {
            return null;
        }

        private static List<StationClosures> getClosures() {
            //Set<String> closed = Collections.singleton(withDiversion.getRawId());
            StationListConfig closed = new StationListConfig(Collections.singleton(withDiversion.getRawId()));
            DateRangeConfig dataRangeConfig = new DateRangeConfig(begin, end);

            StationClosuresConfig closureConfig = new StationClosuresConfig(closed, dataRangeConfig,
                    null, false,  Collections.emptySet(), Collections.singleton(TramStations.Victoria.getRawId()));

            return Collections.singletonList(closureConfig);
        }

    }

}
