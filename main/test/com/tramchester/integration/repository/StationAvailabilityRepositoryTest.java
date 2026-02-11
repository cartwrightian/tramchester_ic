package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationCollectionSingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.ServedRoute;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.testTags.DataExpiryTest;
import com.tramchester.testSupport.testTags.MultiMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.time.TramTime.of;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@MultiMode
public class StationAvailabilityRepositoryTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private StationAvailabilityRepository availabilityRepository;
    private StationRepository stationRepository;
    private TramDate when;
    private ClosedStationsRepository closedStationRepository;
    private ImmutableEnumSet<TransportMode> modes;
    private TramRouteHelper tramRouteHelper;

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
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        availabilityRepository = componentContainer.get(StationAvailabilityRepository.class);
        closedStationRepository = componentContainer.get(ClosedStationsRepository.class);

        tramRouteHelper = new TramRouteHelper(componentContainer);

        when = TestEnv.testDay();
        modes = TramsOnly;
    }

    @Test
    void shouldBeAvailableAtExpectedHoursOld() {

        Station stPeters = StPetersSquare.from(stationRepository);

        boolean duringTheDay = availabilityRepository.isAvailable(stPeters, when,
                TimeRangePartial.of(of(8,45), of(10,45)), modes);

        assertTrue(duringTheDay);

        boolean lateAtNight = availabilityRepository.isAvailable(stPeters, when,
                TimeRangePartial.of(of(3,5), of(3,15)), modes);

        assertFalse(lateAtNight);
    }

    @Test
    void shouldGetAvailableTimeRangeForDate() {
        Station stPeters = StPetersSquare.from(stationRepository);
        LocationCollection destinations = LocationCollectionSingleton.of(stPeters);
        TimeRange result = availabilityRepository.getAvailableTimesFor(destinations, when);

        assertTrue(result.contains(TramTime.of(8,0)));
        assertFalse(result.contains(TramTime.of(3,0)));

        assertEquals(TimeRangePartial.of(TramTime.of(5,12), TramTime.nextDay(0,48)), result);
    }

    @Test
    void shouldGetDifferentTimeRangesForDifferentDays() {
        Station station = Altrincham.from(stationRepository);
        LocationCollection destinations = LocationCollectionSingleton.of(station);

        TramDate monday = TestEnv.nextMonday();
        TramDate friday = monday.plusDays(4);
        assertEquals(DayOfWeek.FRIDAY, friday.getDayOfWeek());

        TimeRange mondayTimes = availabilityRepository.getAvailableTimesFor(destinations, monday);
        TimeRange fridayTimes = availabilityRepository.getAvailableTimesFor(destinations, friday);

        assertNotEquals(mondayTimes, fridayTimes);
    }

    @Test
    void shouldHaveExpectedServedRoutesForLocation() {
        Station station = Altrincham.from(stationRepository);

        ServedRoute dropOffs = availabilityRepository.getServedPickUpRouteFor(station.getLocationId());

        TramDate monday = TestEnv.nextMonday();

        // late services from Alty run to Old Trafford
        TimeRange timeRange = TimeRange.of(TramTime.of(0,16), TramTime.of(0,30));
        Set<Route> tooLate = dropOffs.getRoutes(monday, timeRange, modes);
        assertTrue(tooLate.isEmpty(), tooLate.toString());
    }

    @Test
    void shouldNotGetUnexpectedLateNightService() {
        Station station = Altrincham.from(stationRepository);
        LocationCollection destinations = LocationCollectionSingleton.of(station);

        TramDate monday = TestEnv.nextMonday();

        TimeRange mondayTimes = availabilityRepository.getAvailableTimesFor(destinations, monday);

        //  late services from Alty run to Old Trafford


        assertTrue(mondayTimes.contains(TramTime.of(23,55)), "Unexpected time range " + mondayTimes);
        assertFalse(mondayTimes.contains(TramTime.of(0,30)), "Unexpected time range " + mondayTimes);

    }

    @Test
    void shouldNotBeAvailableIfModesWrong() {

        Station stPeters = StPetersSquare.from(stationRepository);

        ImmutableEnumSet<TransportMode> otherModes = ImmutableEnumSet.of(TransportMode.Ferry);
        boolean duringTheDay = availabilityRepository.isAvailable(stPeters, when,
                TimeRangePartial.of(of(8,45), of(10,45)), otherModes);

        assertFalse(duringTheDay);
    }

    @DataExpiryTest
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRangesOverMidnight() {
        Station altrincham = Altrincham.from(stationRepository);

        long maxDuration = config.getMaxJourneyDuration();

        TimeRange timeRange = TimeRangePartial.of(TramTime.of(22, 50), TramDuration.ZERO, TramDuration.ofMinutes(maxDuration));
        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRange, modes);
        assertFalse(results.isEmpty(), "for " + timeRange + " missing routes from " + altrincham);

        TimeRange timeRangeCrossMidnight = TimeRangePartial.of(TramTime.of(23, 59), TramDuration.ZERO, TramDuration.ofMinutes(maxDuration));

        Set<Route> pickupRoutes = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRangeCrossMidnight, modes);

        // trams run Alty to Old Trafford
        assertFalse(pickupRoutes.isEmpty(), "for " + timeRangeCrossMidnight + " missing routes over mid-night from " + altrincham);

        Set<Route> dropoffRoutes = availabilityRepository.getDropoffRoutesFor(altrincham, when, timeRangeCrossMidnight, modes);
        assertFalse(dropoffRoutes.isEmpty(), "for " + timeRangeCrossMidnight + " missing routes over mid-night from " + altrincham);

    }

    @DataExpiryTest
    @Test
    void shouldHaveCorrectLateNightServicesAtEndOfLine() {
        Station altrincham = Altrincham.from(stationRepository);

        TripRepository tripRepos = componentContainer.get(TripRepository.class);
        Set<Trip> trips = tripRepos.getTripsCallingAt(altrincham, when);

        Set<StopCall> stopCalls = trips.stream().flatMap(trip -> trip.getStopCalls().stream()).
                filter(StopCall::callsAtStation).
                filter(stopCall -> stopCall.getStation().equals(altrincham)).
                collect(Collectors.toSet());

        Optional<TramTime> findLatestArrival = stopCalls.stream().map(StopCall::getArrivalTime).max(TramTime::compareTo);
        assertTrue(findLatestArrival.isPresent());
        TramTime latestArrival = findLatestArrival.get();

        Optional<TramTime> findLatestDepart = stopCalls.stream().map(StopCall::getDepartureTime).max(TramTime::compareTo);
        assertTrue(findLatestDepart.isPresent());
        TramTime latestDepart = findLatestDepart.get();

        TimeRange timeRange = TimeRange.of(latestArrival, latestDepart);
        boolean result = availabilityRepository.isAvailable(altrincham, when, timeRange, modes);

        assertTrue(result, "missing late night services");
    }

    @DataExpiryTest
    @Test
    void shouldNotHaveLateNightServicesAtEndOfLine() {
        Station altrincham = Altrincham.from(stationRepository);

        TimeRange timeRange = TimeRange.of(TramTime.nextDay(0,40), TramTime.nextDay(0,45));
        boolean result = availabilityRepository.isAvailable(altrincham, when, timeRange, modes);

        assertFalse(result, "unexpected late night services");
    }

    @DataExpiryTest
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRangesAfterMidnight() {
        Station altrincham = Altrincham.from(stationRepository);

        long maxDuration = config.getMaxJourneyDuration();

        TimeRange timeRangeATMidnight = TimeRangePartial.of(TramTime.of(0, 0), TramDuration.ZERO, TramDuration.ofMinutes(maxDuration));
        Set<Route> atMidnightResults = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRangeATMidnight, modes);
        assertFalse(atMidnightResults.isEmpty(), "for " + timeRangeATMidnight + " missing routes over mid-night from " + altrincham.getId());
    }

    @DataExpiryTest
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRanges() {

        // earlier to diagnose using end of line station
        Station altrincham = Altrincham.from(stationRepository);

        TimeRange timeRange = TimeRangePartial.of(TramTime.of(12, 50), TramDuration.ofHours(4), TramDuration.ofHours(4));

        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRange, modes);

        assertEquals(2, results.size(),
                timeRange + " missing routes from " + altrincham.getId() + " got " + HasId.asIds(results));
    }

    @DataExpiryTest
    @Test
    void shouldHaveServicesAvailableAtExpectedLateTimeRangeNDaysAhead() {
        TramTime latestHour = TramTime.of(23,0);

        TramDuration maxwait = TramDuration.ofMinutes(config.getMaxWait());

        UpcomingDates.getUpcomingDates().forEach(date -> {

            TimeRange lateRange = TimeRangePartial.of(latestHour, maxwait, maxwait);
            Set<Station> notAvailableLate = stationRepository.getStations().stream().
                    filter(station -> !UpcomingDates.hasClosure(station, date)).
                    filter(Location::isActive).
                    filter(station -> station.getTransportModes().contains(Tram)).
                    filter(station -> !closedStationRepository.isClosed(station, date)).
                    filter(station -> !availabilityRepository.isAvailable(station, date, lateRange, modes)).
                    collect(Collectors.toSet());

            assertTrue(notAvailableLate.isEmpty(), "Not available " + date + " " + lateRange + " " + HasId.asIds(notAvailableLate));

        });
    }

    @DataExpiryTest
    @Test
    void shouldHaveServicesAvailableAtExpectedEarlyTimeRangeNDaysAhead() {
        TramTime earlistHour = TramTime.of(7,0);

        TramDuration maxwait = TramDuration.ofMinutes(config.getMaxWait());

        final TimeRange earlyRange = TimeRangePartial.of(earlistHour, maxwait, maxwait);

        UpcomingDates.getUpcomingDates().forEach(date -> {

            Set<Station> shouldBeAvailable =  stationRepository.getStations(TramsOnly).stream().
                    filter(Location::isActive).
                    filter(station -> !UpcomingDates.hasClosure(station, date, earlyRange)).
                    filter(station -> !closedStationRepository.isClosed(station, date)).
                    collect(Collectors.toSet());

            if (!shouldBeAvailable.isEmpty()) {
                Set<Station> notAvailableEarly = shouldBeAvailable.stream().
                        filter(station -> !availabilityRepository.isAvailable(station, date, earlyRange, modes)).
                        collect(Collectors.toSet());

                assertTrue(notAvailableEarly.isEmpty(), "Not available " + date + " " + earlyRange + " " + HasId.asIds(notAvailableEarly));
            }
        });
    }

    @Test
    void shouldHaveExpectedDropOffRoutesForVictoriaTram() {
        TramDate date = TestEnv.testDay();
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 45), TramTime.of(16, 45));

        Station victoria = Victoria.from(stationRepository);
        Set<Route> dropOffs = availabilityRepository.getDropoffRoutesFor(victoria, date, timeRange, TramsOnly);

        Route yellowInbound = tramRouteHelper.getYellow(when);
        Route blueInbound = tramRouteHelper.getPink(when);
        Route greenOutbound = tramRouteHelper.getGreen(when);

        assertEquals(5, dropOffs.size());
        assertTrue(dropOffs.contains(yellowInbound));
        assertTrue(dropOffs.contains(blueInbound));

        assertTrue(dropOffs.contains(greenOutbound));

        Set<Route> pickups = availabilityRepository.getPickupRoutesFor(victoria, date, timeRange, TramsOnly);

        assertEquals(5, pickups.size());
        assertTrue(pickups.contains(yellowInbound));
        assertTrue(pickups.contains(blueInbound));
        assertTrue(pickups.contains(greenOutbound));

    }

}
