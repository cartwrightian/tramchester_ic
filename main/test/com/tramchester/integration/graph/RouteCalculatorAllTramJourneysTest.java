package com.tramchester.integration.graph;

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
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
public class RouteCalculatorAllTramJourneysTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig testConfig;

    private TramDate when;
    private RouteCalculationCombinations<Station> combinations;
    private InterchangeRepository interchangeRepository;
    private EnumSet<TransportMode> modes;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig config) {
        testConfig = config;
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
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
        combinations = new RouteCalculationCombinations<>(componentContainer, RouteCalculationCombinations.checkStationOpen(componentContainer) );
    }

    @Test
    void shouldFindRouteEachStationToEveryOtherStream() {

        LocationIdPairSet<Station> stationIdPairs = combinations.getCreatePairs(when).
                createStationPairsForAll(TramsOnly);

        final TramTime time = TramTime.of(8, 5);

        JourneyRequest.MaxNumberOfChanges maxChanges = JourneyRequest.MaxNumberOfChanges.of(2);

        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges,
                Duration.ofMinutes(testConfig.getMaxJourneyDuration()), 1, modes);

        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);

        List<RouteCalculationCombinations.JourneyOrNot<Station>> failed = results.getFailed();

        assertEquals(0L, failed.size(), format("For %s Failed some of %s (finished %s) combinations %s",
                    journeyRequest, results.size(), stationIdPairs.size(), combinations.displayFailed(failed)));

    }



}
