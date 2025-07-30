package com.tramchester.integration.graph.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;

import java.util.EnumSet;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;

@TrainTest
public class RailRouteCostsTest {
    private static ComponentContainer componentContainer;

    private GraphTransaction txn;
    private RouteCostCalculator routeCostCalculator;
    private Station stockport;
    private Station manPicc;
    private Station wilmslow;

    private final TramDate date = TestEnv.testDay();
    private EnumSet<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationRailTestConfig(IntegrationRailTestConfig.Scope.GreaterManchester);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        txn = database.beginTx();
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        routeCostCalculator = componentContainer.get(RouteCostCalculator.class);

        stockport = Stockport.from(stationRepository);
        manPicc = ManchesterPiccadilly.from(stationRepository);
        wilmslow = Wilmslow.from(stationRepository);

        modes = EnumSet.of(TransportMode.Train, TransportMode.RailReplacementBus);
    }

    /***
     * May need graph rebuild if a fix made to data import code
     */

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldGetApproxCostBetweenStockportAndManPiccadilly() throws InvalidDurationException {
        assertMinutesEquals(13, routeCostCalculator.getAverageCostBetween(txn, stockport, manPicc, date, modes));
    }

    @Test
    void shouldGetApproxCostBetweenManPiccadillyAndStockport() throws InvalidDurationException {
        assertMinutesEquals(17, routeCostCalculator.getAverageCostBetween(txn, manPicc, stockport, date, modes));
    }

    @Test
    void shouldGetApproxCostBetweenStockportAndWilmslow() throws InvalidDurationException {
        assertMinutesEquals(12, routeCostCalculator.getAverageCostBetween(txn, stockport, wilmslow, date, modes));
    }

//    @Test
//    void shouldGetApproxCostBetweenWilmslowAndCrewe() throws InvalidDurationException {
//        assertMinutesEquals(22, routeCostCalculator.getAverageCostBetween(txn, wilmslow, crewe, date, modes));
//    }

//    @Test
//    void shouldGetApproxCostCreweAndMiltonKeeny() throws InvalidDurationException {
//        assertEquals(Duration.ofHours(1).plusMinutes(14), routeCostCalculator.getAverageCostBetween(txn, crewe, miltonKeynes, date, modes));
//    }

//    @Test
//    void shouldGetApproxCostMiltonKeynesLondon() throws InvalidDurationException {
//        assertMinutesEquals(38, routeCostCalculator.getAverageCostBetween(txn, miltonKeynes, londonEuston, date, modes));
//    }

//    @Test
//    void shouldGetApproxCostBetweenManPicadillyAndLondonEuston() throws InvalidDurationException {
//        assertEquals(Duration.ofHours(2).plusMinutes(14), routeCostCalculator.getAverageCostBetween(txn, manPicc, londonEuston, date, modes));
//    }

//    @Test
//    void shouldGetApproxCostBetweenAltrinchamAndLondonEuston() throws InvalidDurationException {
//        Station altrincham = Altrincham.from(stationRepository);
//
//        assertEquals(Duration.ofHours(2).plusMinutes(24), routeCostCalculator.getAverageCostBetween(txn, altrincham, londonEuston, date, modes));
//    }

    // There is a zero cost for this journey, but only between specific dates 24/1 until 27/1 2022
    // BS N X13514 220124 220127 1111000 5BR0B00    124748000                              N
//    @Test
//    void shouldReproIssueWithZeroCostLegs() throws InvalidDurationException {
//
//        Station mulsecoomb = stationRepository.getStationById(StringIdFor.createId("MLSECMB"));
//        Station londonRoadBrighton = stationRepository.getStationById(StringIdFor.createId("BRGHLRD"));
//        Duration result = routeCostCalculator.getAverageCostBetween(txn, mulsecoomb, londonRoadBrighton, date);
//
//        assertNotEquals(Duration.ZERO, result);
//    }

}
