package com.tramchester.integration.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.dataimport.rail.repository.RailRouteIds;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.dataimport.rail.reference.TrainOperatingCompanies.NT;
import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailAndTramTransportDataFromFilesTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private TransportData transportData;
    private RailRouteIds routeIdRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(TransportData.class);
        routeIdRepository = componentContainer.get(RailRouteIds.class);
    }

    @Test
    void shouldLoadStations() {
        Set<Station> allStations = transportData.getStations();
        assertFalse(allStations.isEmpty());

        BoundingBox bounds = config.getBounds();

        Set<Station> outOfBounds = allStations.stream().
                filter(station -> station.getGridPosition().isValid()).
                filter(Location::isActive).
                filter(station -> !bounds.contained(station)).collect(Collectors.toSet());

        assertTrue(outOfBounds.isEmpty(), HasId.asIds(outOfBounds));

    }

    @Test
    void shouldHaveCorrectPlatformIds() {
        Station station = transportData.getStationById(Altrincham.getId());
        assertTrue(station.hasPlatforms());

        List<Platform> platforms = new LinkedList<>(station.getPlatforms());

        // should be 2??
        assertEquals(1, platforms.size());

        assertEquals(PlatformId.createId(station,"UNK"), platforms.getFirst().getId());
        assertEquals("UNK", platforms.getFirst().getPlatformNumber());
    }

    @Test
    void shouldHaveSensibleNames() {
        Station result = ManchesterPiccadilly.from(transportData);

        assertEquals("Manchester Piccadilly Rail Station", result.getName());
        assertEquals(KnownLocality.ManchesterCityCentre.getLocalityId(), result.getLocalityId());
    }

    @Test
    void shouldHaveExpectedAgencies() {
        Set<Agency> results = transportData.getAgencies();

        // now filter agencies by geo bounds, so only one operating in GM should appear
        assertEquals(6+1, results.size());

        long count = results.stream().filter(agency -> Agency.IsMetrolink(agency.getId())).count();
        assertEquals(1, count);

        List<IdFor<Agency>> missingTrainOperatingCompanyName = results.stream().
                map(Agency::getId).
                filter(id -> !Agency.IsMetrolink(id)).
                filter(id -> TrainOperatingCompanies.companyNameFor(id).equals(TrainOperatingCompanies.UNKNOWN.getCompanyName())).
                toList();

        assertTrue(missingTrainOperatingCompanyName.isEmpty(), missingTrainOperatingCompanyName.toString());

    }

    @Test
    void shouldHaveRouteAgencyConsistency() {
        transportData.getAgencies().forEach(agency ->
                agency.getRoutes().forEach(route -> assertEquals(agency, route.getAgency(),
                "Agency wrong for " +route.getId() + " got " + route.getAgency().getId() + " but needed " + agency.getId())));
    }

    @Test
    void shouldFindServiceAndTrip() {
        Station startStation = transportData.getStationById(ManchesterPiccadilly.getId());
        Station endStation = transportData.getStationById(Mobberley.getId());

        List<Trip> matchingTrips = transportData.getTrips().stream().
                filter(trip -> trip.callsAt(startStation.getId())).
                filter(trip -> trip.callsAt(endStation.getId())).
                filter(trip -> trip.getStopCalls().getStationSequence(false).getFirst().equals(startStation)).
                filter(trip -> trip.getStopCalls().getLastStop(false).getStation().equals(endStation)).
                sorted(Comparator.comparingLong(a -> a.getStopCalls().totalNumber(true))).
                toList();

        assertFalse(matchingTrips.isEmpty());

        final Trip matchingTrip = matchingTrips.getLast();
        final IdFor<Service> svcId = matchingTrip.getService().getId();
        Service service = transportData.getServiceById(svcId);
        assertNotNull(service);

        Trip trip = transportData.getTripById(matchingTrip.getId());
        assertNotNull(trip);
        assertEquals(service, trip.getService());

        StopCalls stops = trip.getStopCalls();
        final StopCall firstStopCall = stops.getFirstStop(false);
        assertEquals(startStation, firstStopCall.getStation());
        assertEquals(GTFSPickupDropoffType.None, firstStopCall.getDropoffType());
        assertEquals(GTFSPickupDropoffType.Regular, firstStopCall.getPickupType());

        final int totalNumberOfCalls = 9;
        final int expectedPassedStops = 9; // calls at all stations

        assertEquals(totalNumberOfCalls, stops.numberOfCallingPoints(),
                "wrong number of stops " + HasId.asIds(stops.getStationSequence(false)));

        assertEquals(expectedPassedStops, stops.totalNumber(false));

        final StopCall lastStopCall = stops.getLastStop(false);
        assertEquals(endStation, lastStopCall.getStation());
        assertEquals(GTFSPickupDropoffType.Regular, lastStopCall.getDropoffType());
        // data says otherwise...
        //assertEquals(GTFSPickupDropoffType.None, lastStopCall.getPickupType());

        assertEquals(totalNumberOfCalls, stops.getStationSequence(false).size());
        assertEquals(expectedPassedStops, stops.getStationSequence(true).size());

        assertEquals(0, stops.getStationSequence(false).stream().filter(station -> !station.isActive()).count());
        //assertEquals(2, stops.getStationSequence(true).stream().filter(station -> !station.isActive()).count());

        Route route = trip.getRoute();
        assertNotNull(route);

        RouteStation firstRouteStation = transportData.getRouteStation(startStation, route);
        assertNotNull(firstRouteStation);
    }

    @Test
    void shouldHaveRouteStationConsistency() {

        IdFor<Station> stationId = ManchesterAirport.getId();
        Station station = transportData.getStationById(stationId);

        Set<Route> pickupRoutes = station.getPickupRoutes();
        assertFalse(pickupRoutes.isEmpty());
        pickupRoutes.forEach(route -> assertNotNull(transportData.getRouteStation(station, route), route.toString()));

        Set<Route> dropoffRoutes = station.getDropoffRoutes();
        assertFalse(dropoffRoutes.isEmpty());
        dropoffRoutes.forEach(route -> assertNotNull(transportData.getRouteStation(station, route), route.toString()));
    }

    @Test
    void shouldHaveDatasourceInfo() {
        assertTrue(transportData.hasDataSourceInfo());

        assertNotNull(transportData.getDataSourceInfo(DataSourceID.tfgm));
        assertNotNull(transportData.getDataSourceInfo(DataSourceID.openRailData));
    }

    @Test
    void shouldHaveCorrectModes() {
        Set<TransportMode> transportModes = config.getTransportModes();

        assertEquals(3, transportModes.size(), transportModes.toString());
        assertTrue(transportModes.contains(Tram));
        assertTrue(transportModes.contains(Train));
        assertTrue(transportModes.contains(RailReplacementBus));

    }

    @Test
    void shouldHaveCorrectPlatforms() {
        IdFor<Station> stationId = ManchesterPiccadilly.getId();
        Station station = transportData.getStationById(stationId);

        IdFor<Platform> platformId = PlatformId.createId(station, "12");

        Optional<Platform> result = station.getPlatforms().stream().filter(platform -> platform.getId().equals(platformId)).findFirst();

        assertTrue(result.isPresent());
        final Platform platform12 = result.get();
        assertEquals("12", platform12.getPlatformNumber());
        assertEquals("Manchester Piccadilly Rail Station platform 12", platform12.getName());
    }

    @Test
    void shouldMultipleRouteForSameStartEndDependingOnCallingPoints() {
        Set<Route> matchingRoutes = transportData.getTrips().stream().
                filter(trip -> matches(ManchesterPiccadilly.getId(), LondonEuston.getId(), trip)).
                map(Trip::getRoute).
                collect(Collectors.toSet());
        assertNotEquals(1, matchingRoutes.size(), matchingRoutes.toString());
    }

    @Test
    void shouldHaveRouteFromManchesterToStockport() {
        Set<RailRouteIds.RailRouteCallingPointsWithRouteId> allNorthern = routeIdRepository.getCallingPointsFor(NT.getAgencyId());

        StationIdPair piccAndStockport = StationIdPair.of(ManchesterPiccadilly.getId(), Stockport.getId());
        Set<RailRouteIds.RailRouteCallingPointsWithRouteId> northernPiccToStockport = allNorthern.stream().
                filter(routeId -> routeId.getBeginEnd().equals(piccAndStockport)).
                collect(Collectors.toSet());

        assertFalse(northernPiccToStockport.isEmpty());

        Set<Route> matchingRoutes = northernPiccToStockport.stream().
                map(routeId -> transportData.getRouteById(routeId.getRouteId())).collect(Collectors.toSet());

        assertFalse(matchingRoutes.isEmpty());

        Set<Service> servicesForRoutes = matchingRoutes.stream().
                flatMap(route -> route.getServices().stream()).collect(Collectors.toSet());

        assertFalse(servicesForRoutes.isEmpty(), "no services for " + matchingRoutes);

        TramDate when = TestEnv.testDay();

        Set<Service> runningAfterDate = servicesForRoutes.stream().
                filter(service -> service.getCalendar().getDateRange().getEndDate().isAfter(when)).collect(Collectors.toSet());

        // Needs up to train data to pass
        assertFalse(runningAfterDate.isEmpty(), "no running on " + when + " services " + servicesForRoutes);

        TramTime time = TramTime.of(23,20);
        TimeRange timeRange = TimeRangePartial.of(time, time.plusMinutes(60));

        Set<Trip> matchingTrips = transportData.getTrips().stream().
                filter(trip -> runningAfterDate.contains(trip.getService())).
                filter(trip -> timeRange.contains(trip.departTime())).
                collect(Collectors.toSet());

        assertFalse(matchingTrips.isEmpty(), "No trip at required time " + HasId.asIds(runningAfterDate));
    }

    @Test
    void shouldHaveRoutesForManchesterAirport() {
        IdFor<Station> startId = ManchesterPiccadilly.getId();
        IdFor<Station> endId = ManchesterAirport.getId();

        // all routes for between piccadilly and the airport
        Set<RailRoute> routes = transportData.getRoutes().stream().
                filter(route -> route instanceof MutableRailRoute).
                map(route -> (RailRoute)route).
                filter(route -> route.getBegin().getId().equals(startId)).
                filter(route -> route.getEnd().getId().equals(endId)).
                collect(Collectors.toSet());

        assertEquals(2, routes.size(), HasId.asIds(routes));

        Set<String> agencies = routes.stream().map(route -> route.getAgency().getName()).collect(Collectors.toSet());

        assertEquals(2, agencies.size(), agencies.toString());
        assertTrue(agencies.contains(TrainOperatingCompanies.TP.getCompanyName()));
        assertTrue(agencies.contains(NT.getCompanyName()));
    }

    @Test
    void shouldHaveSaneServiceStartAndFinishTimes() {
        Set<Service> allServices = transportData.getServices();

        Set<Service> badTimings = allServices.stream().
                filter(svc -> svc.getFinishTime().isBefore(svc.getStartTime())).
                collect(Collectors.toSet());

        String diagnostics = badTimings.stream().
                map(service -> service.getId() + " begin: " + service.getStartTime() + " end: " + service.getFinishTime() + " ").
                collect(Collectors.joining());

        assertTrue(badTimings.isEmpty(), diagnostics);
    }

    @Test
    void shouldNotHaveRailReplacementBusAsTransportModeForRoutesShouldBeTrain() {
       transportData.getRoutes().forEach(route ->
               assertNotEquals(TransportMode.RailReplacementBus, route.getTransportMode(),
                       "route should be rail " +route));
    }

    @Test
    void shouldHaveSomeRailRoutesThatContainTripsWithRailReplacementBusTransportMode() {
        assertTrue(transportData.getRoutes().stream().
                filter(route -> route.getTransportMode()==Train).
                anyMatch(route -> route.getTrips().stream().anyMatch(trip -> trip.getTransportMode()==RailReplacementBus)));
    }

    private boolean matches(IdFor<Station> firstId, IdFor<Station> secondId, Trip trip) {
        StopCall firstCall = trip.getStopCalls().getFirstStop(false);
        if (!firstCall.getStationId().equals(firstId)) {
            return false;
        }
        StopCall finalCall = trip.getStopCalls().getLastStop(false);
        return secondId.equals(finalCall.getStationId());
    }



}
