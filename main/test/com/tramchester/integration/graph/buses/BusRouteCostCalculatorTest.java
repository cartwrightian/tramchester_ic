package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiFunction;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.BusStations.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@BusTest
class BusRouteCostCalculatorTest {
    private static ComponentContainer componentContainer;

    private MutableGraphTransaction txn;
    private RouteCostCalculator routeCost;
    private StationGroup altrincham;
    private StationGroup stockport;
    private StationGroup shudehill;
    private StationRepository stationRepository;

    private final TramDate date = TestEnv.testDay();
    private Set<TransportMode> modes;

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

        txn = database.beginTxMutable();
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
        assertMinutesEquals(45, getCostBetween(average(), altrincham, stockport));
        assertMinutesEquals(52, getCostBetween(average(), stockport, altrincham));
    }

    @Test
    void shouldFindCostsCorrectlyForAltyStockport() {
        assertMinutesEquals(48, getCost(average(), StopAtAltrinchamInterchange, StockportNewbridgeLane));
        assertMinutesEquals(54, getCost(average(), StockportNewbridgeLane, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillAltyComp() {
        assertMinutesEquals(54, getCostBetween(average(), altrincham, shudehill));
        assertMinutesEquals(58, getCostBetween(average(), shudehill, altrincham));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillAlty() {
        assertMinutesEquals(55, getCost(average(), StopAtAltrinchamInterchange, StopAtShudehillInterchange));
        assertMinutesEquals(58, getCost(average(), StopAtShudehillInterchange, StopAtAltrinchamInterchange));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillStockportComp() {
        assertMinutesEquals(55, getCostBetween(average(), shudehill, stockport));
        assertMinutesEquals(37, getCostBetween(average(), stockport, shudehill));
    }

    @Test
    void shouldFindCostsCorrectlyForShudehillStockport() {
        assertMinutesEquals(60, getCost(average(), StopAtShudehillInterchange, StockportNewbridgeLane));
        assertMinutesEquals(42, getCost(average(), StockportNewbridgeLane, StopAtShudehillInterchange));
    }

    private BiFunction<Location<?>, Location<?>, Duration> average() {
        return this::getAverageCostBetween;
    }

    private Duration getAverageCostBetween(Location<?> start, Location<?> finish) {
        try {
            return routeCost.getAverageCostBetween(txn, start, finish, date, modes);
        } catch (InvalidDurationException e) {
            fail("Unexpected exception", e);
            return Duration.ZERO;
        }
    }

    private Duration getCost(BiFunction<Location<?>, Location<?>, Duration> function, BusStations start, BusStations end) {
        return getCostBetween(function, start.from(stationRepository), end.from(stationRepository));
    }

    private Duration getCostBetween(BiFunction<Location<?>, Location<?>, Duration> function, Location<?> start, Location<?> end) {
        return function.apply(start, end);
    }


}
