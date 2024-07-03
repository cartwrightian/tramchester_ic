package com.tramchester.integration.repository;

import com.google.common.collect.Sets;
import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
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
        routeHelper = new TramRouteHelper(routeRepository);

        when = TestEnv.testDay();
    }

    @Test
    void shouldGetRouteWithHeadsigns() {
        Route result = routeHelper.getOneRoute(EcclesManchesterAshtonUnderLyne, when);
        assertEquals("Eccles - Manchester - Ashton-Under-Lyne", result.getName());
        assertEquals(TestEnv.MetAgency(),result.getAgency());
        //assertTrue(IdForDTO.createFor(result).getActualId().startsWith("METLBLUE:I:"));
        assertTrue(TransportMode.isTram(result));
    }

    @Test
    void shouldHaveExpectedRoutesAtDeansgate() {
        // according to the map this should not be the case, but it does seem to be in the timetable

        Station deansgate = Deansgate.from(stationRepository);

        Set<Route> pickups = deansgate.getPickupRoutes();

        Route traffordCenterRoute = routeHelper.getOneRoute(CornbrookTheTraffordCentre, when);

        assertTrue(pickups.contains(traffordCenterRoute), HasId.asIds(pickups));

    }

    @Disabled("appear to be no longer present")
    @Test
    void extraRouteAtShudehillTowardsEcclesFromVictoria() {
        Route towardsEcclesRoute = routeHelper.getOneRoute(EcclesManchesterAshtonUnderLyne, when);

        List<Trip> ecclesTripsViaShudehill = towardsEcclesRoute.getTrips().stream().
                filter(trip -> trip.getStopCalls().getFirstStop().getStationId().equals(Ashton.getId())).
                filter(trip -> trip.callsAt(Shudehill.getId())).toList();

        List<StopCall> fromVictoria = ecclesTripsViaShudehill.stream().
                map(trip -> trip.getStopCalls().getFirstStop()).
                filter(stopCall -> stopCall.getStationId().equals(Victoria.getId())).
                toList();

        assertEquals(fromVictoria.size(), ecclesTripsViaShudehill.size(), ecclesTripsViaShudehill.toString());
    }

    @Disabled("appear to be no longer present")
    @Test
    void extraRouteAtShudehillFromEcclesToVictoria() {
        Route ecclesRoute = routeHelper.getOneRoute(EcclesManchesterAshtonUnderLyne, when);

        List<Trip> ecclesTripsViaShudehill = ecclesRoute.getTrips().stream().
                filter(trip -> trip.getStopCalls().getFirstStop().getStationId().equals(Ashton.getId())).
                filter(trip -> trip.getStopCalls().callsAt(Shudehill)).
                toList();

        assertFalse(ecclesTripsViaShudehill.isEmpty());

        List<StopCall> toVictoria = ecclesTripsViaShudehill.stream().
                map(trip -> trip.getStopCalls().getLastStop()).
                filter(stopCall -> stopCall.getStationId().equals(Victoria.getId())).
                toList();

        assertFalse(toVictoria.isEmpty());

        assertEquals(toVictoria.size(), ecclesTripsViaShudehill.size(), ecclesTripsViaShudehill.toString());
    }

    @Test
    void shouldHaveEndOfLinesExpectedPickupAndDropoffRoutes() {
        Route fromBuryToAltrincham = routeHelper.getOneRoute(BuryManchesterAltrincham, when);

        Station endOfLine = stationRepository.getStationById(Altrincham.getId());

        assertTrue(endOfLine.servesRouteDropOff(fromBuryToAltrincham));

        Station notEndOfLine = stationRepository.getStationById(NavigationRoad.getId());

        assertTrue(notEndOfLine.servesRouteDropOff(fromBuryToAltrincham));
        assertTrue(notEndOfLine.servesRoutePickup(fromBuryToAltrincham));
    }

    @Test
    void shouldHaveExpectedNumberOfTramRoutesRunning() {
        Set<String> running = routeRepository.getRoutesRunningOn(when).stream().
                filter(route -> route.getTransportMode()==Tram).
                map(Route::getName).collect(Collectors.toSet());

        Set<String> knownTramRoutes = getFor(when).stream().map(KnownTramRoute::longName).collect(Collectors.toSet());

        Sets.SetView<String> diffA = Sets.difference(running, knownTramRoutes);
        assertTrue(diffA.isEmpty(), diffA.toString());

        Sets.SetView<String> diffB = Sets.difference(knownTramRoutes, running);
        assertTrue(diffB.isEmpty(), diffB.toString());

        assertEquals(knownTramRoutes.size(), running.size());
    }

    @Test
    void shouldOverlapAsExpected() {

        Set<KnownTramRoute> known = KnownTramRoute.getFor(when);
        Set<RoutePair> noOverlap = new HashSet<>();

        for (KnownTramRoute knownRouteA : known) {
            for (KnownTramRoute knownRouteB : known) {
                Route routeA = routeHelper.getOneRoute(knownRouteA, when);
                Route routeB = routeHelper.getOneRoute(knownRouteB, when);
                if (!routeA.isDateOverlap(routeB)) {
                    noOverlap.add(RoutePair.of(routeA, routeB));
                }
            }
        }

        assertTrue(noOverlap.isEmpty(), noOverlap.toString());

    }

    @Test
    void shouldReproIssueWithUnsymmetricDateOverlap() {

        TramDate date =  when;

        Route routeA = routeHelper.getOneRoute(PiccadillyBury, date);
        Route routeB = routeHelper.getOneRoute(BuryManchesterAltrincham, date);

        assertTrue(routeA.isAvailableOn(date));
        assertTrue(routeB.isAvailableOn(date));

        assertTrue(routeA.isDateOverlap(routeB), "no overlap for " + routeA + " and " + routeB);
        assertTrue(routeB.isDateOverlap(routeA), "no overlap for " + routeB + " and " + routeA);
    }

    @Test
    void shouldHaveExpectedRoutesAtCornbrook() {
        TramRouteHelper tramRouteHelper = new TramRouteHelper(routeRepository);

        Station cornbrook = Cornbrook.from(stationRepository);

        TramDate date = TestEnv.testDay();

        Set<Route> cornbrookPickups = cornbrook.getPickupRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());
        Set<Route> cornbrookDropofss = cornbrook.getDropoffRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        int throughRoutes = 6; // might not match the map, which includes psuedo-routes that are made of trams running part of an existing route
        assertEquals(throughRoutes  , cornbrookPickups.size(), HasId.asIds(cornbrookPickups));
        assertEquals(throughRoutes , cornbrookDropofss.size(), HasId.asIds(cornbrookDropofss));

        Route buryToAlty = tramRouteHelper.getOneRoute(BuryManchesterAltrincham, when);

        assertTrue(cornbrookPickups.contains(buryToAlty));
        assertTrue(cornbrookDropofss.contains(buryToAlty));

        assertTrue(cornbrookPickups.contains(buryToAlty));
        assertTrue(cornbrookDropofss.contains(buryToAlty));

        Route toEccles = tramRouteHelper.getOneRoute(EcclesManchesterAshtonUnderLyne, when);

        assertTrue(cornbrookPickups.contains(toEccles));
        assertTrue(cornbrookDropofss.contains(toEccles));

        Route toTraffordCenter = tramRouteHelper.getOneRoute(CornbrookTheTraffordCentre, when);

        assertTrue(cornbrookPickups.contains(toTraffordCenter));

        Route victoriaToAirport = tramRouteHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, when);

        assertTrue(cornbrookPickups.contains(victoriaToAirport));
        assertTrue(cornbrookDropofss.contains(victoriaToAirport));

    }


}
