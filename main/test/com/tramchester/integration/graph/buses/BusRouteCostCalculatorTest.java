package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.util.EnumSet;
import java.util.function.BiFunction;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.reference.BusStations.*;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
class BusRouteCostCalculatorTest {
    private static ComponentContainer componentContainer;

    private GraphTransaction txn;
    private RouteCostCalculator routeCost;
    private StationLocalityGroup altrincham;
    private StationLocalityGroup stockport;
    private StationLocalityGroup shudehill;
    private StationRepository stationRepository;

    private final TramDate date = TestEnv.testDay();
    private EnumSet<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {

        stationRepository = componentContainer.get(StationRepository.class);
        StationGroupsRepository stationGroupRepository = componentContainer.get(StationGroupsRepository.class);

        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        altrincham = KnownLocality.Altrincham.from(stationGroupRepository);
        stockport = KnownLocality.Stockport.from(stationGroupRepository);
        shudehill = KnownLocality.Shudehill.from(stationGroupRepository);

        modes = EnumSet.of(Bus);

        routeCost = componentContainer.get(RouteCostCalculator.class);

        txn = database.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveStations() {
        assertNotNull(altrincham);
        assertNotNull(stockport);
        assertNotNull(shudehill);
    }

    @Test
    void shouldFindCostsCorrectlyForAltyStockportComp() {
        assertEquals(Durations.of(38,38), getCostBetween(average(), altrincham, stockport));
        assertEquals(Durations.of(43,21), getCostBetween(average(), stockport, altrincham));
    }

    @Test
    void shouldFindCostsCorrectlyForAltyStockport() {
        assertEquals(Durations.of(46, 35), getCost(average(), StopAtAltrinchamInterchange, StockportNewbridgeLane));
        assertEquals(Durations.of(51,18), getCost(average(), StockportNewbridgeLane, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillAltyComp() {
        assertEquals(Durations.of(52,47), getCostBetween(average(), altrincham, shudehill));
        assertEquals(Durations.of(56,25), getCostBetween(average(), shudehill, altrincham));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillAlty() {
        assertEquals(Durations.of(53,47), getCost(average(), StopAtAltrinchamInterchange, StopAtShudehillInterchange));
        assertEquals(Durations.of(57,25), getCost(average(), StopAtShudehillInterchange, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillStockportComp() {
        assertEquals(Durations.of(42,29), getCostBetween(average(), shudehill, stockport));
        assertEquals(Durations.of(33,34), getCostBetween(average(), stockport, shudehill));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillStockport() {
        assertEquals(Durations.of(50,22), getCost(average(), StopAtShudehillInterchange, StockportNewbridgeLane));
        assertEquals(Durations.of(41,16), getCost(average(), StockportNewbridgeLane, StopAtShudehillInterchange));
    }

    private BiFunction<Location<?>, Location<?>, TramDuration> average() {
        return this::getAverageCostBetween;
    }

    private TramDuration getAverageCostBetween(Location<?> start, Location<?> finish) {
        try {
            return routeCost.getAverageCostBetween(txn, start, finish, date, modes);
        } catch (InvalidDurationException e) {
            fail("Unexpected exception", e);
            return TramDuration.ZERO;
        }
    }

    private TramDuration getCost(BiFunction<Location<?>, Location<?>, TramDuration> function, BusStations start, BusStations end) {
        return getCostBetween(function, start.from(stationRepository), end.from(stationRepository));
    }

    private TramDuration getCostBetween(BiFunction<Location<?>, Location<?>, TramDuration> function, Location<?> start, Location<?> end) {
        return function.apply(start, end);
    }


}
