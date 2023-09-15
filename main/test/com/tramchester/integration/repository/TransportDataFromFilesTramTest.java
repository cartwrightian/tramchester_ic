package com.tramchester.integration.repository;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.dataimport.loader.PopulateTransportDataFromSources;
import com.tramchester.dataimport.loader.TransportDataReader;
import com.tramchester.dataimport.loader.TransportDataReaderFactory;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.ServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.dates.TramDateSet;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DataExpiryCategory;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tramchester.domain.reference.CentralZoneStation.StPetersSquare;
import static com.tramchester.domain.reference.CentralZoneStation.TraffordBar;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

// TODO Split out by i/f roles, this has gotten too big
// Begun, started to create tests by repository instead

@ExtendWith(ConfigParameterResolver.class)
@DualTest
@DataUpdateTest
public class TransportDataFromFilesTramTest {

    public static final int NUM_TFGM_TRAM_STATIONS = 99; // summer closures of eccles line
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private TransportData transportData;
    private Collection<Service> allServices;
    private TramRouteHelper routeHelper;
    private ClosedStationsRepository closedStationRepository;
    private TramDate when;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        config = tramchesterConfig;
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
        allServices = transportData.getServices(EnumSet.of(Tram));
        routeHelper = new TramRouteHelper(transportData);
        closedStationRepository = componentContainer.get(ClosedStationsRepository.class);

