package com.tramchester.integration.repository;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
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
import com.tramchester.testSupport.reference.TestRoute;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.MultiMode;
import com.tramchester.testSupport.testTags.Summer2026Closures;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.MutableAgency.METL;
import static com.tramchester.domain.reference.TFGMRouteNames.ReplacementBus_WORKAROUND;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
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
        assertEquals(getNavy(when).getId(), result.getId());
        assertEquals(TestEnv.MetAgency(),result.getAgency());
        assertTrue(TransportMode.isTram(result));
    }

    @Summer2026Closures
    @Test
    void shouldHaveExpectedRoutesAtDeansgate() {

        Station deansgate = Deansgate.from(stationRepository);

        Set<Route> pickups = deansgate.getPickupRoutes();

        Route traffordCenterRoute = routeHelper.getOneRoute(TFGMRouteNames.Red, when);

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
        Route towardsEcclesRoute = routeHelper.getOneRoute(TFGMRouteNames.Blue, when);

        List<Trip> ecclesTripsViaShudehill = towardsEcclesRoute.getTrips().stream().
                filter(trip -> trip.getStopCalls().getFirstStop(true).getStationId().equals(Ashton.getId())).
                filter(trip -> trip.callsAt(Shudehill.getId())).toList();

        List<StopCall> fromVictoria = ecclesTripsViaShudehill.stream().
                map(trip -> trip.getStopCalls().getFirstStop(true)).
                filter(stopCall -> stopCall.getStationId().equals(Victoria.getId())).
                toList();

        assertEquals(fromVictoria.size(), ecclesTripsViaShudehill.size(), ecclesTripsViaShudehill.toString());
    }

    @Summer2026Closures
    @Test
    void shouldNotHaveRedRouteServingShudehill() {
        Route red = routeHelper.getOneRoute(TFGMRouteNames.Red, when);

        @NotNull Set<Trip> callingTrips = red.getTrips().stream().
                filter(trip -> trip.callsAt(Shudehill.getId())).
                filter(trip -> trip.serviceOperatesOn(when)).
                collect(Collectors.toSet());

        assertTrue(callingTrips.isEmpty(), HasId.asIds(callingTrips));
    }

    @Disabled("appear to be no longer present")
    @Test
    void extraRouteAtShudehillFromEcclesToVictoria() {
        Route ecclesRoute = routeHelper.getOneRoute(TFGMRouteNames.Blue, when);

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

    @Summer2026Closures
    @Test
    void shouldHaveEndOfLinesExpectedPickupAndDropoffRoutes() {
        Route fromBuryToAltrincham = routeHelper.getOneRoute(TFGMRouteNames.Green, when);

        Station endOfLine = stationRepository.getStationById(Altrincham.getId());

        assertTrue(endOfLine.servesRouteDropOff(fromBuryToAltrincham));

        Station notEndOfLine = stationRepository.getStationById(NavigationRoad.getId());

        assertTrue(notEndOfLine.servesRouteDropOff(fromBuryToAltrincham));
        assertTrue(notEndOfLine.servesRoutePickup(fromBuryToAltrincham));
    }

    @Summer2026Closures
    @Test
    void shouldHaveExpectedNumberOfTramRoutesRunning() {
        IdSet<Route> running = routeRepository.getRoutesRunningOn(when, TransportMode.TramsOnly).stream().
                filter(route -> route.getTransportMode()==Tram).
                collect(IdSet.collector());

        IdSet<Route> knownTramRoutes = getFor(when).stream().
                map(TestRoute::getId).
                collect(IdSet.idCollector());

        ImmutableIdSet<Route> diffA = IdSet.disjunction(running, knownTramRoutes);
        assertTrue(diffA.isEmpty(), diffA.toString());

        assertEquals(knownTramRoutes.size(), running.size());

    }

    @Test
    void shouldHaveAltrinchamReplacementBuses() {
        List<Route> buses = routeRepository.findRoutesByName(METL, "Altrincham to Piccadilly Station").
                stream().toList();
        assertEquals(1, buses.size());
        Route bus = buses.getFirst();

        assertTrue(isReplacementBus(bus));
    }

    private static boolean isReplacementBus(Route bus) {
        return bus.getShortName().startsWith(ReplacementBus_WORKAROUND.getShortName());
    }

    @Test
    void shouldHaveExpectedNumberOfReplacementBusesRunning() {
        IdSet<Route> running = routeRepository.getRoutesRunningOn(when, TransportMode.TramsOnly).stream().
                filter(route -> route.getTransportMode()==Tram).
                filter(RouteRepositoryTest::isReplacementBus).
                collect(IdSet.collector());

        assertEquals(6, running.size());

    }

    @Summer2026Closures
    @Test
    void shouldOverlapAsExpected() {

        Set<TFGMRouteNames> knownTram = Arrays.stream(TFGMRouteNames.values()).
                filter(route -> !route.isReplacementBus()).
                collect(Collectors.toSet());

        Set<RoutePair> noOverlap = new HashSet<>();

        for (TFGMRouteNames knownRouteA : knownTram) {
            for (TFGMRouteNames knownRouteB : knownTram) {
                Route routeA = routeHelper.getOneRoute(knownRouteA, when);
                Route routeB = routeHelper.getOneRoute(knownRouteB, when);
                if (!routeA.isDateOverlap(routeB)) {
                    noOverlap.add(RoutePair.of(routeA, routeB));
                }
            }
        }

        assertTrue(noOverlap.isEmpty(), noOverlap.toString());

    }

    @Summer2026Closures
    @Test
    void shouldReproIssueWithUnsymmetricDateOverlap() {

        TramDate date =  when;

        Route routeA = routeHelper.getOneRoute(TFGMRouteNames.Yellow, date);
        Route routeB = routeHelper.getOneRoute(TFGMRouteNames.Green, date);

        assertTrue(routeA.isAvailableOn(date));
        assertTrue(routeB.isAvailableOn(date));

        assertTrue(routeA.isDateOverlap(routeB), "no overlap for " + routeA + " and " + routeB);
        assertTrue(routeB.isDateOverlap(routeA), "no overlap for " + routeB + " and " + routeA);
    }

    @Summer2026Closures
    @Test
    void shouldHaveExpectedRoutesAtCornbrook() {
        TramRouteHelper tramRouteHelper = new TramRouteHelper(componentContainer);

        Station cornbrook = Cornbrook.from(stationRepository);

        TramDate date = TestEnv.testDay();

        Set<Route> cornbrookPickups = cornbrook.getPickupRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());
        Set<Route> cornbrookDropofss = cornbrook.getDropoffRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        // summer 2026 closures/buses
        int throughRoutes = 5+3; // might not match the map, which includes psuedo-routes that are made of trams running part of an existing route
        assertEquals(throughRoutes  , cornbrookPickups.size(), HasId.asIds(cornbrookPickups));
        assertEquals(throughRoutes , cornbrookDropofss.size(), HasId.asIds(cornbrookDropofss));

        Route buryToAlty = tramRouteHelper.getOneRoute(TFGMRouteNames.Green, when);

        assertTrue(cornbrookPickups.contains(buryToAlty));
        assertTrue(cornbrookDropofss.contains(buryToAlty));

        assertTrue(cornbrookPickups.contains(buryToAlty));
        assertTrue(cornbrookDropofss.contains(buryToAlty));

        Route toEccles = tramRouteHelper.getOneRoute(TFGMRouteNames.Blue, when);

        assertTrue(cornbrookPickups.contains(toEccles));
        assertTrue(cornbrookDropofss.contains(toEccles));

        Route toTraffordCenter = tramRouteHelper.getOneRoute(TFGMRouteNames.Red, when);

        assertTrue(cornbrookPickups.contains(toTraffordCenter));

        Route victoriaToAirport = tramRouteHelper.getOneRoute(TFGMRouteNames.Navy, when);

        assertTrue(cornbrookPickups.contains(victoriaToAirport));
        assertTrue(cornbrookDropofss.contains(victoriaToAirport));

    }

    @Test
    void shouldHaveExpectedRoutesAtCornbrookSummer2026() {
        TramRouteHelper tramRouteHelper = new TramRouteHelper(componentContainer);

        Station cornbrook = Cornbrook.from(stationRepository);

        TramDate date = TestEnv.testDay();

        Set<Route> cornbrookPickups = cornbrook.getPickupRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());
        Set<Route> cornbrookDropofss = cornbrook.getDropoffRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        // summer 2026 closures/buses
        int throughRoutes = 5+1; // might not match the map, which includes psuedo-routes that are made of trams running part of an existing route
        assertEquals(throughRoutes  , cornbrookPickups.size(), HasId.asIds(cornbrookPickups));
        assertEquals(throughRoutes , cornbrookDropofss.size(), HasId.asIds(cornbrookDropofss));

        Route victoriaToAirport = tramRouteHelper.getOneRoute(TFGMRouteNames.Navy, when);
        assertFalse(cornbrookPickups.contains(victoriaToAirport));
        assertFalse(cornbrookDropofss.contains(victoriaToAirport));

//        Route altToPicc = routeHelper.requireByLongName(date, "Altrincham to Piccadilly Station");
//        assertTrue(cornbrookPickups.contains(altToPicc));
//        assertTrue(cornbrookDropofss.contains(altToPicc));

        Route piccToAlty = routeHelper.requireByLongName(date, "Piccadilly Station - Altrincham");
        assertTrue(cornbrookPickups.contains(piccToAlty));
        assertTrue(cornbrookDropofss.contains(piccToAlty));

        Route ecclesToPicc = routeHelper.requireByLongName(date, "Eccles - Piccadilly Station");
        assertTrue(cornbrookPickups.contains(ecclesToPicc));
        assertTrue(cornbrookDropofss.contains(ecclesToPicc));
    }

}
