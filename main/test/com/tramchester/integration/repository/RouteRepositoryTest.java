package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.TramRouteId;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TFGMRouteNames;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.MultiMode;
import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TFGMRouteNames.*;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@MultiMode
@DataUpdateTest
public class RouteRepositoryTest {

    private static GuiceContainerDependencies componentContainer;
    private RouteRepository routeRepository;
    private TramRouteHelper routeHelper;
    private StationRepository stationRepository;
    private TramDate when;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeRepository = componentContainer.get(RouteRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        routeHelper = new TramRouteHelper(componentContainer);

        when = TestEnv.testDay();
    }

    @Test
    void shouldGetRouteWithHeadsigns() {
        Route result = routeHelper.getOneRoute(TFGMRouteNames.Navy, when);
        //assertEquals(getNavy(when).getId(), result.getId()); // ID's change frequently
        assertEquals(TestEnv.MetAgency(),result.getAgency());
        assertTrue(TransportMode.isTram(result));
    }

    @Test
    void shouldHaveExpectedRoutesAtDeansgate() {

        Station deansgate = Deansgate.from(stationRepository);

        Set<Route> pickups = deansgate.getPickupRoutes();

        Route traffordCenterRoute = routeHelper.getOneRoute(Red, when);

        assertTrue(pickups.contains(traffordCenterRoute), "Could not find " + traffordCenterRoute.getId()
                + " in " + summary(pickups));

    }

    private String summary(Set<Route> pickups) {
        return pickups.stream().
                map(route -> route.getId() + " " + route.getId() + " " + route.getDateRange() + System.lineSeparator()).
                collect(Collectors.joining());
    }

    @Disabled("appear to be no longer present")
    @Test
    void extraRouteAtShudehillTowardsEcclesFromVictoria() {
        Route towardsEcclesRoute = routeHelper.getOneRoute(Blue, when);

        List<Trip> ecclesTripsViaShudehill = towardsEcclesRoute.getTrips().stream().
                filter(trip -> trip.getStopCalls().getFirstStop(true).getStationId().equals(Ashton.getId())).
                filter(trip -> trip.callsAt(Shudehill.getId())).toList();

        List<StopCall> fromVictoria = ecclesTripsViaShudehill.stream().
                map(trip -> trip.getStopCalls().getFirstStop(true)).
                filter(stopCall -> stopCall.getStationId().equals(Victoria.getId())).
                toList();

        assertEquals(fromVictoria.size(), ecclesTripsViaShudehill.size(), ecclesTripsViaShudehill.toString());
    }

    @Test
    void shouldNotHaveRedRouteServingShudehill() {
        Route red = routeHelper.getOneRoute(Red, when);

        @NotNull Set<Trip> callingTrips = red.getTrips().stream().
                filter(trip -> trip.callsAt(Shudehill.getId())).
                filter(trip -> trip.serviceOperatesOn(when)).
                collect(Collectors.toSet());

        assertTrue(callingTrips.isEmpty(), HasId.asIds(callingTrips));
    }

    @Disabled("appear to be no longer present")
    @Test
    void extraRouteAtShudehillFromEcclesToVictoria() {
        Route ecclesRoute = routeHelper.getOneRoute(Blue, when);

        List<Trip> ecclesTripsViaShudehill = ecclesRoute.getTrips().stream().
                filter(trip -> trip.getStopCalls().getFirstStop(true).getStationId().equals(Ashton.getId())).
                filter(trip -> trip.getStopCalls().callsAt(Shudehill.getId())).
                toList();

        assertFalse(ecclesTripsViaShudehill.isEmpty());

        List<StopCall> toVictoria = ecclesTripsViaShudehill.stream().
                map(trip -> trip.getStopCalls().getLastStop(true)).
                filter(stopCall -> stopCall.getStationId().equals(Victoria.getId())).
                toList();

        assertFalse(toVictoria.isEmpty());

        assertEquals(toVictoria.size(), ecclesTripsViaShudehill.size(), ecclesTripsViaShudehill.toString());
    }

    @Test
    void shouldHaveEndOfLinesExpectedPickupAndDropoffRoutes() {
        Route fromBuryToAltrincham = routeHelper.getOneRoute(Green, when);

        Station endOfLine = stationRepository.getStationById(Altrincham.getId());

        assertTrue(endOfLine.servesRouteDropOff(fromBuryToAltrincham));

        Station notEndOfLine = stationRepository.getStationById(NavigationRoad.getId());

        assertTrue(notEndOfLine.servesRouteDropOff(fromBuryToAltrincham));
        assertTrue(notEndOfLine.servesRoutePickup(fromBuryToAltrincham));
    }

