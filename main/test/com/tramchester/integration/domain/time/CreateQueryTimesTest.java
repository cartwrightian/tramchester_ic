package com.tramchester.integration.domain.time;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataUpdateTest
public class CreateQueryTimesTest {

    // Note this needs to be > time for whole test fixture, see note below in @After
    public static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private CreateQueryTimes createQueryTimes;
    private int numberQueries;
    private int mins;
    private TramDate date;

    private final EnumSet<TransportMode> modes = TramsOnly;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        date = TestEnv.testDay();

        createQueryTimes = componentContainer.get(CreateQueryTimes.class);
        stationRepository = componentContainer.get(StationRepository.class);

        numberQueries = config.getNumberQueries();
        mins = config.getQueryInterval();
    }

    @Test
    void shouldHaveNormalQueryTimes() {
        TramTime queryTime = TramTime.of(9, 45);

        Station location = StPetersSquare.from(stationRepository);
        List<TramTime> times = createQueryTimes.generate(queryTime, location, date, modes);
        assertFalse(times.isEmpty());


        assertEquals(numberQueries, times.size());
        assertEquals(queryTime, times.getFirst());

        TramTime lastTime = queryTime.plusMinutes(mins * (numberQueries -1));
        assertEquals(lastTime, times.getLast(), "wrong last time " + times);
    }

    @Test
    void shouldHaveNormalQueryTimesEndOfDayCentral() {
        TramTime queryTime = TramTime.of(23, 55);

        Station location = StPetersSquare.from(stationRepository);

        List<TramTime> times = createQueryTimes.generate(queryTime, location, date, modes);
        assertFalse(times.isEmpty());

        assertEquals(numberQueries, times.size());
        assertEquals(queryTime, times.getFirst());

        TramTime lastTime = queryTime.plusMinutes(mins * (numberQueries -1));
        assertEquals(lastTime, times.getLast(), "wrong last time " + times);
    }

    @Test
    void shouldHaveNormalQueryTimesEndOfDayEndOfLine() {
        TramTime queryTime = TramTime.nextDay(0, 30);

        Station location = Altrincham.from(stationRepository);

        List<TramTime> results = createQueryTimes.generate(queryTime, location, date, modes);
        assertFalse(results.isEmpty());

        assertEquals(1, results.size());
        assertEquals(queryTime, results.getFirst());
    }

    @Test
    void shouldGetTimesForStationWalks() {
        TramTime queryTime = TramTime.nextDay(0, 15);

        Station stPetersSquare = StPetersSquare.from(stationRepository);
        Station marketStreet = MarketStreet.from(stationRepository);

        Set<StationWalk> stationWalks = new HashSet<>();
        stationWalks.add(new StationWalk(stPetersSquare, TramDuration.ofMinutes(1)));
        stationWalks.add(new StationWalk(marketStreet, TramDuration.ofMinutes(2)));

        List<TramTime> results = createQueryTimes.generate(queryTime, stationWalks, date, modes);

        assertFalse(results.isEmpty());

        assertEquals(numberQueries, results.size());
        assertEquals(queryTime, results.getFirst());

        TramTime lastTime = queryTime.plusMinutes(mins * (numberQueries -1));
        assertEquals(lastTime, results.getLast(), "wrong last time " + results);
    }
}
