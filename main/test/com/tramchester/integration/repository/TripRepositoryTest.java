package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.*;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.UpcomingDates;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
@DataUpdateTest
public class TripRepositoryTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private TripRepository tripRepository;
    private TramDate when;
    private StationRepository stationRepository;
    private ClosedStationsRepository closedStationRepository;

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
        tripRepository = componentContainer.get(TripRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        closedStationRepository = componentContainer.get(ClosedStationsRepository.class);

        when = TestEnv.testDay(); // filter by date, otherwise get variations due to upcoming routes etc

    }

    // to get diagnose issues with new GTFS data and no journeys alty to Navigation Rd post midnight
    @Test
    void shouldHaveTripsFromAltrinchamPostMidnight() {
        Set<Trip> fromAlty = tripRepository.getTrips().stream().
                filter(trip -> !trip.isFiltered()).
                filter(trip -> trip.operatesOn(when)).
                filter(trip -> trip.firstStation().equals(Altrincham.getId())).collect(Collectors.toSet());

        assertFalse(fromAlty.isEmpty());

        TimeRange range = TimeRangePartial.of(TramTime.of(0,1), Duration.ZERO, Duration.ofMinutes(config.getMaxWait()));
        TimeRange nextRange = range.transposeToNextDay();

        Set<Trip> atTime = fromAlty.stream().
                filter(trip -> range.contains(trip.departTime()) || nextRange.contains(trip.departTime())).collect(Collectors.toSet());

        assertFalse(atTime.isEmpty());

        HasId<Station> navigationRd = NavigationRoad.from(stationRepository);
        Set<Trip> calls = atTime.stream().filter(trip -> trip.callsAt(navigationRd.getId())).collect(Collectors.toSet());

        assertTrue(calls.size() > 1);
    }

    @Test
    void shouldReproIssueWithShudehillAppearingOnRedRoute() {

        RouteRepository routeRespository = componentContainer.get(RouteRepository.class);
        TramRouteHelper tramRouteHelper = new TramRouteHelper(routeRespository);

        Station shudehill = Shudehill.from(stationRepository);

        Set<Trip> trips = tripRepository.getTripsCallingAt(shudehill, when);

        assertFalse(trips.isEmpty());

        Set<Route> routes = trips.stream().map(Trip::getRoute).collect(Collectors.toSet());

        assertFalse(routes.isEmpty());
        assertEquals(4, routes.size(), HasId.asIds(routes));

        assertTrue(routes.contains(tramRouteHelper.getOneRoute(TFGMRouteNames.Green, when)));
        assertTrue(routes.contains(tramRouteHelper.getOneRoute(TFGMRouteNames.Blue, when)));
        assertTrue(routes.contains(tramRouteHelper.getOneRoute(TFGMRouteNames.Navy, when)));
        assertTrue(routes.contains(tramRouteHelper.getOneRoute(TFGMRouteNames.Yellow, when)));

        assertFalse(routes.contains(tramRouteHelper.getOneRoute(TFGMRouteNames.Red, when)));
        assertFalse(routes.contains(tramRouteHelper.getOneRoute(TFGMRouteNames.Purple, when)));

    }

    @Disabled("Solved by removing reboarding filter which does not impact depth first performance")
    @Test
    void shouldCheckTripsFinishingAtNonInterchangeStationsOrEndOfLines() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
        Set<Trip> allTrips = tripRepository.getTrips();

        Set<String> endTripNotInterchange = allTrips.stream().
                map(trip -> trip.getStopCalls().getLastStop()).
                map(StopCall::getStation).
                filter(station -> !interchangeRepository.isInterchange(station)).
                filter(station -> !TramStations.isEndOfLine(station)).
                map(Station::getName).
                collect(Collectors.toSet());

        assertTrue(endTripNotInterchange.isEmpty(), "End trip not interchange: " + endTripNotInterchange);
    }

    @DataExpiryTest
    @Test
    void shouldHaveTripsOnDateForEachStation() {

        List<Pair<TramDate, IdFor<Station>>> missing = UpcomingDates.getUpcomingDates().
                flatMap(date -> getStations(date).map(station -> Pair.of(date, station))).
                filter(pair -> isOpen(pair.getLeft(), pair.getRight())).
                filter(pair -> tripRepository.getTripsCallingAt(pair.getRight(), pair.getLeft()).isEmpty()).
                map(pair -> Pair.of(pair.getLeft(), pair.getRight().getId())).
                sorted(Map.Entry.comparingByKey()).
                toList();

        assertTrue(missing.isEmpty(), "Got missing trips for " + missing);

    }
    
    @DataExpiryTest
    @Test
    void shouldHaveServicesRunningAtReasonableTimesNDaysAhead() {

        int latestHour = 23;
        int earliestHour = 7;

        final List<TramTime> times = IntStream.range(earliestHour, latestHour).boxed().
                map(hour -> TramTime.of(hour, 0)).
                sorted().
                toList();

        int maxwait = 25;

        final Map<Pair<TramDate, TramTime>, IdSet<Station>> missing = new HashMap<>();

        UpcomingDates.getUpcomingDates().
                forEach(date -> getStations(date).
                        forEach(station -> {
                            final Set<Trip> trips = tripRepository.getTripsCallingAt(station, date);
                            final List<TramTime> timesToCheck = getTimesFor(times, station, date);
                            for (final TramTime time : timesToCheck) {
                                final TimeRange range = TimeRangePartial.of(time.minusMinutes(maxwait), time.plusMinutes(maxwait));
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
                        }));

        assertTrue(missing.isEmpty(), missing.toString());
    }

    @Test
    void shouldReproIssueAtMediaCityWithBranchAtCornbrook() {
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);

        Set<Trip> allTrips = getTripsFor(tripRepository.getTrips(), Cornbrook);

        Set<Route> routes = routeRepository.findRoutesByShortName(MutableAgency.METL, KnownTramRoute.getBlue(when).shortName());

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

    @Test
    void shouldHaveSundayTripsFromCornbrook() {
        ServiceRepository serviceRepository = componentContainer.get(ServiceRepository.class);

        TramDate nextSunday = UpcomingDates.nextSunday();

        Set<Service> sundayServices = serviceRepository.getServicesOnDate(nextSunday, TramsOnly);

        Set<Trip> cornbrookTrips = tripRepository.getTrips().stream().
                filter(trip -> trip.callsAt(Cornbrook.getId())).collect(Collectors.toSet());

        Set<Trip> sundayTrips = cornbrookTrips.stream().
                filter(trip -> sundayServices.contains(trip.getService())).collect(Collectors.toSet());

        assertFalse(sundayTrips.isEmpty());
    }

    private Stream<Station> getStations(final TramDate date) {
        return stationRepository.getStations(TramsOnly).stream().
                filter(station -> !UpcomingDates.hasClosure(station, date));
    }

    private boolean isOpen(final TramDate date, final Station station) {
        return ! (closedStationRepository.isClosed(station, date) || UpcomingDates.hasClosure(station, date));
    }

    private List<TramTime> getTimesFor(final List<TramTime> times, final Station station, final TramDate date) {
        if (UpcomingDates.hasClosure(station.getId(), date)) {
            return Collections.emptyList();
        }
//        if (date.equals(UpcomingDates.PiccAshtonImprovementWorks)) {
//            return times.stream().
//                    filter(time -> !UpcomingDates.hasClosure(station, date)).toList();
//        }
        return times;
    }
}
