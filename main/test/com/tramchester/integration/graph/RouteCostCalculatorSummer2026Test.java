package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.MultiMode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ConfigParameterResolver.class)
@MultiMode
@DataUpdateTest
class RouteCostCalculatorSummer2026Test {

    private static ComponentContainer componentContainer;

    private RouteCostCalculator routeCostCalculator;
    private StationRepository stationRepository;
    private GraphTransaction txn;
    private final TramDate when = TestEnv.testDay();
    private Station altrincham;
    private Station mediaCity;
    private Station airport;
    private ImmutableEnumSet<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) {
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeCostCalculator = componentContainer.get(RouteCostCalculator.class);
        stationRepository = componentContainer.get(StationRepository.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTx();

        altrincham = Altrincham.from(stationRepository);
        mediaCity = MediaCityUK.from(stationRepository);
        airport = ManAirport.from(stationRepository);

        modes = TransportMode.TramsOnly;
    }

    @AfterEach
    void afterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyNavRoad() throws InvalidDurationException {
        assertEquals(TramDuration.ofMinutes(4), getAverageCostBetween(NavigationRoad.from(stationRepository), altrincham));
        assertMinutesEquals(8, getAverageCostBetween(altrincham, NavigationRoad.from(stationRepository)));
    }

    @Test
    void shouldReproduceIssueFromAltyToCornbrook() throws InvalidDurationException {
        assertEquals(TramDuration.ofHours(1).plusMinutes(2), getAverageCostBetween(altrincham, Cornbrook.from(stationRepository)));
    }

    @Test
    void shouldComputeCostsForMediaCityAshton() throws InvalidDurationException {
        assertEquals(TramDuration.ofHours(1).plusMinutes(6), getAverageCostBetween(mediaCity, Ashton.from(stationRepository)));
        assertEquals(TramDuration.ofMinutes(65), getAverageCostBetween(Ashton.from(stationRepository), mediaCity));
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyBury() throws InvalidDurationException {
        // changes regularly with timetable updates

        final Station bury = Bury.from(stationRepository);

        assertEquals(TramDuration.ofHours(1).plusMinutes(57), getAverageCostBetween(bury, altrincham));
        assertEquals(TramDuration.ofHours(2).plusMinutes(2), getAverageCostBetween(altrincham, bury));
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsMediaCityAirport() throws InvalidDurationException {
        assertEquals(TramDuration.ofMinutes(63), getAverageCostBetween(mediaCity, airport));
        assertEquals(TramDuration.ofMinutes(62), getAverageCostBetween(airport, mediaCity));
    }

    private TramDuration getAverageCostBetween(Station start, Station end) throws InvalidDurationException {
        return routeCostCalculator.getAverageCostBetween(txn, start, end, when, modes).truncateToMinutes();
    }

}
