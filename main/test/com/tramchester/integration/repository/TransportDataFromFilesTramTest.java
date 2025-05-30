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
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.livedata.tfgm.TramDepartureFactory;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.reference.FakeStation;
import com.tramchester.domain.reference.TFGMRouteNames;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DataExpiryTest;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.CentralZoneStation.StPetersSquare;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
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
        closedStationRepository = componentContainer.get(ClosedStationsRepository.class);

        when = TestEnv.testDay(); // filter by date, otherwise get variations due to upcoming routes etc

    }

    @Test
    void shouldHaveExpectedNumbersForTram() {
        assertEquals(1, transportData.getAgencies().stream().filter(agency -> agency.getTransportModes().contains(Tram)).count());

        // NOTE: one cause here is closures meaning no stop calls for a station - so unless in config as closed this will mean
        // that station is never loaded
        assertEquals(NUM_TFGM_TRAM_STATIONS, transportData.getStations(TramsOnly).size());

        // -1 May 2025
        int expectedPlatforms = 201-1;
        assertEquals(expectedPlatforms, transportData.getPlatforms(TramsOnly).size());
    }

    @Test
    void shouldHaveExpectedNumRoutes() {
        Set<String> uniqueNames = transportData.getRoutesRunningOn(when, TramsOnly).stream().
//                filter(route -> route.getTransportMode()==Tram).
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
        assertIdEquals("7778482", agencies.getFirst().getId());
        assertEquals("Metrolink", agencies.getFirst().getName());
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

        // 2->1 picc gardens summer 2025
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

        IdSet<Route> routeIds =
                routeStations.stream().
                        map(RouteStation::getRoute).
                        collect(IdSet.collector());

        // Picc gardens 2024
        assertTrue(routeIds.contains(getNavy(when).getId()), routeIds.toString());
    }

    @Test
    void shouldHaveRoutesWithStationsAndCallingPoints() {
        // Note, in logs check for "METROLINK Agency seen with transport type bus for"
        // when this fails.....
        Set<Route> allTramRoutes = transportData.getRoutes(TramsOnly);

        Set<Station> allStations = transportData.getStations(TramsOnly);

        Set<Route> noPickups = allTramRoutes.stream().
                filter(route -> allStations.stream().noneMatch(station -> station.servesRoutePickup(route))).
                collect(Collectors.toSet());

        assertTrue(noPickups.isEmpty(), noPickups.toString());

        Set<Route> noDropOffs = allTramRoutes.stream().
                filter(route -> allStations.stream().noneMatch(station -> station.servesRouteDropOff(route))).
                collect(Collectors.toSet());

        assertTrue(noDropOffs.isEmpty(), noDropOffs.toString());
    }

    @Test
    void shouldGetRouteStationsForStation() {
        Set<RouteStation> routeStations = transportData.getRouteStationsFor(OldTrafford.getId());

        Set<String> lines =
                routeStations.stream().
                        filter(routeStation -> routeStation.getRoute().isAvailableOn(when)).
                        map(routeStation -> routeStation.getRoute().getShortName()).
                        collect(Collectors.toSet());

        // 2->1 picc gardens summer 2025
        assertEquals(1, lines.size(), lines.toString());

        //assertTrue(lines.contains(TFGMRouteNames.Purple.getShortName()));
        assertTrue(lines.contains(TFGMRouteNames.Green.getShortName()));

    }

    @Test
    void shouldGetServicesByDate() {
        TramDate nextSaturday = UpcomingDates.nextSaturday();
        Set<Service> results = transportData.getServicesOnDate(nextSaturday, TramsOnly);

        assertFalse(results.isEmpty(), "no services next saturday");
        long onCorrectDate = results.stream().
                filter(svc -> svc.getCalendar().operatesOn(nextSaturday)).count();

        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

        TramDate noTrams = nextSaturday.plusWeeks(11*52);
        results = transportData.getServicesOnDate(noTrams, TramsOnly);
        assertTrue(results.isEmpty(), "not empty, got " + results);
    }

    @Test
    void shouldHaveSaneServiceStartAndFinishTimes() {
        Set<Service> badTimings = allServices.stream().filter(svc -> svc.getStartTime().isAfter(svc.getFinishTime())).
                collect(Collectors.toSet());
        assertTrue(badTimings.isEmpty());
    }

    @DataExpiryTest
    @Test
    void shouldHaveServiceEndDatesBeyondNextNDays() {

//        TramDate startDate = TramDate.from(TestEnv.LocalNow());
//        TramDate endDate = startDate.plusDays(DAYS_AHEAD);

        DateRange dateRange = DateRange.from(UpcomingDates.daysAhead());

        Set<Service> services = transportData.getServices();
        Set<Service> expiringServices = services.stream().
                filter(service -> !service.getCalendar().getDateRange().overlapsWith(dateRange)).
                collect(Collectors.toSet());

        assertNotEquals(services, expiringServices, "all services are expiring");
    }

    @Test
    void shouldHandleStopCallsThatCrossMidnight() {
        Set<Route> routes = transportData.getRoutes(TramsOnly);

        for (Route route : routes) {
            List<StopCalls.StopLeg> over = route.getTrips().stream().flatMap(trip -> trip.getStopCalls().getLegs(false).stream()).
                    filter(stopLeg -> stopLeg.getCost().compareTo(Duration.ofMinutes(12*24)) > 0).
                    toList();
            assertTrue(over.isEmpty(), over.toString());
        }

    }

    @DataExpiryTest
    @Test
    void shouldHaveTramServicesAvailableNDaysAhead() {

        Set<TramDate> noServices = UpcomingDates.getUpcomingDates().
                filter(date -> !date.isChristmasPeriod()).
                filter(date -> transportData.getServicesOnDate(date, TramsOnly).isEmpty()).
                collect(Collectors.toSet());

        assertTrue(noServices.isEmpty(), "no services on " + noServices);

    }

    @Test
    void shouldHaveServicesThatIncludeDateInRange() {

        Set<Service> allServices = transportData.getServices();

        Set<Service> includeDate = allServices.stream().filter(service -> service.getCalendar().getDateRange().contains(when)).collect(Collectors.toSet());

        assertFalse(includeDate.isEmpty());

        Set<Service> onActualDate = includeDate.stream().filter(service -> service.getCalendar().operatesOn(when)).collect(Collectors.toSet());

        assertFalse(onActualDate.isEmpty());
    }

    @Test
    void shouldHavePlatformAndAreaForCityCenter() {
        IdFor<Platform> platformId = PlatformId.createId(StPetersSquare.getId(), "2");

        assertTrue(transportData.hasPlatformId(platformId));
        Platform platform = transportData.getPlatformById(platformId);
        assertNotNull(platform, "could not find " + platformId);
        assertEquals("St Peter's Square platform 2", platform.getName());
        IdFor<Platform> expectedPlatformId = PlatformId.createId(StPetersSquare.getId(), "2");
        assertEquals(expectedPlatformId, platform.getId());
    }

    @Test
    void shouldHavePlatformAndAreaForCityCenterCrackedRail() {
        IdFor<Platform> platformId = PlatformId.createId(StPetersSquare.getId(), "2");

        Platform platform = transportData.getPlatformById(platformId);
        assertNotNull(platform, "could not find " + platformId);
        assertEquals("St Peter's Square platform 2", platform.getName());
        IdFor<Platform> expectedPlatformId = PlatformId.createId(StPetersSquare.getId(), "2");
        assertEquals(expectedPlatformId, platform.getId());
    }

    @Test
    void shouldHaveAllEndOfLineTramStations() {

        // Makes sure none are missing from the data
        List<Station> filteredStations = transportData.getStations(TramsOnly).stream()
                .filter(TramStations::isEndOfLine).toList();

        assertEquals(TramStations.getEndOfTheLine().size(), filteredStations.size());
    }

    @Test
    void shouldHaveConsistencyOfRouteAndTripAndServiceIds() {
        Set<Route> allTramRoutes = transportData.getRoutes(TramsOnly);

        Set<Service> uniqueSvcs = allTramRoutes.stream().map(Route::getServices).flatMap(Collection::stream).collect(Collectors.toSet());

        assertEquals(uniqueSvcs.size(), allServices.size());

        Set<Station> allsStations = transportData.getStations(TramsOnly);

        Set<Trip> allTrips = new HashSet<>();
        allsStations.forEach(station -> allTrips.addAll(getTripsFor(transportData.getTrips(), station)));

        long tripsSize = transportData.getTrips().stream().filter(trip -> trip.getTransportMode()==Tram).count();
        assertEquals(tripsSize, allTrips.size());

        IdSet<Trip> tripIdsFromSvcs = allTramRoutes.stream().map(Route::getTrips).
                flatMap(Trips::stream).
                map(Trip::getId).collect(IdSet.idCollector());
        assertEquals(tripsSize, tripIdsFromSvcs.size());

    }

    @Test
    void shouldBeApplyingExceptionalDatesCorrectly() {

        TransportDataReaderFactory dataReaderFactory = componentContainer.get(TransportDataReaderFactory.class);
        List<TransportDataReader> transportDataReaders = dataReaderFactory.getReaders();
        TransportDataReader transportDataReader = transportDataReaders.getFirst(); // yuk
        Set<CalendarDateData> calendarsDates = transportDataReader.getCalendarDates().collect(Collectors.toSet());

        Set<Service> calendarsForTrams = transportData.getServices();

        Set<CalendarDateData> applyToCurrentServices = calendarsDates.stream().
                filter(calendarDateData -> transportData.hasServiceId(calendarDateData.getServiceId())).
                collect(Collectors.toSet());

        // are not any in the data at the moment 15/11/2022
        assertFalse(applyToCurrentServices.isEmpty(), "did not alter any of " + HasId.asIds(calendarsForTrams));

        assertEquals(1,  config.getGTFSDataSource().size(), "expected only one data source");
        GTFSSourceConfig sourceConfig = config.getGTFSDataSource().getFirst();
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

        transportData.getStations(TramsOnly).stream().
                filter(station -> !closedStationRepository.isStationClosed(station.getId(), when)).
                forEach(station -> {
                    IdFor<Station> stationId = station.getId();
                    Set<Trip> all = transportData.getTrips().stream().filter(trip -> trip.callsAt(stationId)).collect(Collectors.toSet());

                    long passingThrough = all.stream().filter(trip -> !trip.firstStation().equals(stationId)).
                            filter(trip -> !trip.lastStation().equals(stationId)).count();

                    if (passingThrough==0) {
                        result.add(stationId);
                    }
        });

        IdSet<Station> expected = TramStations.getEndOfTheLine().stream().
                map(FakeStation::getId).
                filter(stationId -> !closedStationRepository.isStationClosed(stationId, when)).
                collect(IdSet.idCollector());

        IdSet<Station> disjunction = IdSet.disjunction(expected, result);
        assertTrue(disjunction.isEmpty(), disjunction + " diff between \n expected " + expected + " and \n result " + result);

    }

    @Test
    void shouldHaveExpectedTraffordParkPlatformsForLiveDataWorkaround() {

        Station traffordPark = TraffordCentre.from(transportData);
        List<Platform> traffordCentrePlatforms = new ArrayList<>(traffordPark.getPlatforms());
        assertEquals(1, traffordCentrePlatforms.size(), HasId.asIds(traffordCentrePlatforms));

        Platform platform = traffordCentrePlatforms.getFirst();
        assertEquals("2", platform.getPlatformNumber());

        assertTrue(transportData.hasPlatformId(platform.getId()), "expected present in timetable");

        assertEquals(TramDepartureFactory.TRAFFORD_CENTER_PLATFORM2, platform.getId().getGraphId());
    }

    @Test
    void shouldFindMaxNumberTripsForAService() {

        // here to find how many trips we might see for each service

        Map<IdFor<Service>, AtomicInteger> tripsPerService = new HashMap<>();

        transportData.getServices().forEach(service -> tripsPerService.put(service.getId(), new AtomicInteger(0)));
        transportData.getTrips().forEach(trip -> tripsPerService.get(trip.getService().getId()).incrementAndGet());

        int maximumNumberOfTrips = tripsPerService.values().stream().map(AtomicInteger::get).max(Integer::compare).orElse(-1);

        assertEquals(1692, maximumNumberOfTrips);
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


}
