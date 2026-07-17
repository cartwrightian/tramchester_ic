package com.tramchester.unit.repository;

import com.tramchester.config.BusReplacementRepository;
import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.TramRouteId;
import com.tramchester.domain.reference.TFGMRouteNames;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static org.junit.jupiter.api.Assertions.*;

public class BusReplacementRepositoryTest extends EasyMockSupport {

    private BusReplacementRepository repository;
    private RouteRepository routeRepository;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        routeRepository = createMock(RouteRepository.class);
        repository = new BusReplacementRepository(routeRepository);
    }

    @Test
    void shouldHaveSomeReplacementBuses() {
        IdFor<Route> routeIdA = TramRouteId.create(TFGMRouteNames.Pink, "idA");
        Route routeA = createRoute(routeIdA, "short name A", "A Tram Route");
        IdFor<Route> routeIdB = TramRouteId.create(TFGMRouteNames.ChorltonPiccadilly, "idB");
        Route routeB = createRoute(routeIdB, "Replacement Bus short name B", "Replacement Bus From X to Y");

        Set<Route> routes = Set.of(routeA, routeB);

        EasyMock.expect(routeRepository.getRoutes(TramsOnly)).andReturn(routes);

        replayAll();
        repository.start();
        assertTrue(repository.hasReplacementBuses());
        assertEquals(1, repository.number());
        assertTrue(repository.isReplacement(routeB.getId()));
        assertFalse(repository.isReplacement(routeA.getId()));

        repository.stop();
        verifyAll();
    }

    private static @NonNull Route createRoute(IdFor<Route> id, String shortName, String longName) {
        return MutableRoute.getRoute(id, shortName, longName, TestEnv.MetAgency(), TransportMode.Tram);
    }


}
