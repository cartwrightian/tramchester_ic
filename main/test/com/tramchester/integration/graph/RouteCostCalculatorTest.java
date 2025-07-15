package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.facade.ImmutableGraphTransaction;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.conditional.PiccGardensWorkSummer2025;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
@DataUpdateTest
class RouteCostCalculatorTest {

    private static ComponentContainer componentContainer;

    private RouteCostCalculator routeCostCalculator;
    private StationRepository stationRepository;
    private ImmutableGraphTransaction txn;
    private final TramDate when = TestEnv.testDay();
    private Station altrincham;
    private Station mediaCity;
    private Station airport;
    private EnumSet<TransportMode> modes;

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

        modes = TramsOnly;
    }

    @AfterEach
    void afterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyNavRoad() throws InvalidDurationException {
        assertEquals(Duration.ofMinutes(3).plusSeconds(1), routeCostCalculator.getAverageCostBetween(txn, NavigationRoad.from(stationRepository), altrincham, when, modes));
        assertMinutesEquals(4, routeCostCalculator.getAverageCostBetween(txn, altrincham, NavigationRoad.from(stationRepository), when, modes));
    }

    @PiccGardensWorkSummer2025
    @Test
    void shouldComputeCostsForMediaCityAshton() throws InvalidDurationException {
        assertEquals(Duration.ofMinutes(56).plusSeconds(1), routeCostCalculator.getAverageCostBetween(txn, mediaCity, Ashton.from(stationRepository), when, modes));
        assertEquals(Duration.ofMinutes(54), routeCostCalculator.getAverageCostBetween(txn,  Ashton.from(stationRepository), mediaCity, when, modes));
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyBury() throws InvalidDurationException {
        // changes regularly with timetable updates

        final Station bury = Bury.from(stationRepository);
        final Duration buryToAlty = routeCostCalculator.getAverageCostBetween(txn, bury, altrincham, when, modes);
        final Duration altyToBury = routeCostCalculator.getAverageCostBetween(txn, altrincham, bury, when, modes);

        // often changes by a few seconds....
        assertEquals(Duration.ofMinutes(63), buryToAlty.truncatedTo(ChronoUnit.MINUTES));
        assertEquals(Duration.ofMinutes(65).plusSeconds(21), altyToBury);
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsMediaCityAirport() throws InvalidDurationException {
        assertEquals(Duration.ofMinutes(58), routeCostCalculator.getAverageCostBetween(txn, mediaCity, airport, when, modes));
        assertEquals(Duration.ofMinutes(60), routeCostCalculator.getAverageCostBetween(txn, airport, mediaCity, when, modes));
    }

}
