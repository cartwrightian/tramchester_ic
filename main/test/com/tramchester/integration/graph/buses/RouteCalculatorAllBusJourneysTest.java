package com.tramchester.integration.graph.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.collections.LocationIdPairSet;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;

@BusTest
class RouteCalculatorAllBusJourneysTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    private TramDate when;
    private RouteCalculationCombinations<StationGroup> combinations;
    private EnumSet<TransportMode> modes;
    private StationGroupsRepository stationGroupRepository;

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
        combinations = new RouteCalculationCombinations<>(componentContainer, RouteCalculationCombinations.checkGroupOpen(componentContainer));
        stationGroupRepository = componentContainer.get(StationGroupsRepository.class);
    }

    @Test
    void shouldFindRouteEachStationToEveryOtherStream() {

        final TramTime time = TramTime.of(8, 5);

        int maxChanges = 3;
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, modes);


        LocationIdPairSet<StationGroup> stationGroupPairs = stationGroupRepository.getStationGroupsFor(Bus).stream().
                flatMap(groupA -> stationGroupRepository.getStationGroupsFor(Bus).stream().
                        map(groupB -> LocationIdPair.of(groupA, groupB))).
                filter(pair -> !pair.same()).
                collect(LocationIdPairSet.collector());

        RouteCalculationCombinations.CombinationResults<StationGroup> results = combinations.getJourneysFor(stationGroupPairs, journeyRequest);

        List<RouteCalculationCombinations.JourneyOrNot<StationGroup>> failed = results.getFailed();

        assertEquals(0L, failed.size(), String.format("For %s Failed some of %s (finished %s) combinations %s",
                    journeyRequest, results.size(), stationGroupPairs.size(), combinations.displayFailed(failed)));

    }


}