    @Test
    void shouldHaveExpectedNumberOfTramRoutesRunning() {
        IdSet<Route> running = routeRepository.getRoutesRunningOn(when, TransportMode.TramsOnly).stream().
                filter(route -> route.getTransportMode()==Tram).
                collect(IdSet.collector());

        // closures summer 2026
        assertEquals(8-1, running.size());

    }

    private static boolean isReplacementBus(Route bus) {
        return bus.getShortName().startsWith(TFGMRouteNames.Constants.REPLACEMENT_BUS_PREFIX);
    }

    @Test
    void shouldHaveExpectedNumberOfReplacementBusesRunning() {
        IdSet<Route> running = routeRepository.getRoutesRunningOn(when, TransportMode.TramsOnly).stream().
                filter(route -> route.getTransportMode()==Tram).
                filter(RouteRepositoryTest::isReplacementBus).
                collect(IdSet.collector());

        assertEquals(0, running.size());

    }

    @Test
    void shouldOverlapAsExpected() {

        Set<TFGMRouteNames> knownTram = Arrays.stream(TFGMRouteNames.values()).
                filter(route -> !route.isReplacementBus()).
                collect(Collectors.toSet());

        Set<RoutePair> noOverlap = new HashSet<>();

        UpcomingDates.daysAhead().forEach(date -> {

            for (TFGMRouteNames routeNameA : knownTram) {
                for (TFGMRouteNames routeNameB : knownTram) {
                    Optional<Route> maybeRouteA = maybeRouteForDate(date, routeNameA);
                    Optional<Route> maybeRouteB = maybeRouteForDate(date, routeNameB);
                    if (maybeRouteA.isPresent() && maybeRouteB.isPresent()) {
                        final Route routeA = maybeRouteA.get();
                        final Route routeB = maybeRouteB.get();
                        if (!routeA.isDateOverlap(routeB)) {
                            noOverlap.add(RoutePair.of(routeA, routeB));
                        }
                    }
                }
            }

            assertTrue(noOverlap.isEmpty(), "On " + date + " " + noOverlap);
        });

    }

    @Test
    void shouldReproduceIssueWithRoutesThatHaveNoOverlap() {
        // overlap blue route have appeared, cannot use Blue here
        UpcomingDates.daysAhead().forEach(date -> {
            Route route = routeHelper.getOneRoute(Red, date);
            assertTrue(route.isDateOverlap(route), date + " failed for route " + route);
        } );
    }


    @Test
    void shouldOverlapWithSelf() {

        Set<TFGMRouteNames> knownTram = Arrays.stream(TFGMRouteNames.values()).
                filter(route -> !route.isReplacementBus()).
                collect(Collectors.toSet());

        UpcomingDates.daysAhead().forEach(date -> {
            Set<Route> noOverlap = new HashSet<>();
            for (TFGMRouteNames name : knownTram) {
                Optional<Route> maybeRoute = maybeRouteForDate(date, name);
                if (maybeRoute.isPresent()) {
                    Route route = maybeRoute.get();
                    if (!route.isDateOverlap(route)) {
                        noOverlap.add(route);
                    }
                }
            }
            assertTrue(noOverlap.isEmpty(), date + " problem with self overlap for " + HasId.asIds(noOverlap));
        });
    }

    Optional<Route> maybeRouteForDate(TramDate date, TFGMRouteNames routeName) {
        return routeRepository.getRoutesRunningOn(date, TramsOnly).stream().
                filter(route -> ((TramRouteId) route.getId()).getRouteName() == routeName).
                findFirst();
    }

    @Test
    void shouldReproIssueWithUnsymmetricDateOverlap() {

        TramDate date =  when;

        Route routeA = routeHelper.getOneRoute(Yellow, date);
        Route routeB = routeHelper.getOneRoute(Green, date);

        assertTrue(routeA.isAvailableOn(date));
        assertTrue(routeB.isAvailableOn(date));

        assertTrue(routeA.isDateOverlap(routeB), "no overlap for " + routeA + " and " + routeB);
        assertTrue(routeB.isDateOverlap(routeA), "no overlap for " + routeB + " and " + routeA);
    }

