package com.tramchester.integration.mappers;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.mappers.StopOrderChecker;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.integration.testSupport.rail.RailStationIds.Chester;
import static com.tramchester.integration.testSupport.rail.RailStationIds.LondonEuston;
import static org.junit.jupiter.api.Assertions.assertTrue;

@GMTest
public class TramTrainStopOrderCheckerTest {
    private static GuiceContainerDependencies componentContainer;
    private StationRepository stationRepository;
    private TramDate when;
    private StopOrderChecker stopOrderChecker;
    private CRSRepository crsRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = componentContainer.get(StationRepository.class);
        stopOrderChecker = componentContainer.get(StopOrderChecker.class);
        crsRepository = componentContainer.get(CRSRepository.class);

        when = TestEnv.testDay();
    }

    @Test
    void shouldMatchForAltrinchamStockportPiccadilly() {
        Station begin = RailStationIds.Altrincham.from(stationRepository);
        Station middle = RailStationIds.Stockport.from(stationRepository);
        Station end = RailStationIds.ManchesterPiccadilly.from(stationRepository);

        boolean result = stopOrderChecker.check(when, begin, middle.getId(), end.getId());

        assertTrue(result);
    }

    @Test
    void shouldMatchForManchesterPiccSHaleChester() {
        Station begin = RailStationIds.ManchesterPiccadilly.from(stationRepository);
        Station middle = RailStationIds.Hale.from(stationRepository);

        Station end = crsRepository.getStationFor(Chester.crs());

        // not loaded into main station repository as out of bounds
        boolean result = stopOrderChecker.check(when, begin, middle.getId(), end.getId());

        assertTrue(result);
    }

    @Test
    void shouldMatchForManchesterPiccStockportLondon() {
        Station begin = RailStationIds.ManchesterPiccadilly.from(stationRepository);
        Station middle = RailStationIds.Stockport.from(stationRepository);

        // not loaded into main station repository as out of bounds
        Station end = crsRepository.getStationFor(LondonEuston.crs());

        boolean result = stopOrderChecker.check(when, begin, middle.getId(), end.getId());

        assertTrue(result);
    }


}
