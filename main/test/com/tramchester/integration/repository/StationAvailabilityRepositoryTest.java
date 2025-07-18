package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationCollectionSingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.testTags.DataExpiryTest;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.time.TramTime.of;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
public class StationAvailabilityRepositoryTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private StationAvailabilityRepository availabilityRepository;
    private StationRepository stationRepository;
    private TramDate when;
    private ClosedStationsRepository closedStationRepository;
    private EnumSet<TransportMode> modes;
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

        assertEquals(TimeRangePartial.of(TramTime.of(5,13), TramTime.nextDay(0,42)), result);
    }

    @Test
    void shouldNotBeAvailableIfModesWrong() {

        Station stPeters = StPetersSquare.from(stationRepository);

        EnumSet<TransportMode> otherModes = EnumSet.of(TransportMode.Ferry);
        boolean duringTheDay = availabilityRepository.isAvailable(stPeters, when,
                TimeRangePartial.of(of(8,45), of(10,45)), otherModes);

        assertFalse(duringTheDay);
    }

    @DataExpiryTest
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRangesOverMidnight() {
        Station altrincham = Altrincham.from(stationRepository);

        long maxDuration = config.getMaxJourneyDuration();

        TimeRange timeRange = TimeRangePartial.of(TramTime.of(22, 50), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRange, modes);
        assertFalse(results.isEmpty(), "for " + timeRange + " missing routes from " + altrincham);

        TimeRange timeRangeCrossMidnight = TimeRangePartial.of(TramTime.of(23, 59), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> overMidnightResults = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRangeCrossMidnight, modes);
        assertFalse(overMidnightResults.isEmpty(), "for " + timeRangeCrossMidnight + " missing routes over mid-night from " + altrincham);

    }

    @DataExpiryTest
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRangesAfterMidnight() {
        Station altrincham = Altrincham.from(stationRepository);

        long maxDuration = config.getMaxJourneyDuration();

        TimeRange timeRangeATMidnight = TimeRangePartial.of(TramTime.of(0, 0), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> atMidnightResults = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRangeATMidnight, modes);
        assertFalse(atMidnightResults.isEmpty(), "for " + timeRangeATMidnight + " missing routes over mid-night from " + altrincham.getId());
    }

    @DataExpiryTest
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRanges() {

        // earlier to diagnose using end of line station
        Station altrincham = Altrincham.from(stationRepository);

        TimeRange timeRange = TimeRangePartial.of(TramTime.of(12, 50), Duration.ofHours(4), Duration.ofHours(4));

        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRange, modes);

        // 2->1 picc gardens closure 2025
        assertEquals(1, results.size(),
                timeRange + " missing routes from " + altrincham.getId() + " got " + HasId.asIds(results));
    }

    @DataExpiryTest
    @Test
    void shouldHaveServicesAvailableAtExpectedLateTimeRangeNDaysAhead() {
        TramTime latestHour = TramTime.of(23,0);

        Duration maxwait = Duration.ofMinutes(config.getMaxWait());

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

        Duration maxwait = Duration.ofMinutes(config.getMaxWait());

        final TimeRange earlyRange = TimeRangePartial.of(earlistHour, maxwait, maxwait);

        UpcomingDates.getUpcomingDates().forEach(date -> {

            Set<Station> notAvailableEarly = stationRepository.getStations().stream().
                    filter(station -> !UpcomingDates.hasClosure(station, date, earlyRange)).
                    filter(Location::isActive).
                    filter(station -> station.getTransportModes().contains(Tram)).
                    filter(station -> !closedStationRepository.isClosed(station, date)).
                    filter(station -> !availabilityRepository.isAvailable(station, date, earlyRange, modes)).
                    collect(Collectors.toSet());

            assertTrue(notAvailableEarly.isEmpty(), "Not available " + date + " " + earlyRange + " " + HasId.asIds(notAvailableEarly));
        });
    }

    @Test
    void shouldHaveExpectedDropOffRoutesForVictoriaTram() {
        TramDate date = TestEnv.testDay();
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 45), TramTime.of(16, 45));

        Station victoria = Victoria.from(stationRepository);
        Set<Route> dropOffs = availabilityRepository.getDropoffRoutesFor(victoria, date, timeRange, TramsOnly);

        // Picc gardens closure
        //Route yellowInbound = tramRouteHelper.getYellow(when);
        Route blueInbound = tramRouteHelper.getPink(when);
        Route greenOutbound = tramRouteHelper.getGreen(when);

        assertEquals(4, dropOffs.size());
        //assertTrue(dropOffs.contains(yellowInbound));
        assertTrue(dropOffs.contains(blueInbound));
        assertTrue(dropOffs.contains(greenOutbound));

        Set<Route> pickups = availabilityRepository.getPickupRoutesFor(victoria, date, timeRange, TramsOnly);

        assertEquals(4, pickups.size());
        //assertTrue(pickups.contains(yellowInbound));
        assertTrue(pickups.contains(blueInbound));
        assertTrue(pickups.contains(greenOutbound));

    }

}
