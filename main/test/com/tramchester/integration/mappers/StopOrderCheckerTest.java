package com.tramchester.integration.mappers;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.mappers.StopOrderChecker;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
public class StopOrderCheckerTest {
    private static GuiceContainerDependencies componentContainer;
    private StationRepository stationRepository;
    private TramDate when;
    private StopOrderChecker stopOrderChecker;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
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

    @Test
    void shouldNotMatchShudehillPicadillyAltrincham() {
        Station begin = Shudehill.from(stationRepository);
        Station middle = Piccadilly.from(stationRepository);
        Station end = Altrincham.from(stationRepository);

        boolean result = stopOrderChecker.check(when, begin, middle, end);

        assertFalse(result);
    }
}
