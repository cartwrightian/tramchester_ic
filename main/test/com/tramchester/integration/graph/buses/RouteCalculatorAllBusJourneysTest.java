package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static org.junit.jupiter.api.Assertions.fail;

@BusTest
class RouteCalculatorAllBusJourneysTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    private TramDate when;
    private RouteCalculationCombinations<Station> combinations;
    private ClosedStationsRepository closedRepository;
    private EnumSet<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        modes = TramsOnly;
        combinations = new RouteCalculationCombinations<>(componentContainer);
        closedRepository = componentContainer.get(ClosedStationsRepository.class);
    }

    @Test
    void wip() {
        fail("todo");
    }

//    @Test
//    void shouldFindRouteEachStationToEveryOtherStream() {
//        TransportData data = componentContainer.get(TransportData.class);
//
//        final TramTime time = TramTime.of(8, 5);
//        Set<Station> haveServices = data.getStationsServing(Tram).stream().
//                filter(station -> !closedRepository.isClosed(station, when)).
//                collect(Collectors.toSet());
//
//        int maxChanges = 2;
//        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges,
//                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, modes);
//
//        // pairs of stations to check
//        Set<StationIdPair> stationIdPairs = haveServices.stream().flatMap(start -> haveServices.stream().
//                filter(dest -> !combinations.betweenInterchanges(start, dest)).
//                map(dest -> StationIdPair.of(start, dest))).
//                filter(pair -> !pair.same()).
//                // was here to avoid duplication....
//                //filter(pair -> !combinations.betweenEndsOfRoute(pair)).
//                collect(Collectors.toSet());
//
//        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);
//
//        List<RouteCalculationCombinations.JourneyOrNot<Station>> failed = results.getFailed();
//
//        assertEquals(0L, failed.size(), format("For %s Failed some of %s (finished %s) combinations %s",
//                    journeyRequest, results.size(), stationIdPairs.size(), combinations.displayFailed(failed)));
//
//    }


}
