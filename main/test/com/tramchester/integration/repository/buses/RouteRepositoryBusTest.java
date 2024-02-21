package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.integration.repository.buses.TransportDataFromFilesBusTest.TGFM_BUS_ROUTES;
import static com.tramchester.testSupport.TestEnv.StagecoachManchester;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BusTest
public class RouteRepositoryBusTest {
    private static GuiceContainerDependencies componentContainer;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationBusTestConfig config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeRepository = componentContainer.get(RouteRepository.class);
    }

    @Test
    void shouldFindLargestNumberOfHopsSPIKE() {
        OptionalLong findMostStopCalls = routeRepository.getRoutes().stream().
                flatMap(route -> route.getTrips().stream()).
                mapToLong(trip -> trip.getStopCalls().totalNumber()).
                max();

        assertTrue(findMostStopCalls.isPresent());

        long most = findMostStopCalls.getAsLong();

        assertEquals(121L, most);
    }

    @Test
    void shouldFindTotalTopFiveSpike() {
        long total = routeRepository.getRoutes().stream().
                flatMap(route -> route.getTrips().stream()).
                map(trip -> trip.getStopCalls().totalNumber()).
                sorted((a,b) -> Long.compare(b,a)).
                limit(5).mapToLong(i -> i).sum();

        assertEquals(605L , total);
    }

    @Test
    void shouldHaveRouteNumbersForBus() {
        int numberRoutes = routeRepository.getRoutes().size();
        assertWithinNPercent(TGFM_BUS_ROUTES, numberRoutes, 0.1F);
    }

    @Test
    void shouldGetSpecificBusRoutes() {
        Collection<Route> results = routeRepository.getRoutes();
        long gmsRoutes = results.stream().filter(route -> route.getAgency().equals(StagecoachManchester)).count();
        assertWithinNPercent(157, gmsRoutes, 0.1F);
    }

    @Test
    void shouldGetOnlyBusRoutes() {
        Collection<Route> results = routeRepository.getRoutes();
        long notBus = results.stream().filter(route -> !route.getTransportMode().equals(Bus)).count();
        assertEquals(0, notBus);
    }

    @Test
    void shouldHaveRoutesWithServices() {
        Set<Route> routes = routeRepository.getRoutes();

        Set<Route> noSvcs = routes.stream().filter(route -> route.getServices().isEmpty()).collect(Collectors.toSet());

        //assertTrue(noSvcs.isEmpty(), HasId.asIds(noSvcs));
        // see next test
        assertEquals(0, noSvcs.size(),  HasId.asIds(noSvcs));

    }

    // for things changing very frequently
    private void assertWithinNPercent(long expected, long actual, float percentage) {
        int margin = Math.round(expected * percentage);
        long upper = expected + margin;
        long lower = expected - margin;

        String diagnostic = String.format("%s not within %s of %s", actual, percentage, expected);
        assertTrue( (actual>lower) && (actual<upper), diagnostic);
    }
}
