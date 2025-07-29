package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UnitTestOfGraphConfig;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("for diagnosing cache issue, repeat until fail")
class TramRouteTestCacheIssue {
    private static ComponentContainer componentContainer;
    private static UnitTestOfGraphConfig config;

    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;
    private RouteCalculator calculator;

    private TramDate queryDate;
    private GraphTransaction txn;

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        config = new UnitTestOfGraphConfig();
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        transportData = (TramTransportDataForTestFactory.TramTransportDataForTest) componentContainer.get(TransportData.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        calculator = componentContainer.get(RouteCalculator.class);

        queryDate = TramDate.of(2014,6,30);

        txn = database.beginTx();

    }

    @AfterEach
    void afterEachTestRuns() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
        txn.close();
    }

    @NotNull
    private JourneyRequest createJourneyRequest(TramTime queryTime, int maxChanges) {
        return new JourneyRequest(queryDate, queryTime, false, maxChanges,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 3, TramsOnly);
    }

    @Test
    void shouldTestSimpleJourneyIsNotPossible() {

        JourneyRequest journeyRequest = createJourneyRequest(TramTime.of(10, 0), 1);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getFirst(),
                transportData.getInterchange(),
                journeyRequest, () -> true).collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), journeys);
    }

    @Test
    void shouldTestJourneyInterchangeToFive() {
        JourneyRequest journeyRequest = createJourneyRequest(TramTime.of(7,56), 0);
        //journeyRequest.setDiag(true);

        Set<Journey> journeys = calculator.calculateRoute(txn, transportData.getInterchange(),
                transportData.getFifthStation(), journeyRequest, () -> true).collect(Collectors.toSet());
        assertTrue(journeys.isEmpty());

        JourneyRequest journeyRequestB = createJourneyRequest(TramTime.of(8, 10), 3);
        journeys = calculator.calculateRoute(txn, transportData.getInterchange(),
                transportData.getFifthStation(), journeyRequestB, () -> true).collect(Collectors.toSet());
        assertFalse(journeys.isEmpty());
        journeys.forEach(journey-> assertEquals(1, journey.getStages().size()));
    }

}
