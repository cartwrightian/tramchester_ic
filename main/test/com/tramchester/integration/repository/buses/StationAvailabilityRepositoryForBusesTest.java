package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import com.tramchester.testSupport.testTags.DataExpiryCategory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.time.TramTime.of;
import static com.tramchester.testSupport.reference.BusStations.Composites.ShudehillInterchange;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
public class StationAvailabilityRepositoryForBusesTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private StationAvailabilityRepository availabilityRepository;
    private StationRepository stationRepository;
    private StationGroupsRepository stationGroupsRepository;
    private TramDate when;
    private ClosedStationsRepository closedStationRepository;
    private EnumSet<TransportMode> modes;

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
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        availabilityRepository = componentContainer.get(StationAvailabilityRepository.class);
        closedStationRepository = componentContainer.get(ClosedStationsRepository.class);
        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);

        when = TestEnv.testDay();
        modes = TestEnv.Modes.BusesOnly;
    }

    @Test
    void shouldBeAvailableAtExpectedHoursOld() {

        Station altrinchamStop = BusStations.StopAtAltrinchamInterchange.from(stationRepository);

        boolean duringTheDay = availabilityRepository.isAvailable(altrinchamStop, when, TimeRange.of(of(8,45), of(10,45)), modes);

        assertTrue(duringTheDay);

        boolean lateAtNight = availabilityRepository.isAvailable(altrinchamStop, when, TimeRange.of(of(3,5), of(3,15)), modes);

        assertFalse(lateAtNight);
    }

    @Test
    void shouldNotBeAvailableIfModesWrong() {

        Station stPeters = BusStations.StopAtAltrinchamInterchange.from(stationRepository);

        EnumSet<TransportMode> otherModes = EnumSet.of(TransportMode.Ferry);
        boolean duringTheDay = availabilityRepository.isAvailable(stPeters, when, TimeRange.of(of(8,45), of(10,45)), otherModes);

        assertFalse(duringTheDay);

    }

    @Test
    void shouldCheckStationGroupsAsExpected() {
        StationGroup stationGroup = stationGroupsRepository.findByName(BusStations.Composites.AltrinchamInterchange.getName());
        assertNotNull(stationGroup);

        boolean duringTheDay = availabilityRepository.isAvailable(stationGroup, when, TimeRange.of(of(8,45), of(10,45)), modes);

        assertTrue(duringTheDay);

    }

    @DataExpiryCategory
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRangesOverMidnight() {
        StationGroup shudeHillInterchange = stationGroupsRepository.findByName(ShudehillInterchange.getName());

        long maxDuration = config.getMaxJourneyDuration();

        TimeRange timeRange = TimeRange.of(TramTime.of(22, 50), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> pickupResults = availabilityRepository.getPickupRoutesFor(shudeHillInterchange, when, timeRange, modes);
        assertFalse(pickupResults.isEmpty(), "for " + timeRange + " missing pickup routes from " + shudeHillInterchange);

        Set<Route> dropOffResults = availabilityRepository.getDropoffRoutesFor(shudeHillInterchange, when, timeRange, modes);
        assertFalse(dropOffResults.isEmpty(), "for " + timeRange + " missing dropoff routes from " + shudeHillInterchange);

        TimeRange timeRangeCrossMidnight = TimeRange.of(TramTime.of(23, 59), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> overMidnightPickupResults = availabilityRepository.getPickupRoutesFor(shudeHillInterchange, when, timeRangeCrossMidnight, modes);
        assertFalse(overMidnightPickupResults.isEmpty(), "for " + timeRangeCrossMidnight + " missing pickup routes over mid-night from " + shudeHillInterchange);

        Set<Route> overMidnightDropoffResults = availabilityRepository.getDropoffRoutesFor(shudeHillInterchange, when, timeRangeCrossMidnight, modes);
        assertFalse(overMidnightDropoffResults.isEmpty(), "for " + timeRangeCrossMidnight + " missing pickup routes over mid-night from " + shudeHillInterchange);

    }

    @DataExpiryCategory
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRangesAfterMidnight() {
        StationGroup shudeHillInterchange = stationGroupsRepository.findByName(ShudehillInterchange.getName());

        long maxDuration = config.getMaxJourneyDuration();

        TimeRange timeRangeATMidnight = TimeRange.of(TramTime.of(0, 0), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> atMidnightResults = availabilityRepository.getPickupRoutesFor(shudeHillInterchange, when, timeRangeATMidnight, modes);
        assertFalse(atMidnightResults.isEmpty(), "for " + timeRangeATMidnight + " missing routes over mid-night from " + shudeHillInterchange.getId());
    }

    @DataExpiryCategory
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRanges() {

        // earier to diagnose using end of line station
        Station station = BusStations.StopAtShudehillInterchange.from(stationRepository);

        TimeRange timeRange = TimeRange.of(TramTime.of(12, 50), Duration.ofHours(4), Duration.ofHours(4));

        Set<Route> results = availabilityRepository.getPickupRoutesFor(station, when, timeRange, modes);

        assertEquals(3, results.size(),
                timeRange + " missing routes from " + station.getId() + " got " + HasId.asIds(results));
    }

    @DataExpiryCategory
    @Test
    void shouldHaveServicesAvailableAtExpectedLateTimeRangeNDaysAhead() {
        TramTime latestHour = TramTime.of(23,0);

        Duration maxwait = Duration.ofMinutes(config.getMaxWait());

        TestEnv.getUpcomingDates().forEach(date -> {

            TimeRange lateRange = TimeRange.of(latestHour, maxwait, maxwait);
            Set<Station> notAvailableLate = stationRepository.getStations().stream().
                    filter(Location::isActive).
                    filter(station -> station.getTransportModes().contains(Tram)).
                    filter(station -> !closedStationRepository.isClosed(station, date)).
                    filter(station -> !availabilityRepository.isAvailable(station, date, lateRange, modes)).
                    collect(Collectors.toSet());

            assertTrue(notAvailableLate.isEmpty(), "Not available " + date + " " + lateRange + " " + HasId.asIds(notAvailableLate));

        });
    }

    @DataExpiryCategory
    @Test
    void shouldHaveServicesAvailableAtExpectedEarlyTimeRangeNDaysAhead() {
        TramTime earlistHour = TramTime.of(7,0);

        Duration maxwait = Duration.ofMinutes(config.getMaxWait());

        TestEnv.getUpcomingDates().forEach(date -> {

            TimeRange earlyRange = TimeRange.of(earlistHour, maxwait, maxwait);
            Set<Station> notAvailableEarly = stationRepository.getStations().stream().
                    filter(Location::isActive).
                    filter(station -> station.getTransportModes().contains(Tram)).
                    filter(station -> !closedStationRepository.isClosed(station, date)).
                    filter(station -> !availabilityRepository.isAvailable(station, date, earlyRange, modes)).
                    collect(Collectors.toSet());

            assertTrue(notAvailableEarly.isEmpty(), "Not available " + date + " " + earlyRange + " " + HasId.asIds(notAvailableEarly));
        });
    }

}
