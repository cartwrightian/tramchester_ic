package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.core.GraphDatabase;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphPath;
import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.inMemory.SpikeAlgo;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UnitTestOfGraphConfig;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("WIP")
class TramRouteTestSpikeInMemoryTest {

    private static ComponentContainer componentContainer;
    private static UnitTestOfGraphConfig config;
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
    }

    @AfterEach
    void afterEachTestRuns()
    {
        if (txn!=null) {
            txn.close();
        }
    }

    @Test
    void shouldTestSimpleJourneyIsPossible() {
        //JourneyRequest journeyRequest = createJourneyRequest(queryTime, 0);

        Station begin = transportData.getFirst();
        Station destination = transportData.getInterchange();

        GraphNode beginNode = txn.findNode(begin);
        GraphNode destNode = txn.findNode(destination);

        SpikeAlgo spikeAlgo = new SpikeAlgo(txn, beginNode, destNode, config);

        GraphPath result = spikeAlgo.findRoute();

        assertEquals(beginNode, result.getStartNode(txn));
        assertEquals(destNode, result.getEndNode(txn));

        assertEquals(3,result.length());
    }

}
