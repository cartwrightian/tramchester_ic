package com.tramchester.integration.repository.buses;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.dataimport.loader.TransportDataReader;
import com.tramchester.dataimport.loader.TransportDataReaderFactory;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.ServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.dates.TramDateSet;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static org.junit.jupiter.api.Assertions.*;

@DataUpdateTest
@BusTest
public
class TransportDataFromFilesBusTest {

    public static final int TGFM_BUS_AGENCIES = 26;
    public static final int TGFM_BUS_ROUTES = 685;
    public static final int NUM_TFGM_BUS_STATIONS = 15697;
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private TransportData transportData;
    private Collection<Service> allServices;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationBusTestConfig();
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
        allServices = transportData.getServices();
    }

    @Test
    void shouldHaveExpectedAgenciesNumbersForBus() {
        assertEquals(TGFM_BUS_AGENCIES, transportData.getAgencies().size());
    }


    @Test
    void shouldHaveExpectedStationAndPlatformNumbersForBus() {

        int numStations = transportData.getStations().size();
        assertWithinNPercent(NUM_TFGM_BUS_STATIONS, numStations, 0.1F);

        // no platforms represented in bus data
        Set<Platform> platforms = transportData.getPlatforms(EnumSet.of(Bus));
        assertEquals(0, platforms.size(), platforms.toString());
    }

    @Test
    void shouldNotHavePlatformStations() {
        Set<Station> hasPlatforms = transportData.getActiveStationStream().
                filter(Station::hasPlatforms).
                collect(Collectors.toSet());

        assertTrue(hasPlatforms.isEmpty(), hasPlatforms.toString());
    }

    @Test
    void shouldJustHaveBusStations() {
        long tram = transportData.getActiveStationStream().
                filter(station -> station.getTransportModes().contains(TransportMode.Tram)).
                count();
        assertEquals(0, tram);
    }

    @Test
    void shouldGetAgencies() {
        Set<Agency> agencies = transportData.getAgencies();
        assertTrue(agencies.contains(TestEnv.WarringtonsOwnBuses), HasId.asIds(agencies));
        assertTrue(agencies.contains(TestEnv.BEE_A), HasId.asIds(agencies));
    }

    @Test
    void shouldHaveNotHaveRoutesWithZeroTrips() {
        Set<Route> routes = transportData.getRoutes();

        IdSet<Route> emptyRoutes = routes.stream().
                filter(route -> route.getTrips().isEmpty()).
                map(Route::getId).
                collect(IdSet.idCollector());

//        assertEquals(Collections.emptySet(), emptyRoutes);

        // should be zero? but seem to have 2 at the moment, see also above
        assertEquals(0, emptyRoutes.size());

    }

    @Test
    void shouldHaveExpectedEndOfLinesAndRoutes() {
        IdFor<Agency> agencyId = Agency.createId("7778532");

        Set<Route> outbounds = transportData.findRoutesByName(agencyId, "Accrington - Rawtenstall - Bacup - Rochdale");
        assertFalse(outbounds.isEmpty());
        outbounds.forEach(outbound -> assertEquals("464", outbound.getShortName()));

        Station firstStation = transportData.getStationById(Station.createId("2500ACC0009"));
        assertEquals("Bus Station (Stand 9), Accrington, Accrington", firstStation.getName());
        outbounds.forEach(outbound -> assertFalse(firstStation.servesRouteDropOff(outbound)));
        outbounds.forEach(outbound -> assertTrue(firstStation.servesRoutePickup(outbound)));

        Station secondStation = transportData.getStationById(Station.createId("2500LAA15791"));
        assertEquals("Infant Street (opp Abbey St), Accrington, Hyndburn", secondStation.getName());
        outbounds.forEach(outbound -> assertTrue(secondStation.servesRoutePickup(outbound)));
        outbounds.forEach(outbound -> assertTrue(secondStation.servesRouteDropOff(outbound)));
    }

    @Test
    void shouldGetServicesByDate() {
        TramDate nextSaturday = UpcomingDates.nextSaturday();
        Set<Service> results = transportData.getServicesOnDate(nextSaturday, config.getTransportModes());

        assertFalse(results.isEmpty());
        long onCorrectDate = results.stream().
                filter(svc -> svc.getCalendar().operatesOn(nextSaturday)).count();
        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

//        TramDate noBusesDate = TramDate.from(TestEnv.LocalNow().plusMonths(36));
//        Set<Service> futureServices = transportData.getServicesOnDate(noBusesDate);
//        assertTrue(results.size() > futureServices.size(), "expected " + results.size() + " > " + futureServices.size());
    }

    @Test
    void shouldGetStations() {

        for(BusStations testStation : BusStations.values()) {
            IdFor<Station> testStationId = testStation.getId();
            assertTrue(transportData.hasStationId(testStationId), "stop id is missing for " + testStation.name() + " id:"+ testStationId);
            Station found = transportData.getStationById(testStationId);
            assertEquals(testStation.getName(), found.getName());
        }
    }

    @Disabled("too slow currently for buses")
    @Test
    void shouldHaveConsistencyOfRouteAndTripAndServiceIds() {
        Collection<Route> allRoutes = transportData.getRoutes();
        List<Integer> svcSizes = new LinkedList<>();

        allRoutes.forEach(route -> svcSizes.add(route.getServices().size()));

        int allSvcs = svcSizes.stream().reduce(0, Integer::sum);

        assertEquals(allSvcs, allServices.size());

        Set<Station> allsStations = transportData.getStations();

        Set<Trip> allTrips = new HashSet<>();
        allsStations.forEach(station -> allTrips.addAll(getTripsFor(transportData.getTrips(), station)));

        int tripsSize = transportData.getTrips().size();
        assertEquals(tripsSize, allTrips.size());

        IdSet<Trip> tripIdsFromSvcs = transportData.getRoutes().stream().map(Route::getTrips).
                flatMap(Trips::stream).
                map(Trip::getId).collect(IdSet.idCollector());
        assertEquals(tripsSize, tripIdsFromSvcs.size());

    }

    @Test
    void shouldBeApplyingExceptionalDatesCorrectly() {

        TransportDataReaderFactory dataReaderFactory = componentContainer.get(TransportDataReaderFactory.class);
        List<TransportDataReader> transportDataReaders = dataReaderFactory.getReaders();
        TransportDataReader transportDataReader = transportDataReaders.get(0); // yuk

        Stream<CalendarDateData> allExceptions = transportDataReader.getCalendarDates();

        Set<CalendarDateData> exceptionalDatesForServices = allExceptions.
                filter(calendarDateData -> transportData.hasServiceId(calendarDateData.getServiceId())).
                collect(Collectors.toSet());

        allExceptions.close();

        assertFalse(exceptionalDatesForServices.isEmpty());

        assertEquals(1,  config.getGTFSDataSource().size(), "expected only one data source");
        GTFSSourceConfig sourceConfig = config.getGTFSDataSource().get(0);
        TramDateSet excludedByConfig = TramDateSet.of(sourceConfig.getNoServices());

        exceptionalDatesForServices.forEach(exception -> {
            Service service = transportData.getServiceById(exception.getServiceId());
            ServiceCalendar calendar = service.getCalendar();

            TramDate exceptionDate = exception.getDate();
            int exceptionType = exception.getExceptionType();
            if (exceptionType == CalendarDateData.ADDED) {
                if (excludedByConfig.contains(exceptionDate)) {
                    assertFalse(calendar.operatesOn(exceptionDate));
                } else {
                    assertTrue(calendar.operatesOn(exceptionDate), "missing " + exception + " from " + calendar);
                }
            } else if (exceptionType == CalendarDateData.REMOVED) {
                assertFalse(calendar.operatesOn(exceptionDate));
            }
        });
    }

    // for things changing very frequently
    private void assertWithinNPercent(long expected, long actual, float percentage) {
        int margin = Math.round(expected * percentage);
        long upper = expected + margin;
        long lower = expected - margin;

        String diagnostic = String.format("%s not within %s of %s", actual, percentage, expected);
        assertTrue( (actual>lower) && (actual<upper), diagnostic);
    }

}
