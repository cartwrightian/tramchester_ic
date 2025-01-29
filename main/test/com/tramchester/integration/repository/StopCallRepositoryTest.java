package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StopCallRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.*;
import static com.tramchester.testSupport.reference.TramStations.Victoria;
import static org.junit.jupiter.api.Assertions.*;

public class StopCallRepositoryTest {
    private static ComponentContainer componentContainer;
    private static IntegrationTramTestConfig config;

    private StopCallRepository stopCallRepository;
    private StationRepository stationRepository;
    private ServiceRepository serviceRepository;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
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
        serviceRepository = componentContainer.get(ServiceRepository.class);
        routeRepository = componentContainer.get(RouteRepository.class);
    }

    @Test
    void shouldGetStopCallsForAStation() {
        final TramDate date = TestEnv.testDay();
        Set<Service> servicesForDate = serviceRepository.getServicesOnDate(date, config.getTransportModes());

        final IdFor<Station> stationId = TramStations.ManAirport.getId();

        Station station = stationRepository.getStationById(stationId);

        final TramTime begin = TramTime.of(9, 0);
        final TramTime end = TramTime.of(10, 0);

        Set<StopCall> results = stopCallRepository.getStopCallsFor(station, date, begin, end);
        assertFalse(results.isEmpty());

        results.forEach(stopCall -> assertEquals(stationId, stopCall.getStationId()));

        boolean wrongService = results.stream().
                filter(stopCall -> !servicesForDate.contains(stopCall.getService())).
                findAny().isEmpty();
        assertTrue(wrongService);

        long correctTimes = results.stream().
                filter(stopCall -> stopCall.getArrivalTime().between(begin, end)).
                count();
        assertEquals(results.size(), correctTimes);

        assertEquals(5, results.size(), results.toString());
    }

    @Test
    void shouldGetCostsForAStopCall() {
        Route route = routeRepository.getRouteById(KnownTramRoute.BuryManchesterAltrincham.getId());
        assertNotNull(route);

        Station alty = stationRepository.getStationById(TramStations.Altrincham.getId());
        Station navigationRoad = stationRepository.getStationById(TramStations.NavigationRoad.getId());

        StopCallRepository.Costs costs = stopCallRepository.getCostsBetween(route, alty, navigationRoad);

        assertFalse(costs.isEmpty());
        assertTrue(costs.consistent(), costs.toString());

        assertEquals(Duration.ofMinutes(3), costs.min(), costs.toString());
    }

    @Test
    void shouldFindUniqueCallingPointsBetween() {
        List<IdFor<Station>> stations = stopCallRepository.getClosedBetween(OldTrafford.getId(), StPetersSquare.getId());

        assertEquals(5, stations.size());

        assertEquals(OldTrafford.getId(), stations.get(4));
        assertEquals(TraffordBar.getId(), stations.get(3));
        assertEquals(Cornbrook.getId(), stations.get(2));
        assertEquals(Deansgate.getId(), stations.get(1));
        assertEquals(StPetersSquare.getId(), stations.get(0));

    }

    @Test
    void shouldFindUniqueCallingPointsBetweenAdjacentStations() {
        List<IdFor<Station>> stations = stopCallRepository.getClosedBetween(NavigationRoad.getId(), Timperley.getId());

        assertEquals(2, stations.size());

        assertEquals(Timperley.getId(), stations.get(0));
        assertEquals(NavigationRoad.getId(), stations.get(1));

    }

    @Test
    void shouldFindUniqueCallingPointsEndOfALine() {
        List<IdFor<Station>> stations = stopCallRepository.getClosedBetween(Altrincham.getId(), Timperley.getId());

        assertEquals(3, stations.size());

        assertEquals(Timperley.getId(), stations.get(0));
        assertEquals(NavigationRoad.getId(), stations.get(1));
        assertEquals(Altrincham.getId(), stations.get(2));

    }

    @Test
    void shouldFailToFindUniqueSequenceIfAmbiguous() {
        assertThrows(RuntimeException.class, () -> stopCallRepository.getClosedBetween(StPetersSquare.getId(), Victoria.getId()));
    }
}
