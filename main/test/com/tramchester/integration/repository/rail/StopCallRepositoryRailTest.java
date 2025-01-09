package com.tramchester.integration.repository.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StopCallRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig.Scope.GreaterManchester;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
public class StopCallRepositoryRailTest {
    private static ComponentContainer componentContainer;

    private StopCallRepository stopCallRepository;
    private StationRepository stationRepository;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationRailTestConfig(GreaterManchester), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTest() {
        stopCallRepository = componentContainer.get(StopCallRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        routeRepository = componentContainer.get(RouteRepository.class);
    }

    @Test
    void shouldReproIssueWithCrossingMidnight() {

        List<Trip> crossMidnightTrips = routeRepository.getRoutes().stream().
                filter(Route::intoNextDay).
                flatMap(route -> route.getTrips().stream()).
                filter(Trip::intoNextDay).
                toList();

        assertFalse(crossMidnightTrips.isEmpty());

        List<Trip> trips = crossMidnightTrips.stream().
                filter(trip -> trip.getStopCalls().getLegs(false).stream().
                    anyMatch(leg -> !leg.getFirst().intoNextDay() && leg.getSecond().intoNextDay())).
                toList();

        assertFalse(trips.isEmpty());
        Trip trip = trips.get(0);

        List<StopCalls.StopLeg> legsIntoNextDay = trip.getStopCalls().getLegs(false).stream().
                filter(stopLeg -> !stopLeg.getFirst().intoNextDay()).
                filter(stopLeg -> stopLeg.getSecond().intoNextDay()).
                toList();

        assertFalse(legsIntoNextDay.isEmpty());

        StopCalls.StopLeg leg = legsIntoNextDay.get(0);

        Station firstStation = leg.getFirstStation();
        Station secondStation = leg.getSecondStation();

        StopCallRepository.Costs costs = stopCallRepository.getCostsBetween(trip.getRoute(), firstStation, secondStation);

        // crossing midnight costs where incorrectly >22 hours previously
        assertTrue(costs.max().compareTo(Duration.ofHours(22))<0);

    }

    @Disabled("Data does contain a zero cost trip X13514:20220124:20220127")
    @Test
    void shouldReproIssueWithIncorrectZeroCosts() {
        Station mulsecoomb = stationRepository.getStationById(Station.createId("MLSECMB"));
        Station londonRoadBrighton = stationRepository.getStationById(Station.createId("BRGHLRD"));

        Set<Route> calling = routeRepository.getRoutes().stream().
                flatMap(route -> route.getTrips().stream()).
                filter(trip -> trip.callsAt(mulsecoomb.getId()) && trip.callsAt(londonRoadBrighton.getId())).
                filter(trip -> isBefore(trip, mulsecoomb, londonRoadBrighton)).
                map(Trip::getRoute).
                collect(Collectors.toSet());

        assertFalse(calling.isEmpty());

        calling.forEach(route -> {
            StopCallRepository.Costs costs = stopCallRepository.getCostsBetween(route, mulsecoomb, londonRoadBrighton);
            assertNotEquals(Duration.ZERO, costs.average(), costs.toString() + route);
            assertNotEquals(Duration.ZERO, costs.max(), costs.toString() + route);
        });
    }

    private boolean isBefore(Trip trip, Station stationA, Station stationB) {
        final boolean includeNotStopping = true;
        return trip.getStopCalls().getStationSequence(includeNotStopping).
                indexOf(stationA) < trip.getStopCalls().getStationSequence(includeNotStopping).indexOf(stationB);
    }

}
