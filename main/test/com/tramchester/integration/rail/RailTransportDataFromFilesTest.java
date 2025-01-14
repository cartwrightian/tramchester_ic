package com.tramchester.integration.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.*;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.RailReplacementBus;
import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
public class RailTransportDataFromFilesTest {
    private static ComponentContainer componentContainer;
    private static IntegrationRailTestConfig config;
    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationRailTestConfig(IntegrationRailTestConfig.Scope.National);
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
    }

    @Test
    void shouldHaveNaptanEnabled() {
        NaptanRepository naptanRepository = componentContainer.get(NaptanRepository.class);
        assertTrue(naptanRepository.isEnabled());
    }

    @Test
    void shouldLoadStations() {
        Set<Station> allStations = transportData.getStations();
        assertFalse(allStations.isEmpty());

        BoundingBox bounds = config.getBounds();

        allStations.forEach(station -> assertTrue(bounds.contained(station), station + "out of bounds"));
    }

    @Test
    void shouldHaveCorrectPlatformIds() {
        Station station = transportData.getStationById(Altrincham.getId());
        assertTrue(station.hasPlatforms());

        List<Platform> platforms = new LinkedList<>(station.getPlatforms());

        // should be 2??
        assertEquals(1, platforms.size());

        assertEquals(PlatformId.createId(station,"UNK"), platforms.get(0).getId());
        assertEquals("UNK", platforms.get(0).getPlatformNumber());
    }

    @Test
    void shouldGetSpecificStation() {
        Station result = transportData.getStationById(Derby.getId());

        assertEquals("Derby Rail Station", result.getName());
        assertIdEquals("E0054915", result.getLocalityId());

        final GridPosition expectedGrid = new GridPosition(436182, 335593);
        assertEquals(expectedGrid, result.getGridPosition());

        final LatLong expectedLatLong = CoordinateTransforms.getLatLong(expectedGrid);
        //assertEquals(expectedLatLong, result.getLatLong());
        assertEquals(expectedLatLong.getLat(), result.getLatLong().getLat(), 0.0001);
        assertEquals(expectedLatLong.getLon(), result.getLatLong().getLon(), 0.0001);

        assertEquals(DataSourceID.openRailData, result.getDataSourceID());
        assertTrue(result.isMarkedInterchange());
    }

    @Test
    void shouldHaveSensibleNamesManPicc() {
        Station result = transportData.getStationById(ManchesterPiccadilly.getId());

        assertEquals("Manchester Piccadilly Rail Station", result.getName());
        assertIdEquals("E0057786", result.getLocalityId());
    }

    /*
       crossing checking with @See NaptanRepositoryTest
    */
    @Test
    void shouldHaveMacclesfield() {
        Station result = transportData.getStationById(Macclesfield.getId());

        assertEquals("Macclesfield Rail Station", result.getName());
        assertEquals(KnownLocality.Macclesfield.getLocalityId(), result.getLocalityId());
    }

    @Test
    void shouldHaveExpectedCallingPointsForTripOnARoute() {

        Station piccadilly = ManchesterPiccadilly.from(transportData);
        Station euston = LondonEuston.from(transportData);

        String longName = "VT service from Manchester Piccadilly Rail Station to London Euston Rail Station via Stockport " +
                "Rail Station, Macclesfield Rail Station, Stoke-on-Trent Rail Station, Milton Keynes Central Rail Station";

        List<Station> expectedCallingPoints = Arrays.asList(piccadilly,
                Stockport.from(transportData),
                Macclesfield.from(transportData),
                StokeOnTrent.from(transportData),
                MiltonKeynesCentral.from(transportData),
                euston);

        Set<Route> routes = piccadilly.getPickupRoutes().stream().
                filter(route -> longName.equals(route.getName())).
                collect(Collectors.toSet());

        Set<Trip> wrongCallingPoints = routes.stream().
                flatMap(route -> route.getTrips().stream()).
                filter(trip -> !trip.getStopCalls().getStationSequence(false).equals(expectedCallingPoints)).
                collect(Collectors.toSet());

        assertTrue(wrongCallingPoints.isEmpty(), wrongCallingPoints.toString());

    }

    @Test
    void shouldNotLoadTFGMMetStations() {
        final IdFor<Station> unwantedStation = Station.createId("ALTRMET");

        // TODO Split active vs inactive stations? Problem is don't know modes until after the load
        // likely need to split station load into temp collection first and post filter
        final boolean result = transportData.hasStationId(unwantedStation);
        assertFalse(result);

        Set<RouteStation> routeStations = transportData.getRouteStations();
        IdSet<Station> unwantedRouteStations = routeStations.stream().
                map(routeStation -> routeStation.getStation().getId()).
                filter(unwantedStation::equals).collect(IdSet.idCollector());
        assertTrue(unwantedRouteStations.isEmpty());
    }

    @Test
    void shouldHaveExpectedAgencies() {
        Set<Agency> results = transportData.getAgencies();

        assertEquals(30, results.size());

        List<IdFor<Agency>> missingTrainOperatingCompanyName = results.stream().
                map(Agency::getId).
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

    @Disabled("now filtering out stations in Ireland etc")
    @Test
    void shouldGetSpecificStationWithoutPosition() {
        // A    KILLARNEY   (CIE              0KILARNYKLL   KLL00000E00000 5

        Station result = transportData.getStationById(Station.createId("KILARNY"));

        assertEquals("KILLARNEY   (CIE", result.getName());
        assertFalse(result.getGridPosition().isValid());
        assertFalse(result.getLatLong().isValid());
        assertEquals(DataSourceID.openRailData, result.getDataSourceID());
        assertFalse(result.isMarkedInterchange());
    }

    @Test
    void shouldFindServiceAndTrip() {
        Station startStation = transportData.getStationById(Derby.getId());
        Station endStation = transportData.getStationById(LondonStPancras.getId());

        final int numberOfCalingPoints = 7;
        final int numberPassedStops = 21;

        List<Trip> matchingTripsForCallingPoints = transportData.getTrips().stream().
                filter(trip -> trip.callsAt(startStation.getId())).
                filter(trip -> trip.callsAt(endStation.getId())).
                filter(trip -> trip.getStopCalls().numberOfCallingPoints()==numberOfCalingPoints).
                filter(trip -> trip.getStopCalls().getStationSequence(false).get(0).equals(startStation)).
                filter(trip -> trip.getStopCalls().getLastStop().getStation().equals(endStation)).
                toList();

        assertFalse(matchingTripsForCallingPoints.isEmpty());

        List<Trip> matchingTrips = matchingTripsForCallingPoints.stream().
                filter(trip -> trip.getStopCalls().totalNumber()==numberPassedStops).toList();

        assertFalse(matchingTrips.isEmpty());

        final Trip matchingTrip = matchingTrips.get(0);
        final IdFor<Service> svcId = matchingTrip.getService().getId();
        Service service = transportData.getServiceById(svcId);
        assertNotNull(service);

        Trip trip = transportData.getTripById(matchingTrip.getId());
        assertNotNull(trip);
        assertEquals(service, trip.getService());

        StopCalls stops = trip.getStopCalls();
        final StopCall firstStopCall = stops.getFirstStop();
        assertEquals(startStation, firstStopCall.getStation());
        assertEquals(GTFSPickupDropoffType.None, firstStopCall.getDropoffType());
        assertEquals(GTFSPickupDropoffType.Regular, firstStopCall.getPickupType());

        //final int expectedCalls = 7;

        assertEquals(numberOfCalingPoints, stops.numberOfCallingPoints(),
                "wrong number of stops " + HasId.asIds(stops.getStationSequence(false)));


        final StopCall lastStopCall = stops.getLastStop();
        assertEquals(endStation, lastStopCall.getStation());
        assertEquals(GTFSPickupDropoffType.Regular, lastStopCall.getDropoffType());
        assertEquals(GTFSPickupDropoffType.None, lastStopCall.getPickupType());

        assertEquals(numberOfCalingPoints, stops.getStationSequence(false).size());
        assertEquals(numberPassedStops, stops.getStationSequence(true).size());

        // 2 if including passed stop, 0 otherwise
        assertEquals(0, stops.getStationSequence(false).stream().filter(station -> !station.isActive()).count());
        assertEquals(2, stops.getStationSequence(true).stream().filter(station -> !station.isActive()).count());

        Route route = trip.getRoute();
        assertNotNull(route);

        RouteStation firstRouteStation = transportData.getRouteStation(startStation, route);
        assertNotNull(firstRouteStation);
    }

    @Test
    void shouldHaveRouteStationConsistency() {

        IdFor<Station> stationId = Wimbledon.getId();
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

        DataSourceInfo info = transportData.getDataSourceInfo(DataSourceID.openRailData); //dataSourceInfos.get(0);
        assertEquals(DataSourceID.openRailData, info.getID());
        assertEquals(config.getRailConfig().getModes(), info.getModes());

    }

    @Test
    void shouldHaveCorrectPlatforms() {
        IdFor<Station> stationId = LondonWaterloo.getId();
        Station station = transportData.getStationById(stationId);

//        IdFor<Platform> platformId = Platform.createId(station,"WATRLMN:12");
        IdFor<Platform> platformId = PlatformId.createId(station,"12");

        Optional<Platform> result = station.getPlatforms().stream().filter(platform -> platform.getId().equals(platformId)).findFirst();

        assertTrue(result.isPresent());
        final Platform platform12 = result.get();
        assertEquals("12", platform12.getPlatformNumber());
        assertEquals("London Waterloo Rail Station platform 12", platform12.getName());
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
    void shouldHaveRouteFromManchesterToLondon() {
        IdFor<Station> manchesterPicc = ManchesterPiccadilly.getId();
        IdFor<Station> londonEuston = LondonEuston.getId();

        Set<Route> matchingRoutes = transportData.getTrips().stream().
                filter(trip -> matches(manchesterPicc, londonEuston, trip)).
                map(Trip::getRoute).
                collect(Collectors.toSet());

        assertFalse(matchingRoutes.isEmpty());
        IdSet<Route> routeIds = matchingRoutes.stream().collect(IdSet.collector());

        IdFor<Agency> agency = TrainOperatingCompanies.VT.getAgencyId();

        RailRouteId expected = new RailRouteId(manchesterPicc, londonEuston, agency, 1);
        assertTrue(routeIds.contains(expected), "did find route " + routeIds);

        Set<Service> matchingServices = matchingRoutes.stream().
                flatMap(route -> route.getServices().stream()).collect(Collectors.toSet());

        assertFalse(matchingServices.isEmpty(), "no services for " + matchingRoutes);

        TramDate when = TestEnv.testDay();

        Set<Service> runningServices = matchingServices.stream().
                filter(service -> service.getCalendar().operatesOn(when)).collect(Collectors.toSet());

        assertFalse(runningServices.isEmpty(), "none running from " + runningServices);

        TramTime time = TramTime.of(8,5);

        Set<Trip> am8Trips = transportData.getTrips().stream().
                filter(trip -> runningServices.contains(trip.getService())).
                filter(trip -> trip.departTime().isBefore(time) && trip.arrivalTime().isAfter(time)).
                collect(Collectors.toSet());

        assertFalse(am8Trips.isEmpty(), "No trip at required time " + runningServices);
    }

    @Test
    void shouldHaveRoutesForAgencyStartAndEnd() {
        IdFor<Agency> agencyId = Agency.createId("VT");
        Optional<Agency> foundAgency = transportData.getAgencies().stream().
                filter(agency -> agency.getId().equals(agencyId)).findFirst();

        IdFor<Station> startId = ManchesterPiccadilly.getId();
        IdFor<Station> endId = LondonEuston.getId();

        assertTrue(foundAgency.isPresent());
        Agency agency = foundAgency.get();

        // all routes for VT between start and end
        Set<MutableRailRoute> routes = transportData.getRoutes().stream().
                filter(route -> route instanceof MutableRailRoute).
                filter(route -> route.getAgency().equals(agency)).
                map(route -> (MutableRailRoute)route).
                filter(route -> route.getBegin().getId().equals(startId)).
                filter(route -> route.getEnd().getId().equals(endId)).
                collect(Collectors.toSet());

        // get unique sets of calling points
        Set<List<Station>> uniqueCallingPoints = routes.stream().
                map(MutableRailRoute::getCallingPoints).collect(Collectors.toSet());

        assertEquals(routes.size(), uniqueCallingPoints.size());

        assertEquals(9, routes.size(), routes.toString());
    }

    @Test
    void shouldHaveAllRoutesWithOperatingDays() {
        Set<Route> noDays = transportData.getRoutes().stream().
                filter(route -> route.getOperatingDays().isEmpty()).
                collect(Collectors.toSet());
        assertTrue(noDays.isEmpty());
    }

    // Likely this will break with new data
    @Disabled("Service not in latest data")
    @Test
    void reproIssueWithCrossingMidnightThatOnlyOccursWhenWholeFileLoaded() {
        Service service = transportData.getServiceById(Service.createId("N51867:20220730:20220730"));

        TramTime startTime = service.getStartTime();
        TramTime finishTime = service.getFinishTime();

        assertFalse(startTime.isNextDay(), startTime.toString());
        assertTrue(finishTime.isNextDay(), finishTime.toString());

        assertTrue(finishTime.isAfter(startTime));
        assertFalse(finishTime.isBefore(startTime));
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

    @Test
    void shouldHaveLongestSingleLeg() {
        // Currently Euston to Sterling

        Optional<StopCalls.StopLeg> findLongest = transportData.getTrips().stream().
                filter(trip -> trip.getTransportMode().equals(Train)).
                flatMap(trip -> getLongDurationStopLeg(trip).stream()).
                max(Comparator.comparing(StopCalls.StopLeg::getCost));

        assertTrue(findLongest.isPresent());

        StopCalls.StopLeg longest = findLongest.get();

        // TODO This seems suspect, unless train just stops somewhere for 7 hours???

        IdFor<Station> carrbridgeScotland = Station.createId("CARRBDG");
        IdFor<Station> dundee = Station.createId("DUNDETB");

        assertEquals(carrbridgeScotland, longest.getFirstStation().getId());
        assertEquals(dundee, longest.getSecondStation().getId());
        assertEquals(Duration.ofHours(9).plusMinutes(28), longest.getCost());
    }

    private Set<StopCalls.StopLeg> getLongDurationStopLeg(Trip trip) {
        Optional<StopCalls.StopLeg> found = trip.getStopCalls().getLegs(false).stream().
                max(Comparator.comparingLong(a -> a.getCost().toSeconds()));
        return found.map(Collections::singleton).orElse(Collections.emptySet());
    }


    @Disabled("trip not in latest data")
    @Test
    void reproIssueWithStopLegsOnSpecificTrip() {
        IdFor<Trip> tripIdFor = Trip.createId("trip:C43611:20220703:20220703OVERLAY");

        Trip trip = transportData.getTripById(tripIdFor);

        StopCalls stopCalls = trip.getStopCalls();

        assertEquals(12, stopCalls.numberOfCallingPoints());
        assertEquals(11, stopCalls.getLegs(false).size());
    }

    @Test
    void shouldHaveTransportModeSetForARequestStop() {

        // this station is only listed as a request stop in the timetable
        Station station = transportData.getStationById(Station.createId("HOPTONH"));

        Set<TransportMode> modes = station.getTransportModes();
        assertFalse(modes.isEmpty(), station.toString());
        assertTrue(modes.contains(Train), station.toString());

        assertTrue(station.hasPickup());
        assertTrue(station.hasDropoff());
    }

    @Test
    void shouldHaveStationsWithNoStops() {
        Station station = transportData.getStationById(Station.createId("LCHTNJ"));

        Set<TransportMode> modes = station.getTransportModes();
        assertTrue(modes.isEmpty(), station.toString());

        assertFalse(station.hasDropoff());
        assertFalse(station.hasPickup());

    }


    private boolean matches(IdFor<Station> firstId, IdFor<Station> secondId, Trip trip) {
        StopCall firstCall = trip.getStopCalls().getFirstStop();
        if (!firstCall.getStationId().equals(firstId)) {
            return false;
        }
        StopCall finalCall = trip.getStopCalls().getLastStop();
        return secondId.equals(finalCall.getStationId());
    }



}
