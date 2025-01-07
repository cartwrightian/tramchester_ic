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
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.testTags.BusTest;
import com.tramchester.testSupport.testTags.DataExpiryTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.time.TramTime.of;
import static com.tramchester.testSupport.TestEnv.Modes.BusesOnly;
import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BusTest
public class StationAvailabilityRepositoryBusTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private StationAvailabilityRepository availabilityRepository;
    private StationRepository stationRepository;
    private TramDate when;
    private ClosedStationsRepository closedStationRepository;
    private EnumSet<TransportMode> modes;
    private StationGroupsRepository stationGroupRepository;
    private TimeRange morningRange;
    private TimeRange eveningRange;

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

        stationGroupRepository = componentContainer.get(StationGroupsRepository.class);

        when = TestEnv.testDay();
        modes = BusesOnly;
        morningRange = TimeRangePartial.of(of(8, 45), of(10, 45));
        eveningRange = TimeRangePartial.of(of(20, 53), of(23, 53));
    }

    @Test
    void shouldBeAvailableAtExpectedHoursOld() {

        StationGroup stockport = KnownLocality.Stockport.from(stationGroupRepository);

        boolean duringTheDay = availabilityRepository.isAvailable(stockport, when, morningRange, modes);
        assertTrue(duringTheDay);

        boolean evening = availabilityRepository.isAvailable(stockport, when, eveningRange, modes);
        assertTrue(evening);

        boolean lateAtNight = availabilityRepository.isAvailable(stockport, when, TimeRangePartial.of(of(3,5), of(3,15)), modes);
        assertFalse(lateAtNight);
    }

    @Test
    void shouldBeAvailableAtMaccExpectedHours() {

        StationGroup macclesfield = KnownLocality.Macclesfield.from(stationGroupRepository);

        boolean duringTheDay = availabilityRepository.isAvailable(macclesfield, when, morningRange, modes);

        assertTrue(duringTheDay);

        // there really are no buses in the evening!
        boolean evening = availabilityRepository.isAvailable(macclesfield, when, eveningRange, modes);
        assertFalse(evening);
    }

    // WARN  [2024-01-29 20:53:54,656] com.tramchester.graph.search.routes.RouteToRouteCosts:
    // destination station Id{'StationGroup:N0076467'} has no matching drop-off routes for TramDate{epochDays=19751, dayOfWeek=MONDAY, date=2024-01-29}
    // TimeRange{begin=TramTime{h=20, m=53}, end=TramTime{h=23, m=53}} [Bus]

    @Test
    void shouldBeAvailableAtMaccExpectedDropOffsAndPickups() {

        StationGroup macclesfield = KnownLocality.Macclesfield.from(stationGroupRepository);

        Set<Route> pickups = availabilityRepository.getPickupRoutesFor(macclesfield, when, morningRange, modes);
        assertFalse(pickups.isEmpty());

        Set<Route> dropOffs = availabilityRepository.getDropoffRoutesFor(macclesfield, when, morningRange, modes);
        assertFalse(dropOffs.isEmpty());

    }

    @Test
    void shouldBeAvailableAtMaccExpectedDropOffsAndPickupsEvenings() {

        // there really are no buses in the evening!

        StationGroup macclesfield = KnownLocality.Macclesfield.from(stationGroupRepository);

        Set<Route> pickups = availabilityRepository.getPickupRoutesFor(macclesfield, when, eveningRange, modes);
        assertTrue(pickups.isEmpty());

        Set<Route> dropoffs = availabilityRepository.getDropoffRoutesFor(macclesfield, when, eveningRange, modes);
        assertTrue(dropoffs.isEmpty());

    }

    @Test
    void shouldHaveAvailabilityAtBollington() {

        StationGroup bollington = KnownLocality.Bollington.from(stationGroupRepository);

        TimeRange timeRange = TimeRangePartial.of(TramTime.of(2,0), TramTime.of(23,30));
        Set<Route> dropoffs = availabilityRepository.getDropoffRoutesFor(bollington, when, timeRange, modes);
        assertFalse(dropoffs.isEmpty());
    }


    @Disabled("to convert to bus")
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

    @Disabled("to convert to bus")
    @DataExpiryTest
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRangesAfterMidnight() {
        Station altrincham = Altrincham.from(stationRepository);

        long maxDuration = config.getMaxJourneyDuration();

        TimeRange timeRangeATMidnight = TimeRangePartial.of(TramTime.of(0, 0), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> atMidnightResults = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRangeATMidnight, modes);
        assertFalse(atMidnightResults.isEmpty(), "for " + timeRangeATMidnight + " missing routes over mid-night from " + altrincham.getId());
    }


    @Disabled("to convert to bus")
    @DataExpiryTest
    @Test
    void shouldHaveServicesAvailableAtExpectedLateTimeRangeNDaysAhead() {
        TramTime latestHour = TramTime.of(23,0);

        Duration maxwait = Duration.ofMinutes(config.getMaxWait());

        UpcomingDates.getUpcomingDates().forEach(date -> {

            TimeRange lateRange = TimeRangePartial.of(latestHour, maxwait, maxwait);
            Set<Station> notAvailableLate = stationRepository.getStations().stream().
                    filter(Location::isActive).
                    filter(station -> station.getTransportModes().contains(Tram)).
                    filter(station -> !closedStationRepository.isClosed(station, date)).
                    filter(station -> !availabilityRepository.isAvailable(station, date, lateRange, modes)).
                    collect(Collectors.toSet());

            assertTrue(notAvailableLate.isEmpty(), "Not available " + date + " " + lateRange + " " + HasId.asIds(notAvailableLate));

        });
    }

    @Disabled("to convert to bus")
    @DataExpiryTest
    @Test
    void shouldHaveServicesAvailableAtExpectedEarlyTimeRangeNDaysAhead() {
        TramTime earlistHour = TramTime.of(7,0);

        Duration maxwait = Duration.ofMinutes(config.getMaxWait());

        UpcomingDates.getUpcomingDates().forEach(date -> {

            TimeRange earlyRange = TimeRangePartial.of(earlistHour, maxwait, maxwait);
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