        when = TestEnv.testDay(); // filter by date, otherwise get variations due to upcoming routes etc

    }

    @Test
    void shouldHaveExpectedNumbersForTram() {
        assertEquals(1, transportData.getAgencies().stream().filter(agency -> agency.getTransportModes().contains(Tram)).count());
        assertEquals(NUM_TFGM_TRAM_STATIONS, transportData.getStations(EnumSet.of(Tram)).size());

        int expected = 199;
        assertEquals(expected, transportData.getPlatforms(EnumSet.of(Tram)).size());
    }

    @Test
    void shouldHaveExpectedNumRoutes() {
        Set<String> uniqueNames = transportData.getRoutesRunningOn(when).stream().
                filter(route -> route.getTransportMode()==Tram).
                map(Route::getName).collect(Collectors.toSet());

        assertEquals(KnownTramRoute.numberOn(when), uniqueNames.size(), uniqueNames.toString());

    }

    @Test
    void shouldGetDateRangeAndVersion() {
        DateRangeAndVersion result = transportData.getDateRangeAndVersionFor(DataSourceID.tfgm);
        LocalDate localDate = when.toLocalDate();
        assertTrue(result.validUntil().isAfter(localDate));
        assertTrue(result.validFrom().isBefore(localDate) || result.validFrom().equals(localDate));
        assertFalse(result.getVersion().isEmpty());
    }

    @Test
    void shouldGetAgenciesWithNames() {
        List<Agency> agencies = transportData.getAgencies().stream().
                filter(agency -> agency.getTransportModes().contains(Tram)).
                collect(Collectors.toList());

        //List<Agency> agencies = new ArrayList<>(agencySet);
        assertEquals(1, agencies.size()); // just MET for trams
        assertIdEquals("7778482", agencies.get(0).getId());
        assertEquals("Metrolink", agencies.get(0).getName());
    }

    @Test
    void shouldHaveRouteStationsThatOccurDueToDepot() {
        Set<RouteStation> routeStations = transportData.getRouteStations();

        Set<RouteStation> traffordBar = routeStations.stream().
                filter(routeStation -> routeStation.getStationId().equals(TraffordBar.getId())).collect(Collectors.toSet());

        IdSet<Route> traffordBarRoutes = traffordBar.stream().
                map(RouteStation::getRoute).map(Route::getId).collect(IdSet.idCollector());

        // cannot check for specific size as the way routes handled in tfgm gtfs feed can lead to duplicates
        //assertEquals(10, traffordBarRoutes.size());

        // contains -> containsAll

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(AltrinchamPiccadilly)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(PiccadillyAltrincham)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(EastDidisburyManchesterShawandCromptonRochdale)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(RochdaleShawandCromptonManchesterEastDidisbury)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(VictoriaWythenshaweManchesterAirport)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(ManchesterAirportWythenshaweVictoria)));

        // todo PiccGardens2022
        //assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(EcclesManchesterAshtonUnderLyne)));
        //assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(AshtonUnderLyneManchesterEccles)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(AltrinchamManchesterBury)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(BuryManchesterAltrincham)));

    }

    @Test
    void shouldHaveExpectedRoutesNonDepot() {
        Set<RouteStation> all = transportData.getRouteStations();

        Set<RouteStation> routeStationSet = all.stream().
                filter(routeStation -> routeStation.getRoute().isAvailableOn(when)).
                filter(routeStation -> routeStation.getStationId().equals(OldTrafford.getId())).
                collect(Collectors.toSet());

        Set<Route> callingRoutes = routeStationSet.stream().map(RouteStation::getRoute).collect(Collectors.toSet());

        Set<String> uniqueRouteNames = callingRoutes.stream().map(Route::getName).collect(Collectors.toSet());

        assertEquals(2, uniqueRouteNames.size(), uniqueRouteNames.toString());
    }

    @Test
    void shouldGetRouteStationsForStationOnOneRoute() {
        Set<RouteStation> routeStations = transportData.getRouteStationsFor(ManAirport.getId());

        Set<Pair<IdFor<Station>, String>> routeStationPairs = routeStations.stream().
                map(routeStation -> Pair.of(routeStation.getStationId(), routeStation.getRoute().getName())).
                collect(Collectors.toSet());

        assertEquals(1, routeStationPairs.size(), routeStationPairs.toString());

        Set<String> routeNames =
                routeStations.stream().
                        map(RouteStation::getRoute).
                        map(Route::getName).collect(Collectors.toSet());

        assertTrue(routeNames.contains(VictoriaWythenshaweManchesterAirport.longName()), routeNames.toString());
        assertTrue(routeNames.contains(ManchesterAirportWythenshaweVictoria.longName()), routeNames.toString());

    }

    @Test
    void shouldHaveRoutesWithStationsAndCallingPoints() {
        Set<Route> allTramRoutes = transportData.getRoutes(EnumSet.of(Tram));

        Set<Station> allStations = transportData.getStations(EnumSet.of(Tram));

        Set<Route> noPickups = allTramRoutes.stream().
                filter(route -> allStations.stream().noneMatch(station -> station.servesRoutePickup(route))).
                collect(Collectors.toSet());

        assertTrue(noPickups.isEmpty(), noPickups.toString());

        Set<Route> noDropOffs = allTramRoutes.stream().
                filter(route -> allStations.stream().noneMatch(station -> station.servesRouteDropOff(route))).
                collect(Collectors.toSet());

        assertTrue(noDropOffs.isEmpty(), noDropOffs.toString());
    }

    @Disabled("No longer a route")
    @Test
    void shouldHaveExpectedStationsForGreenFromAlty() {
        Route green = routeHelper.getOneRoute(AltrinchamManchesterBury, when);

        Set<Station> allStations = transportData.getStations(EnumSet.of(Tram));

        IdSet<Station> dropOffs = allStations.stream().filter(station -> station.servesRouteDropOff(green)).collect(IdSet.collector());

        assertEquals(24, dropOffs.size(), dropOffs.toString());
        assertFalse(dropOffs.contains(Altrincham.getId()));
        assertTrue(dropOffs.contains(Bury.getId()));
        assertTrue(dropOffs.contains(Cornbrook.getId()));
        assertTrue(dropOffs.contains(Shudehill.getId()));

        IdSet<Station> pickUps = allStations.stream().filter(station -> station.servesRoutePickup(green)).collect(IdSet.collector());

        assertEquals(24, pickUps.size(), pickUps.toString());
        assertTrue(pickUps.contains(Altrincham.getId()));
        assertFalse(pickUps.contains(Bury.getId()));
        assertTrue(pickUps.contains(Cornbrook.getId()));
        assertTrue(pickUps.contains(Shudehill.getId()));

    }

    @Test
    void shouldHaveExpectedStationsForGreenFromBury() {
        Route green = routeHelper.getOneRoute(BuryManchesterAltrincham, when);

        Set<Station> allStations = transportData.getStations(EnumSet.of(Tram));

        IdSet<Station> dropOffs = allStations.stream().filter(station -> station.servesRouteDropOff(green)).collect(IdSet.collector());

        // 24 -> 25
        assertEquals(25, dropOffs.size(), dropOffs.toString());
        // in new data Bury is dropoff since no direction to routes....
        //assertFalse(dropOffs.contains(Bury.getId()));
        assertTrue(dropOffs.contains(Altrincham.getId()));
        assertTrue(dropOffs.contains(Cornbrook.getId()));
        assertTrue(dropOffs.contains(Shudehill.getId()));

        IdSet<Station> pickUps = allStations.stream().filter(station -> station.servesRoutePickup(green)).collect(IdSet.collector());

        assertEquals(25, pickUps.size(), pickUps.toString());
        assertTrue(pickUps.contains(Bury.getId()));
        //assertFalse(pickUps.contains(Altrincham.getId()));
        assertTrue(pickUps.contains(Cornbrook.getId()));
        assertTrue(pickUps.contains(Shudehill.getId()));

    }

    @Test
    void shouldGetRouteStationsForStation() {
        Set<RouteStation> routeStations = transportData.getRouteStationsFor(Shudehill.getId());

        Set<Pair<IdFor<Station>, String>> routeStationPairs = routeStations.stream().
                filter(routeStation -> routeStation.getRoute().isAvailableOn(when)).
                map(routeStation -> Pair.of(routeStation.getStationId(), routeStation.getRoute().getName())).
                collect(Collectors.toSet());

        // 8 -> 4
        assertEquals(4, routeStationPairs.size(), routeStations.toString());

        Set<String> routeNames =
                routeStations.stream().
                        map(RouteStation::getRoute).
                        map(Route::getName).collect(Collectors.toSet());

        assertTrue(routeNames.contains(VictoriaWythenshaweManchesterAirport.longName()), routeNames.toString());
        assertTrue(routeNames.contains(ManchesterAirportWythenshaweVictoria.longName()), routeNames.toString());

        assertTrue(routeNames.contains(AltrinchamManchesterBury.longName()), routeNames.toString());
        assertTrue(routeNames.contains(BuryManchesterAltrincham.longName()), routeNames.toString());

        assertTrue(routeNames.contains(BuryPiccadilly.longName()), routeNames.toString());
        assertTrue(routeNames.contains(PiccadillyBury.longName()), routeNames.toString());

    }

    @Test
    void shouldGetServicesByDate() {
        TramDate nextSaturday = TestEnv.nextSaturday();
        Set<Service> results = transportData.getServicesOnDate(nextSaturday);

        assertFalse(results.isEmpty(), "no services next saturday");
        long onCorrectDate = results.stream().
                filter(svc -> svc.getCalendar().operatesOn(nextSaturday)).count();

        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

        TramDate noTrams = nextSaturday.plusWeeks(11*52);
        results = transportData.getServicesOnDate(noTrams);
        assertTrue(results.isEmpty(), "not empty, got " + results);
    }

    @Test
    void shouldHaveSaneServiceStartAndFinishTimes() {
        Set<Service> badTimings = allServices.stream().filter(svc -> svc.getStartTime().isAfter(svc.getFinishTime())).
                collect(Collectors.toSet());
        assertTrue(badTimings.isEmpty());
    }

    @Test
    void shouldHaveSundayServicesFromCornbrook() {
        TramDate nextSunday = TestEnv.nextSunday();

        Set<Service> sundayServices = transportData.getServicesOnDate(nextSunday);

        Set<Trip> cornbrookTrips = transportData.getTrips().stream().
                filter(trip -> trip.callsAt(Cornbrook)).collect(Collectors.toSet());

        Set<Trip> sundayTrips = cornbrookTrips.stream().
                filter(trip -> sundayServices.contains(trip.getService())).collect(Collectors.toSet());

        assertFalse(sundayTrips.isEmpty());
    }

    @DataExpiryCategory
    @Test
    void shouldHaveServiceEndDatesBeyondNextNDays() {

        TramDate startDate = TramDate.from(TestEnv.LocalNow());
        TramDate endDate = startDate.plusDays(DAYS_AHEAD);

        DateRange dateRange = DateRange.of(startDate, endDate);

        Set<Service> services = transportData.getServices();
        Set<Service> expiringServices = services.stream().
                filter(service -> !service.getCalendar().getDateRange().overlapsWith(dateRange)).
                collect(Collectors.toSet());

        assertNotEquals(services, expiringServices, "all services are expiring");
    }

    @Disabled("Solved by removing reboarding filter which does not impact depth first performance")
    @Test
    void shouldCheckTripsFinishingAtNonInterchangeStationsOrEndOfLines() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
        Set<Trip> allTrips = transportData.getTrips();

        Set<String> endTripNotInterchange = allTrips.stream().
                map(trip -> trip.getStopCalls().getLastStop()).
                map(StopCall::getStation).
                filter(station -> !interchangeRepository.isInterchange(station)).
                filter(station -> !TramStations.isEndOfLine(station)).
                map(Station::getName).
                collect(Collectors.toSet());

        assertTrue(endTripNotInterchange.isEmpty(), "End trip not interchange: " + endTripNotInterchange);
    }

    @Test
    void shouldHandleStopCallsThatCrossMidnight() {
        Set<Route> routes = transportData.getRoutes(EnumSet.of(Tram));

        for (Route route : routes) {
            List<StopCalls.StopLeg> over = route.getTrips().stream().flatMap(trip -> trip.getStopCalls().getLegs(false).stream()).
                    filter(stopLeg -> stopLeg.getCost().compareTo(Duration.ofMinutes(12*24)) > 0).
                    collect(Collectors.toList());
            assertTrue(over.isEmpty(), over.toString());
        }

    }

    @DataExpiryCategory
    @Test
    void shouldHaveTramServicesAvailableNDaysAhead() {

        Set<TramDate> noServices = TestEnv.getUpcomingDates().
                filter(date -> !date.isChristmasPeriod()).
                filter(date -> transportData.getServicesOnDate(date).isEmpty()).
                collect(Collectors.toSet());

        assertTrue(noServices.isEmpty(), "no services on " + noServices);

    }

    @DataExpiryCategory
    @Test
    void shouldHaveTripsOnDateForEachStation() {

        Set<Pair<TramDate, IdFor<Station>>> missing = TestEnv.getUpcomingDates().
                filter(date -> !date.isChristmasPeriod()).
                flatMap(date -> transportData.getStations(EnumSet.of(Tram)).stream().map(station -> Pair.of(date, station))).
                filter(pair -> !closedStationRepository.isClosed(pair.getRight(), pair.getLeft())).
                filter(pair -> transportData.getTripsFor(pair.getRight(), pair.getLeft()).isEmpty()).
                map(pair -> Pair.of(pair.getLeft(), pair.getRight().getId())).
                collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), "Got missing trips for " + missing);

    }

    @Test
    void shouldHaveServicesThatIncludeDateInRange() {
        //TramDate date = TramDate.of(2022,11,21);

        Set<Service> allServices = transportData.getServices();

        Set<Service> includeDate = allServices.stream().filter(service -> service.getCalendar().getDateRange().contains(when)).collect(Collectors.toSet());

        assertFalse(includeDate.isEmpty());

        Set<Service> onActualDate = includeDate.stream().filter(service -> service.getCalendar().operatesOn(when)).collect(Collectors.toSet());

        assertFalse(onActualDate.isEmpty());
    }

    @DataExpiryCategory
    @Test
    void shouldHaveServicesRunningAtReasonableTimesNDaysAhead() {

        int latestHour = 23;
        int earlistHour = 7;

        List<TramTime> times = IntStream.range(earlistHour, latestHour).boxed().
                map(hour -> TramTime.of(hour, 0)).
                sorted().
                collect(Collectors.toList());

        int maxwait = 25;

        Map<Pair<TramDate, TramTime>, IdSet<Station>> missing = new HashMap<>();

        TestEnv.getUpcomingDates().filter(date -> !date.isChristmasPeriod()).forEach(date -> {
            transportData.getStations(EnumSet.of(Tram)).stream().
                    filter(station -> !closedStationRepository.isClosed(station, date)).
                    forEach(station -> {
                        Set<Trip> trips = transportData.getTripsFor(station, date);
                        for (TramTime time : times) {
                            TimeRange range = TimeRange.of(time.minusMinutes(maxwait), time.plusMinutes(maxwait));
                            boolean calls = trips.stream().flatMap(trip -> trip.getStopCalls().stream()).
                                    filter(stopCall -> stopCall.getStation().equals(station)).
                                    anyMatch(stopCall -> range.contains(stopCall.getArrivalTime()));
                            if (!calls) {
                                Pair<TramDate, TramTime> key = Pair.of(date, time);
                                if (!missing.containsKey(key)) {
                                    missing.put(key, new IdSet<>());
                                }
                                missing.get(key).add(station.getId());
                            }

                        }
                    });
        });

        assertTrue(missing.isEmpty(), missing.toString());

    }


    @Test
    void shouldHaveAtLeastOnePlatformForEveryStation() {
        Set<Station> stations = transportData.getStations(EnumSet.of(Tram));
        Set<Station> noPlatforms = stations.stream().filter(station -> station.getPlatforms().isEmpty()).collect(Collectors.toSet());
        assertEquals(Collections.emptySet(),noPlatforms);
    }

    @Test
    void shouldGetStation() {
        assertTrue(transportData.hasStationId(Altrincham.getId()));
        Station station = transportData.getStationById(Altrincham.getId());
        assertEquals("Altrincham", station.getName());

        assertTrue(station.hasPlatforms());

        assertEquals(1, station.getPlatforms().size());
        final Optional<Platform> maybePlatformOne = station.getPlatforms().stream().findFirst();
        assertTrue(maybePlatformOne.isPresent());

        Platform platformOne = maybePlatformOne.get();
        final IdFor<Platform> expectedId = Altrincham.createIdFor("1");

        assertEquals(expectedId, platformOne.getId());
        assertEquals( "1", platformOne.getPlatformNumber());
        assertEquals( "Altrincham platform 1", platformOne.getName());

        // Needs naptan enabled to work
        //assertEquals(station.getAreaId(), platformOne.getAreaId());

        assertEquals(station.getDataSourceID(), platformOne.getDataSourceID());
        assertEquals(LocationType.Platform, platformOne.getLocationType());

        assertEquals(DataSourceID.tfgm, station.getDataSourceID());
    }

    @Test
    @Disabled("naptan load is disabled for trams")
    void shouldHaveAreaForCityCenterStop() {
        Station station = transportData.getStationById(StPetersSquare.getId());
        assertEquals("St Peter's Square", station.getName());
    }

    @Test
    void shouldHavePlatformAndAreaForCityCenter() {
        IdFor<Platform> platformId = PlatformId.createId(StPetersSquare.getId(), "3");

        //assertTrue(transportData.hasPlatformId(id));
        Platform platform = transportData.getPlatformById(platformId);
        assertNotNull(platform, "could not find " + platformId);
        assertEquals("St Peter's Square platform 3", platform.getName());
        assertEquals(TramStations.StPetersSquare.createIdFor("3"), platform.getId());
    }

    @Test
    void shouldHaveAllEndOfLineTramStations() {

        // Makes sure none are missing from the data
        List<Station> filteredStations = transportData.getStations(EnumSet.of(Tram)).stream()
                .filter(TramStations::isEndOfLine).collect(Collectors.toList());

        assertEquals(TramStations.EndOfTheLine.size(), filteredStations.size());
    }

    @Test
    void shouldHaveConsistencyOfRouteAndTripAndServiceIds() {
        Set<Route> allTramRoutes = transportData.getRoutes(EnumSet.of(Tram));

        Set<Service> uniqueSvcs = allTramRoutes.stream().map(Route::getServices).flatMap(Collection::stream).collect(Collectors.toSet());

        assertEquals(uniqueSvcs.size(), allServices.size());

        Set<Station> allsStations = transportData.getStations(EnumSet.of(Tram));

        Set<Trip> allTrips = new HashSet<>();
        allsStations.forEach(station -> allTrips.addAll(getTripsFor(transportData.getTrips(), station)));

        long tripsSize = transportData.getTrips().stream().filter(trip -> trip.getTransportMode()==Tram).count();
        assertEquals(tripsSize, allTrips.size());

        IdSet<Trip> tripIdsFromSvcs = allTramRoutes.stream().map(Route::getTrips).
                flatMap(Collection::stream).
                map(Trip::getId).collect(IdSet.idCollector());
        assertEquals(tripsSize, tripIdsFromSvcs.size());

    }

    @Test
    void shouldBeApplyingExceptionalDatesCorrectly() {

        TransportDataReaderFactory dataReaderFactory = componentContainer.get(TransportDataReaderFactory.class);
        List<TransportDataReader> transportDataReaders = dataReaderFactory.getReaders();
        TransportDataReader transportDataReader = transportDataReaders.get(0); // yuk
        Set<CalendarDateData> calendarsDates = transportDataReader.getCalendarDates().collect(Collectors.toSet());

        Set<Service> calendarsForTrams = transportData.getServices();

        Set<CalendarDateData> applyToCurrentServices = calendarsDates.stream().
                filter(calendarDateData -> transportData.hasServiceId(calendarDateData.getServiceId())).
                collect(Collectors.toSet());

        // are not any in the data at the moment 15/11/2022
        assertFalse(applyToCurrentServices.isEmpty());

        assertFalse(applyToCurrentServices.isEmpty(), "did not alter any of " + HasId.asIds(calendarsForTrams));

        assertEquals(1,  config.getGTFSDataSource().size(), "expected only one data source");
        GTFSSourceConfig sourceConfig = config.getGTFSDataSource().get(0);
        TramDateSet excludedByConfig = TramDateSet.of(sourceConfig.getNoServices());

        applyToCurrentServices.forEach(exception -> {
            Service service = transportData.getServiceById(exception.getServiceId());
            ServiceCalendar calendar = service.getCalendar();

            TramDate exceptionDate = exception.getDate();
            int exceptionType = exception.getExceptionType();
            if (exceptionType == CalendarDateData.ADDED) {
                if (excludedByConfig.contains(exceptionDate)) {
                    assertFalse(calendar.operatesOn(exceptionDate));
                } else {
                    assertTrue(calendar.operatesOn(exceptionDate));
                }
            } else if (exceptionType == CalendarDateData.REMOVED) {
                assertFalse(calendar.operatesOn(exceptionDate));
            }
        });
    }

    @Test
    void shouldReproIssueAtMediaCityWithBranchAtCornbrook() {
        Set<Trip> allTrips = getTripsFor(transportData.getTrips(), Cornbrook);

        Set<Route> routes = transportData.findRoutesByShortName(MutableAgency.METL, AshtonUnderLyneManchesterEccles.shortName());

        assertFalse(routes.isEmpty());

        Set<Trip> toMediaCity = allTrips.stream().
                filter(trip -> trip.callsAt(Cornbrook)).
                filter(trip -> trip.callsAt(TramStations.MediaCityUK)).
                filter(trip -> routes.contains(trip.getRoute())).
                collect(Collectors.toSet());

        Set<Service> services = toMediaCity.stream().
                map(Trip::getService).collect(Collectors.toSet());

        TramDate nextTuesday = TestEnv.testTramDay();

        Set<Service> onDay = services.stream().
                filter(service -> service.getCalendar().operatesOn(nextTuesday)).
                collect(Collectors.toSet());

        assertFalse(onDay.isEmpty());

        TramTime time = TramTime.of(12, 0);

        long onTimeTrips = toMediaCity.stream().
                filter(trip -> trip.departTime().isBefore(time)).
                filter(trip -> trip.arrivalTime().isAfter(time)).
                count();

        assertTrue(onTimeTrips>0);

    }

    @Disabled("Duplicated of test that checks for every station")
    @DataExpiryCategory
    @Test
    void shouldHaveCorrectDataForTramsCallingAtVeloparkMonday8AM() {
        Set<Trip> origTrips = getTripsFor(transportData.getTrips(), TramStations.VeloPark);

        TramDate aMonday = TestEnv.nextMonday();
        assertEquals(DayOfWeek.MONDAY, aMonday.getDayOfWeek());

        IdSet<Service> mondayServices = allServices.stream()
                .filter(svc -> svc.getCalendar().operatesOn(aMonday))
                .collect(IdSet.collector());

        // reduce the trips to the ones for the right route on the monday by filtering by service ID
        List<Trip> filteredTrips = origTrips.stream().filter(trip -> mondayServices.contains(trip.getService().getId())).
                collect(Collectors.toList());

        assertFalse(filteredTrips.isEmpty(), "No trips for velopark on " + aMonday);

        // find the stops, invariant is now that each trip ought to contain a velopark stop
        List<StopCall> stoppingAtVelopark = filteredTrips.stream()
                .filter(trip -> mondayServices.contains(trip.getService().getId()))
                .map(trip -> getStopsFor(trip, TramStations.VeloPark.getId()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertEquals(filteredTrips.size(), stoppingAtVelopark.size());

        // finally check there are trams stopping within 15 mins of 8AM on Monday
        stoppingAtVelopark.removeIf(stop -> {
            TramTime arrivalTime = stop.getArrivalTime();
            return arrivalTime.asLocalTime().isAfter(LocalTime.of(7,59)) &&
                    arrivalTime.asLocalTime().isBefore(LocalTime.of(8,16));
        });

        assertTrue(stoppingAtVelopark.size()>=1); // at least 1
        assertNotEquals(filteredTrips.size(), stoppingAtVelopark.size());
    }

    @Disabled("Performance tests")
    @Test
    void shouldLoadData() {
        PopulateTransportDataFromSources transportDataFromFiles = componentContainer.get(PopulateTransportDataFromSources.class);

        int count = 10;
        //int count = 1;
        long total = 0;
        for (int i = 0; i < count; i++) {
            long begin = System.currentTimeMillis();
            //TransportDataFromFiles fromFiles = builder.create();

            transportDataFromFiles.getData();
            long finish = System.currentTimeMillis();

            total = total + (finish - begin);
        }

        System.out.printf("Total: %s ms Average: %s ms%n", total, total/count);
    }

    private List<StopCall> getStopsFor(Trip trip, IdFor<Station> stationId) {
        return trip.getStopCalls().stream().
                filter(stopCall -> stopCall.getStationId().equals(stationId)).
                collect(Collectors.toList());
    }

}
