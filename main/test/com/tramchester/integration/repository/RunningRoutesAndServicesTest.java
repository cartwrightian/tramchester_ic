package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.CrossesDay;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

public class RunningRoutesAndServicesTest {

    private static ComponentContainer componentContainer;
    private TransportData transportData;
    private RunningRoutesAndServices runningRoutesAndServices;
    private TramRouteHelper helper;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationTramTestConfig();
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
        runningRoutesAndServices = componentContainer.get(RunningRoutesAndServices.class);
        helper = new TramRouteHelper(transportData);
    }

    @Test
    void shouldHaveTripsAndRoutesThatCrossIntoNextDay() {

        Set<Trip> tripsIntoNextDay = transportData.getTrips().stream().filter(Trip::intoNextDay).collect(Collectors.toSet());

        assertFalse(tripsIntoNextDay.isEmpty());

        Set<Route> routesFromTrips = tripsIntoNextDay.stream().map(Trip::getRoute).collect(Collectors.toSet());
        assertFalse(routesFromTrips.isEmpty());

        Set<Route> routesIntoNextDay = transportData.getRoutes().stream().filter(Route::intoNextDay).collect(Collectors.toSet());
        assertEquals(routesFromTrips, routesIntoNextDay);
    }

    @Test
    void shouldHaveServicesThatCrossIntoNextDay() {
        assertTrue(transportData.getServices().stream().anyMatch(CrossesDay::intoNextDay));
    }

    @Test
    void shouldConsiderServicesFromDayBeforeIfTheyAreStillRunningTheFollowingDay() {

        TramDate when = TestEnv.avoidChristmasDate(TestEnv.nextSaturday().minusDays(1));

        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(when);

        // findFirst here means can be nondeterministic
        Optional<Route> findRoute = transportData.getRoutes().stream().
                filter(route -> route.getId().equals(KnownTramRoute.CornbrookTheTraffordCentre.getId())).
                filter(CrossesDay::intoNextDay).findFirst();
        assertTrue(findRoute.isPresent());

        Route route = findRoute.get();
        IdFor<Route> routeId = route.getId();

        assertTrue(filter.isRouteRunning(routeId, false));
        assertTrue(filter.isRouteRunning(routeId, true));

        Set<Service> services = route.getServices();

        TramDate previousDay = when.minusDays(1);

        Set<Service> allPreviousDay = services.stream().
                filter(service -> service.getCalendar().operatesOn(previousDay)).collect(Collectors.toSet());
        assertFalse(allPreviousDay.isEmpty());

        // actually checking for a service that crosses into following day
        Set<Service> fromPreviousDay = allPreviousDay.stream().
                filter(service -> filter.isServiceRunningByTime(service.getId(), TramTime.of(0, 0), 25)).
                collect(Collectors.toSet());

        assertFalse(fromPreviousDay.isEmpty(), "no services from previous day " + HasId.asIds(allPreviousDay));

    }

    @Test
    void shouldConsiderRoutesFromDayBeforeIfTheyAreStillRunningTheFollowingDay() {
        TramDate when = TestEnv.testDay().plusWeeks(1); // disruption week of 28/11/22

        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(when);

        TramDate previousDay = when.minusDays(1);

        Route altyToBuryRoute = helper.getOneRoute(KnownTramRoute.BuryManchesterAltrincham, previousDay);

        assertTrue(filter.isRouteRunning(altyToBuryRoute.getId(), false));
        assertTrue(filter.isRouteRunning(altyToBuryRoute.getId(), true));

    }

    @Test
    void shouldTakeAccountOfCrossingIntoNextDayForRunningServices() {
        // need to find service running mon to fri and one running saturday
        final EnumSet<DayOfWeek> weekdays = EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);
        final EnumSet<DayOfWeek> saturdays = EnumSet.of(SATURDAY);

        TramDate testDay = TestEnv.nextMonday();

        while (christmasWeek(testDay)) {
            testDay = testDay.plusWeeks(1);
        }

        final TramDate nextTuesday = testDay.plusDays(1);
        final TramDate friday = getFridayAfter(nextTuesday);

        assertEquals(TUESDAY, nextTuesday.getDayOfWeek(), "sanity check failed");

        // check for any intersection of days here since as of oct'23 no services actually operate Mon->Fri
        Set<Service> weekdayServices = transportData.getServices().stream().
                filter(service -> !SetUtils.intersection(service.getCalendar().getOperatingDays(), weekdays).isEmpty()).
                collect(Collectors.toSet());

        assertFalse(weekdayServices.isEmpty(), "no weekday services?");

        // date range contains next monday and service does weekday operating
        List<Service> matchingServices = weekdayServices.stream().
                filter(service -> service.getCalendar().getDateRange().contains(nextTuesday)).toList();

        assertFalse(matchingServices.isEmpty(), "found no weekday services for " + nextTuesday);

        // start of the range for services
        TramDate weekdayServicesBegin = matchingServices.stream().
                map(service -> service.getCalendar().getDateRange().getStartDate()).
                min(TramDate::compareTo).get();

        // end of the range for services
        TramDate weekdayServicesEnd = matchingServices.stream().
                map(service -> service.getCalendar().getDateRange().getEndDate()).
                max(TramDate::compareTo).get();

        // a range capturing all the dates for the weekday services
        DateRange weekdayDateRange = new DateRange(weekdayServicesBegin, weekdayServicesEnd);

        // double check contains range does contain next tuesday
        assertTrue(weekdayDateRange.contains(nextTuesday));
        assertTrue(weekdayDateRange.contains(friday));

        RunningRoutesAndServices.FilterForDate filterForNextFriday = runningRoutesAndServices.getFor(friday);

        Set<Service> weekdayFiltered = matchingServices.stream().
                filter(svc -> filterForNextFriday.isServiceRunningByDate(svc.getId(), false))
                .collect(Collectors.toSet());

        assertFalse(weekdayFiltered.isEmpty(), "Filter " + filterForNextFriday + " matched none of "
                + HasId.asIds(matchingServices) + " testday: " + testDay);

        // services operating on a Saturday within this range
        List<Service> saturdayServices = transportData.getServices().stream().
                filter(service -> service.getCalendar().getOperatingDays().equals(saturdays)).
                filter(service -> weekdayDateRange.overlapsWith(service.getCalendar().getDateRange())).
                //filter(service -> service.getCalendar().overlapsDatesWith(weekdayDateRange)).
                //filter(service -> service.getCalendar().overlapsDatesAndDaysWith(weekdayDateRange, saturdays)).
                collect(Collectors.toList());
        assertFalse(saturdayServices.isEmpty(), weekdayDateRange.toString());

        Set<Service> matchingForSaturday = saturdayServices.stream().
                filter(service -> filterForNextFriday.isServiceRunningByDate(service.getId(), true)).
                collect(Collectors.toSet());

        // Most likely reason for fail here is a route switch over on this exact date; this tests selects
        // services that are running on the test day, which would definitely not be running the next day if there
        // is a cut-over that same day
        assertFalse(matchingForSaturday.isEmpty(), "Filter:" + filterForNextFriday + " not matching any of " +HasId.asIds(saturdayServices));

    }

    private boolean christmasWeek(TramDate testDay) {
        if (testDay.isChristmasPeriod()) {
            return true;
        }
        TramDate remainingDay = testDay.plusDays(1);
        while (remainingDay.getDayOfWeek()!=MONDAY) {
            if (remainingDay.isChristmasPeriod()) {
                return true;
            }
            remainingDay = remainingDay.plusDays(1);
        }
        return false;
    }

    private TramDate getFridayAfter(final TramDate weekdayServicesBegin) {
        TramDate result = weekdayServicesBegin;
        while (result.getDayOfWeek()!=FRIDAY) {
            result = result.plusDays(1);
        }
        return result;
    }


}
