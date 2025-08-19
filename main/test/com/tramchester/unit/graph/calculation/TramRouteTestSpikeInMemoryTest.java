package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.TramRouteCalculator;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TramRouteTestSpikeInMemoryTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;

    private MutableGraphTransaction txn;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleGroupedGraphConfig(true);
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = (TramTransportDataForTestFactory.TramTransportDataForTest) componentContainer.get(TransportData.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);

        txn = database.beginTxMutable();

        // force DB build
        StagedTransportGraphBuilder transportGraphBuilder = componentContainer.get(StagedTransportGraphBuilder.class);
        transportGraphBuilder.getReady();

        config = componentContainer.get(TramchesterConfig.class);
    }

    @AfterEach
    void afterEachTestRuns()
    {
        if (txn!=null) {
            txn.close();
        }
    }

    @Test
    void shouldDoInMemoryRouteCalc() {
        TramRouteCalculator tramRouteCalculator = componentContainer.get(TramRouteCalculator.class);

        Station begin = transportData.getFirst();
        Station dest = transportData.getFifthStation();

        TramDate queryDate = TramTransportDataForTestFactory.startDate; //   TramDate.of(2014,6,30);
        TramTime queryTime = TramTime.of(7, 57);

        JourneyRequest request = standardJourneyRequest(queryDate, queryTime, 3,1);
        Running running = () -> true;

        request.setDiag(true);

        List<Journey> result = tramRouteCalculator.calculateRoute(txn, begin, dest, request, running).toList();

        assertFalse(result.isEmpty());

    }

    private JourneyRequest standardJourneyRequest(TramDate date, TramTime time, long maxNumberJourneys, int maxNumberChanges) {
        Duration maxDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        return new JourneyRequest(date, time, false, maxNumberChanges, maxDuration, maxNumberJourneys, EnumSet.of(Tram));
    }

}
