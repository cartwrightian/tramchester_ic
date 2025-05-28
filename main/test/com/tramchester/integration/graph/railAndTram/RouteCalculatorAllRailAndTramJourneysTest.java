package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.collections.LocationIdPairSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;


// see also performanceTestGM in gradle for another way to run this same test

@Disabled("performance testing only - too slow")
@GMTest
public class RouteCalculatorAllRailAndTramJourneysTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    private TramDate when;
    private RouteCalculationCombinations<Station> combinations;
    private EnumSet<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new RailAndTramGreaterManchesterConfig();
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
        modes = EnumSet.of(Tram, Train);
        combinations = new RouteCalculationCombinations<>(componentContainer,
                RouteCalculationCombinations.checkStationOpen(componentContainer) );
    }

    @Test
    void shouldFindRouteEachStationToEveryOtherStream() {
        StationRepository stationRepository = componentContainer.get(StationRepository.class);

        LocationIdPairSet<Station> stationIdPairs = RouteCalculationCombinations.createStationPairs(stationRepository,
                when, modes);

        final TramTime time = TramTime.of(8, 5);

        JourneyRequest.MaxNumberOfChanges maxChanges = JourneyRequest.MaxNumberOfChanges.of(2);

        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, modes);

        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);

        List<RouteCalculationCombinations.JourneyOrNot<Station>> failed = results.getFailed();

        assertEquals(0L, failed.size(), format("For %s Failed some of %s (finished %s) combinations %s %s",
                    journeyRequest, results.size(), stationIdPairs.size(),
                failed.size(), combinations.displayFailed(failed)));

    }



}
