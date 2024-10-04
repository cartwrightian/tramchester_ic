package com.tramchester.integration.mappers;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.mappers.StopOrderChecker;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StopOrderCheckerTest {
    private static GuiceContainerDependencies componentContainer;
    private StationRepository stationRepository;
    private TramDate when;
    private StopOrderChecker stopOrderChecker;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = componentContainer.get(StationRepository.class);
        stopOrderChecker = componentContainer.get(StopOrderChecker.class);
        when = TestEnv.testDay();
    }

    @Test
    void shouldMatchForAltrinchamStPetersBury() {
        Station begin = Altrincham.from(stationRepository);
        Station middle = StPetersSquare.from(stationRepository);
        Station end = Bury.from(stationRepository);

        boolean result = stopOrderChecker.check(when, begin, middle, end);

        assertTrue(result);
    }

    @Test
    void shouldNotMatchForStPetersAltrinchamBury() {
        Station begin = StPetersSquare.from(stationRepository);
        Station middle = Altrincham.from(stationRepository);
        Station end = Bury.from(stationRepository);

        boolean result = stopOrderChecker.check(when, begin, middle, end);

        assertFalse(result);
    }

    @Test
    void shouldNotMatchForStPetersAltrinchamVictoria() {
        Station begin = StPetersSquare.from(stationRepository);
        Station middle = Altrincham.from(stationRepository);
        Station end = Victoria.from(stationRepository);

        boolean result = stopOrderChecker.check(when, begin, middle, end);

        assertFalse(result);
    }
}