    @Test
    void shouldReproduceIssuesStPetersSquareSept2026() {

        TramDate problemDate = TramDate.of(2026, 9, 19);
//        assertHaveOverlap(problemDate, StPetersSquare, PiccadillyGardens);
        assertHaveOverlap(problemDate, StPetersSquare, Piccadilly);
//        assertHaveOverlap(problemDate, StPetersSquare, NewIslington);
//        assertHaveOverlap(problemDate, StPetersSquare, HoltTown);
        assertHaveOverlap(problemDate, StPetersSquare, Etihad);

//        assertHaveOverlap(problemDate, Piccadilly, Etihad);
//        assertHaveOverlap(problemDate, Piccadilly, VeloPark);
        assertHaveOverlap(problemDate, Piccadilly, Ashton);

//        assertHaveOverlap(problemDate, NewIslington, Ashton);
        assertHaveOverlap(problemDate, Etihad, Ashton);
//        assertHaveOverlap(problemDate, VeloPark, Ashton);

        //assertHaveOverlap(problemDate, StPetersSquare, VeloPark);
//        assertHaveOverlap(problemDate, StPetersSquare, Ashton);

    }

    private void assertHaveOverlap(TramDate problemDate, TramStations start, TramStations end) {
        Set<Route> overlapRoutesFor = getOverlapRoutesFor(problemDate, start, end);
        assertFalse(overlapRoutesFor.isEmpty(), "no overlap between " + start + " and " + end);
    }

    private Set<Route> getOverlapRoutesFor(TramDate problemDate, TramStations startStart, TramStations endStation) {
        Station start = startStart.from(stationRepository);
        Station end = endStation.from(stationRepository);

        Set<Route> rawPickups = start.getPickupRoutes();
        Set<Route> pickupAvailable = rawPickups.stream().filter(route -> route.isAvailableOn(problemDate)).collect(Collectors.toSet());
        assertFalse(pickupAvailable.isEmpty());

        Set<Route> rawDropoffs = end.getDropoffRoutes();
        Set<Route> dropoffAvailable = rawDropoffs.stream().filter(route -> route.isAvailableOn(problemDate)).collect(Collectors.toSet());
        assertFalse(dropoffAvailable.isEmpty());

        return SetUtils.intersection(pickupAvailable, dropoffAvailable);
    }

    @Test
    void shouldHaveExpectedRoutesAtCornbrook() {
        TramRouteHelper tramRouteHelper = new TramRouteHelper(componentContainer);

        Station cornbrook = Cornbrook.from(stationRepository);

        TramDate date = TestEnv.testDay();

        Set<Route> cornbrookPickups = cornbrook.getPickupRoutes().stream().
                filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        Set<Route> cornbrookDropofss = cornbrook.getDropoffRoutes().stream().
                filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        // summer 2026 closures/buses
        int throughRoutes = 5+1; // might not match the map, which includes psuedo-routes that are made of trams running part of an existing route
        assertEquals(throughRoutes  , cornbrookPickups.size(), HasId.asIds(cornbrookPickups));
        assertEquals(throughRoutes , cornbrookDropofss.size(), HasId.asIds(cornbrookDropofss));


        String diagnostics = "Missing on " + when + " got ";
        String pickupDiag = diagnostics + HasId.asIds(cornbrookPickups);
        String dropoffDiag = diagnostics + HasId.asIds(cornbrookDropofss);

        Route buryToAlty = tramRouteHelper.getOneRoute(Green, when);

        assertTrue(cornbrookPickups.contains(buryToAlty), pickupDiag);
        assertTrue(cornbrookDropofss.contains(buryToAlty), dropoffDiag);

        // was Blue, now Yellow
        Route toEccles = tramRouteHelper.getOneRoute(Yellow, when);

        assertTrue(cornbrookPickups.contains(toEccles), pickupDiag);
        assertTrue(cornbrookDropofss.contains(toEccles), dropoffDiag);

        Route toTraffordCenter = tramRouteHelper.getOneRoute(Red, when);

        assertTrue(cornbrookPickups.contains(toTraffordCenter), pickupDiag);

        Route victoriaToAirport = tramRouteHelper.getOneRoute(Navy, when);

        assertTrue(cornbrookPickups.contains(victoriaToAirport), pickupDiag);
        assertTrue(cornbrookDropofss.contains(victoriaToAirport), dropoffDiag);

    }

}
