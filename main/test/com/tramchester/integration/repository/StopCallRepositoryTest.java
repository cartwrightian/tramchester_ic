package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
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
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.conditional.BuryWorksSummer2025;
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
import static org.junit.jupiter.api.Assertions.*;

public class StopCallRepositoryTest {
    private static ComponentContainer componentContainer;
    private static IntegrationTramTestConfig config;

    private StopCallRepository stopCallRepository;
    private StationRepository stationRepository;
    private ServiceRepository serviceRepository;
    private RouteRepository routeRepository;
    private TramDate when;

    private static final IdFor<Station> freeHold = Station.createId("9400ZZMAFRE");

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
        when = TestEnv.testDay();
    }

    @Test
    void shouldGetStopCallsForAStation() {
        Set<Service> servicesForDate = serviceRepository.getServicesOnDate(when, config.getTransportModes());

        final IdFor<Station> stationId = TramStations.ManAirport.getId();

        Station station = stationRepository.getStationById(stationId);

        final TramTime begin = TramTime.of(9, 0);
        final TramTime end = TramTime.of(10, 0);

        Set<StopCall> results = stopCallRepository.getStopCallsFor(station, when, begin, end);
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
        TramDate when = TestEnv.testDay();
        Route route = routeRepository.getRouteById(KnownTramRoute.getGreen(when).getId());
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
        List<IdFor<Station>> stations = stopCallRepository.getStopcallsBetween(OldTrafford.getId(), StPetersSquare.getId(), when);

        assertEquals(5, stations.size());

        assertEquals(OldTrafford.getId(), stations.get(0));
        assertEquals(TraffordBar.getId(), stations.get(1));
        assertEquals(Cornbrook.getId(), stations.get(2));
        assertEquals(Deansgate.getId(), stations.get(3));
        assertEquals(StPetersSquare.getId(), stations.get(4));
    }

    @Test
    void shouldFindUniqueCallingPointsBetweenAdjacentStations() {
        List<IdFor<Station>> stationsFwd = stopCallRepository.getStopcallsBetween(NavigationRoad.getId(), Timperley.getId(), when);

        assertEquals(2, stationsFwd.size());

        assertEquals(NavigationRoad.getId(), stationsFwd.get(0));
        assertEquals(Timperley.getId(), stationsFwd.get(1));

        List<IdFor<Station>> stationsBack = stopCallRepository.getStopcallsBetween(Timperley.getId(), NavigationRoad.getId(), when);

        assertEquals(2, stationsBack.size());

        assertEquals(Timperley.getId(), stationsBack.get(0));
        assertEquals(NavigationRoad.getId(), stationsBack.get(1));

    }

    @Test
    void shouldFindUniqueCallingPointsEndOfALine() {
        List<IdFor<Station>> stations = stopCallRepository.getStopcallsBetween(Altrincham.getId(), Timperley.getId(), when);

        assertEquals(3, stations.size());

        assertEquals(Timperley.getId(), stations.get(2));
        assertEquals(NavigationRoad.getId(), stations.get(1));
        assertEquals(Altrincham.getId(), stations.get(0));
    }

    @Test
    void shouldFindUniqueCallingPointsEndOfALineDateRange() {
        DateRange range = DateRange.of(when, when.plusWeeks(1));
        List<IdFor<Station>> stations = stopCallRepository.getStopcallsBetween(Altrincham.getId(), Timperley.getId(), range);

        assertEquals(3, stations.size());

        assertEquals(Timperley.getId(), stations.get(2));
        assertEquals(NavigationRoad.getId(), stations.get(1));
        assertEquals(Altrincham.getId(), stations.get(0));
    }

    @Test
    void shouldFailToFindUniqueSequenceIfAmbiguous() {
        assertThrows(RuntimeException.class, () -> stopCallRepository.getStopcallsBetween(StPetersSquare.getId(), Victoria.getId(), when));
    }

    @BuryWorksSummer2025
    @Test
    void shouldDoublecheckStopsForClosuresCrumpsalToBury() {
        List<IdFor<Station>> stopsBetween = stopCallRepository.getStopcallsBetween(Crumpsal.getId(), Bury.getId(), when);
        assertEquals(stopsBetween, UpcomingDates.CrumpsalToBury);
    }

    @BuryWorksSummer2025
    @Test
    void shouldDoublecheckStopsForClosuresWhitefieldToBury() {
        List<IdFor<Station>> stopsBetween = stopCallRepository.getStopcallsBetween(Whitefield.getId(), Bury.getId(), when);
        assertEquals(stopsBetween, UpcomingDates.WhitefieldToBury);
    }

    @Test
    void shouldHaveExpectedOrdering() {
        List<IdFor<Station>> stopsBetween = stopCallRepository.getStopcallsBetween(freeHold, Rochdale.getId(), when);

        assertEquals(freeHold, stopsBetween.getFirst());
        assertEquals(Rochdale.getId(), stopsBetween.getLast());
    }

    // Freehold and Rochdale Town Centre
    @Test
    void shouldHaveExpectedClosuresForMay2025Rochdale() {
        List<IdFor<Station>> stopsBetween = stopCallRepository.getStopcallsBetween(freeHold, Rochdale.getId(), when);
        assertEquals(stopsBetween, UpcomingDates.RochdaleLineStations);
    }
}
