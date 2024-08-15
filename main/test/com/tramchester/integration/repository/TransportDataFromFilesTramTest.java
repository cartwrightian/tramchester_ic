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
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.FakeStation;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DataExpiryCategory;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import com.tramchester.testSupport.testTags.ShudehillMarketStreetClosedTestCategory;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.CentralZoneStation.StPetersSquare;
import static com.tramchester.domain.reference.CentralZoneStation.TraffordBar;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

// TODO WIP Split out by i/f roles, this has gotten too big
// Begun, started to create tests by repository instead

@ExtendWith(ConfigParameterResolver.class)
@DualTest
@DataUpdateTest
public class TransportDataFromFilesTramTest {

    public static final int NUM_TFGM_TRAM_STATIONS = 99;
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

        int expectedPlatforms = 200 - 1; //
        assertEquals(expectedPlatforms, transportData.getPlatforms(EnumSet.of(Tram)).size());
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
        assertFalse(result.version().isEmpty());
    }

    @Test
    void shouldGetAgenciesWithNames() {
        List<Agency> agencies = transportData.getAgencies().stream().
                filter(agency -> agency.getTransportModes().contains(Tram)).
                toList();

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

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(PiccadillyAltrincham)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(RochdaleShawandCromptonManchesterEastDidisbury)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(VictoriaWythenshaweManchesterAirport)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(EcclesManchesterAshtonUnderLyne)));

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

        // London road closure 2 -> 1
        assertEquals(1, uniqueRouteNames.size(), uniqueRouteNames.toString());
    }

    @Test
    void shouldGetRouteStationsForStationOnOneRoute() {
        Set<RouteStation> routeStations = transportData.getRouteStationsFor(ManAirport.getId()).stream().
            filter(routeStation -> routeStation.getRoute().isAvailableOn(when)).
            collect(Collectors.toSet());

        assertFalse(routeStations.isEmpty());

        Set<Pair<IdFor<Station>, String>> routeStationPairs = routeStations.stream().
                map(routeStation -> Pair.of(routeStation.getStationId(), routeStation.getRoute().getName())).
                collect(Collectors.toSet());

        assertEquals(1, routeStationPairs.size(), routeStationPairs.toString());

        Set<String> routeNames =
                routeStations.stream().
                        map(RouteStation::getRoute).
                        map(Route::getName).collect(Collectors.toSet());

        assertTrue(routeNames.contains(VictoriaWythenshaweManchesterAirport.longName()), routeNames.toString());
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

    @ShudehillMarketStreetClosedTestCategory
    @Test
    void shouldGetRouteStationsForStation() {
        Set<RouteStation> routeStations = transportData.getRouteStationsFor(Shudehill.getId());

        Set<Pair<IdFor<Station>, String>> routeStationPairs = routeStations.stream().
                filter(routeStation -> routeStation.getRoute().isAvailableOn(when)).
                map(routeStation -> Pair.of(routeStation.getStationId(), routeStation.getRoute().getName())).
                collect(Collectors.toSet());

        assertEquals(3, routeStationPairs.size(), routeStations.toString());

        Set<String> routeNames =
                routeStations.stream().
                        map(RouteStation::getRoute).
                        map(Route::getName).collect(Collectors.toSet());

        assertTrue(routeNames.contains(VictoriaWythenshaweManchesterAirport.longName()), routeNames.toString());

        assertTrue(routeNames.contains(BuryManchesterAltrincham.longName()), routeNames.toString());

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
                filter(trip -> trip.callsAt(Cornbrook.getId())).collect(Collectors.toSet());

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
                    toList();
            assertTrue(over.isEmpty(), over.toString());
        }

    }

    // to get diagnose issues with new GTFS data and no journeys alty to Navigation Rd post midnight
    @Test
    void shouldHaveTripsFromAltrinchamPostMidnight() {
        Set<Trip> fromAlty = transportData.getTrips().stream().
                filter(trip -> !trip.isFiltered()).
                filter(trip -> trip.operatesOn(when)).
                filter(trip -> trip.firstStation().equals(Altrincham.getId())).collect(Collectors.toSet());

        assertFalse(fromAlty.isEmpty());

        TimeRange range = TimeRangePartial.of(TramTime.of(0,1), Duration.ZERO, Duration.ofMinutes(config.getMaxWait()));
        TimeRange nextRange = range.transposeToNextDay();

        Set<Trip> atTime = fromAlty.stream().
                filter(trip -> range.contains(trip.departTime()) || nextRange.contains(trip.departTime())).collect(Collectors.toSet());

        assertFalse(atTime.isEmpty());

        HasId<Station> navigationRd = NavigationRoad.from(transportData);
        Set<Trip> calls = atTime.stream().filter(trip -> trip.callsAt(navigationRd.getId())).collect(Collectors.toSet());

        assertEquals(2, calls.size(), HasId.asIds(calls));
    }

    @DataExpiryCategory
    @Test
    void shouldHaveTramServicesAvailableNDaysAhead() {

        Set<TramDate> noServices = getUpcomingDates().
                filter(date -> !date.isChristmasPeriod()).
                filter(date -> transportData.getServicesOnDate(date).isEmpty()).
                collect(Collectors.toSet());

        assertTrue(noServices.isEmpty(), "no services on " + noServices);

    }

    @DataExpiryCategory
    @Test
    void shouldHaveTripsOnDateForEachStation() {

        Set<Pair<TramDate, IdFor<Station>>> missing = getUpcomingDates().
                filter(date -> !date.isChristmasPeriod()).
                flatMap(date -> transportData.getStations(EnumSet.of(Tram)).stream().map(station -> Pair.of(date, station))).
                filter(pair -> isOpen(pair.getLeft(), pair.getRight())).
                filter(pair -> transportData.getTripsCallingAt(pair.getRight(), pair.getLeft()).isEmpty()).
                map(pair -> Pair.of(pair.getLeft(), pair.getRight().getId())).
                collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), "Got missing trips for " + missing);

    }

    private static Stream<TramDate> getUpcomingDates() {
        return TestEnv.getUpcomingDates();
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
        int earliestHour = 7;

        List<TramTime> times = IntStream.range(earliestHour, latestHour).boxed().
                map(hour -> TramTime.of(hour, 0)).
                sorted().
                toList();

        int maxwait = 25;

        final Map<Pair<TramDate, TramTime>, IdSet<Station>> missing = new HashMap<>();

        getUpcomingDates().filter(date -> !date.isChristmasPeriod()).forEach(date -> {
            transportData.getStations(EnumSet.of(Tram)).stream().
                    filter(station -> isOpen(date, station)).
                    forEach(station -> {
                        Set<Trip> trips = transportData.getTripsCallingAt(station, date);
                        for (TramTime time : times) {
                            TimeRange range = TimeRangePartial.of(time.minusMinutes(maxwait), time.plusMinutes(maxwait));
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
    void shouldHavePlatformAndAreaForCityCenter() {
        IdFor<Platform> platformId = PlatformId.createId(StPetersSquare.getId(), "2");

        assertTrue(transportData.hasPlatformId(platformId));
        Platform platform = transportData.getPlatformById(platformId);
        assertNotNull(platform, "could not find " + platformId);
        assertEquals("St Peter's Square platform 2", platform.getName());
        assertEquals(TramStations.StPetersSquare.createIdFor("2"), platform.getId());
    }

    @Test
    void shouldHavePlatformAndAreaForCityCenterCrackedRail() {
        IdFor<Platform> platformId = PlatformId.createId(StPetersSquare.getId(), "2");

        //assertTrue(transportData.hasPlatformId(id));
        Platform platform = transportData.getPlatformById(platformId);
        assertNotNull(platform, "could not find " + platformId);
        assertEquals("St Peter's Square platform 2", platform.getName());
        assertEquals(TramStations.StPetersSquare.createIdFor("2"), platform.getId());
    }

    @Test
    void shouldHaveAllEndOfLineTramStations() {

        // Makes sure none are missing from the data
        List<Station> filteredStations = transportData.getStations(EnumSet.of(Tram)).stream()
                .filter(TramStations::isEndOfLine).toList();

        assertEquals(TramStations.getEndOfTheLine().size(), filteredStations.size());
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
    void shouldHaveEndOfLineStations() {

        IdSet<Station> result = new IdSet<>();

        transportData.getStations().stream().
                filter(station -> !closedStationRepository.isClosed(station.getId(), when)).
                forEach(station -> {
                    IdFor<Station> stationId = station.getId();
                    Set<Trip> all = transportData.getTrips().stream().filter(trip -> trip.callsAt(stationId)).collect(Collectors.toSet());

                    long passingThrough = all.stream().filter(trip -> !trip.firstStation().equals(stationId)).
                            filter(trip -> !trip.lastStation().equals(stationId)).count();

                    if (passingThrough==0) {
                        result.add(stationId);
                    }
        });

        //Stream.of(Altrincham, EastDidsbury, ManAirport, Eccles, TraffordCentre, Bury, Rochdale, Ashton).

        IdSet<Station> expected = TramStations.getEndOfTheLine().stream().
                map(FakeStation::getId).
                filter(stationId -> !closedStationRepository.isClosed(stationId, when)).
                collect(IdSet.idCollector());

        // exchange square, due to broken rail diversion?
        //expected.add(ExchangeSquare.getId());

        IdSet<Station> disjunction = IdSet.disjunction(expected, result);
        assertTrue(disjunction.isEmpty(), disjunction + " diff between expected " + expected + " and result " + result);

    }

    @Test
    void shouldReproIssueAtMediaCityWithBranchAtCornbrook() {
        Set<Trip> allTrips = getTripsFor(transportData.getTrips(), Cornbrook);

        Set<Route> routes = transportData.findRoutesByShortName(MutableAgency.METL, EcclesManchesterAshtonUnderLyne.shortName());

        assertFalse(routes.isEmpty());

        Set<Trip> toMediaCity = allTrips.stream().
                filter(trip -> trip.callsAt(Cornbrook.getId())).
                filter(trip -> trip.callsAt(TramStations.MediaCityUK.getId())).
                filter(trip -> routes.contains(trip.getRoute())).
                collect(Collectors.toSet());

        Set<Service> services = toMediaCity.stream().
                map(Trip::getService).collect(Collectors.toSet());

        TramDate nextTuesday = TestEnv.testDay();

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


    private boolean isOpen(final TramDate date, final Station station) {
        // workaround timetable not updated yet for Shudehill and MarketStreet, shows still closed after meant to re-open....
        final IdFor<Station> stationId = station.getId();
        if (stationId.equals(Shudehill.getId()) || stationId.equals(MarketStreet.getId())) {
            final DateRange missingFromTimetable = DateRange.of(TramDate.of(2024,8,20), TramDate.of(2024,8,22));
            if (missingFromTimetable.contains(date)) {
                return false;
            }
        }
        return !closedStationRepository.isClosed(station, date);
    }

}
