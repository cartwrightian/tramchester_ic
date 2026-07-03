package com.tramchester.unit.domain;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.ServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.ServiceRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RunningRoutesAndServicesTest extends EasyMockSupport {

    public static final String SERVICE_A_ID = "serviceAId";
    public static final String SERVICE_B_ID = "serviceBId";
    public static final String ROUTE_A_ID = "routeAId";
    public static final String ROUTE_B_ID = "routeBId";
    private TramDate date;
    private RunningRoutesAndServices runningRoutesAndServices;
    private Route routeA;
    private Route routeB;
    private Service serviceA;
    private Service serviceB;
    private TramDate previousDate;
    private TramDate nextDay;
    private ImmutableEnumSet<TransportMode> modes;
    private ServiceCalendar calendarA;
    private ServiceCalendar calendarB;
    private IdFor<Service> serviceAId;
    private IdFor<Service> serviceBId;
    private IdFor<Route> routeAId;
    private IdFor<Route> routeBId;

    @BeforeEach
    void beforeEachTestRuns() {
        date = TramDate.of(2021, 12, 5);

        modes = TransportMode.TramsOnly;

        previousDate = date.minusDays(1);
        nextDay = date.plusDays(1);

        ServiceRepository serviceRepository = createMock(ServiceRepository.class);
        RouteRepository routeRepository = createMock(RouteRepository.class);

        serviceA = createMock(Service.class);
        serviceB = createMock(Service.class);

        serviceAId = Service.createId(SERVICE_A_ID);
        serviceBId = Service.createId(SERVICE_B_ID);

        EasyMock.expect(serviceA.getId()).andStubReturn(serviceAId);
        EasyMock.expect(serviceB.getId()).andStubReturn(serviceBId);

        calendarA = createMock(ServiceCalendar.class);
        EasyMock.expect(serviceA.getCalendar()).andStubReturn(calendarA);

        calendarB = createMock(ServiceCalendar.class);
        EasyMock.expect(serviceB.getCalendar()).andStubReturn(calendarB);

        routeA = createMock(Route.class);
        routeB = createMock(Route.class);

        routeAId = Route.createBasicRouteId(ROUTE_A_ID);
        routeBId = Route.createBasicRouteId(ROUTE_B_ID);

        EasyMock.expect(routeA.getId()).andStubReturn(routeAId);
        EasyMock.expect(routeB.getId()).andStubReturn(routeBId);

        EasyMock.expect(routeRepository.getRoutes(modes)).andReturn(new HashSet<>(Arrays.asList(routeA, routeB)));
        EasyMock.expect(serviceRepository.getServices(modes)).andReturn(new HashSet<>(Arrays.asList(serviceA, serviceB)));

        runningRoutesAndServices = new RunningRoutesAndServices(serviceRepository, routeRepository);
    }

    @Test
    void shouldHaveRunningForDate() {

        setServiceCalendarExpectations(calendarA, false, true, false);
        setServiceCalendarExpectations(calendarB, false, false, true);

        setRouteAvailableExpectations(routeA, false, true, false);
        setRouteAvailableExpectations(routeB, false, false, false);

        replayAll();
        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(date, modes);
        verifyAll();

        TramTime time = TramTime.of(10,45);
        assertTrue(filter.isRouteRunning(routeAId, false));
        assertFalse(filter.isRouteRunning(routeBId, false));

        assertTrue(filter.isServiceRunningByDate(serviceAId, time.isNextDay()));
        assertFalse(filter.isServiceRunningByDate(serviceBId, time.isNextDay()));
    }

    @Test
    void shouldHaveRunningForTimeSameDay() {

        setServiceCalendarExpectations(calendarA, false, true, false);

        setServiceCalendarExpectations(calendarB, false, true, false);

        // Service A begin: 9.30 end: 12:30
        EasyMock.expect(serviceA.getStartTime()).andStubReturn(TramTime.of(9,30));
        EasyMock.expect(serviceA.getFinishTime()).andStubReturn(TramTime.of(12,30));

        // Service B begin: 23.45 end: 1.15 , runs from today over midnight into next day
        EasyMock.expect(serviceB.getStartTime()).andStubReturn(TramTime.of(23,45));
        EasyMock.expect(serviceB.getFinishTime()).andStubReturn(TramTime.nextDay(1,15));

        setRouteAvailableExpectations(routeA, false, false, false);
        setRouteAvailableExpectations(routeB, false, false, false);

        replayAll();
        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(date, modes);
        verifyAll();

        int maxWait = 0;
        assertTrue(filter.isServiceRunningByTime(serviceAId, TramTime.of(10,45), maxWait));
        assertFalse(filter.isServiceRunningByTime(serviceAId, TramTime.of(12,45), maxWait));
        assertFalse(filter.isServiceRunningByTime(serviceAId, TramTime.of(8,45), maxWait));

        assertTrue(filter.isServiceRunningByTime(serviceBId, TramTime.of(23,50), maxWait));
        assertTrue(filter.isServiceRunningByTime(serviceBId, TramTime.nextDay(0,10), maxWait));

        assertFalse(filter.isServiceRunningByTime(serviceBId, TramTime.of(0,10), maxWait));
        assertFalse(filter.isServiceRunningByTime(serviceBId, TramTime.nextDay(23,50), maxWait));
        assertFalse(filter.isServiceRunningByTime(serviceBId, TramTime.of(10,45), maxWait));
    }

    @Test
    void shouldHaveRunningForTomorrow() {
        setServiceCalendarExpectations(calendarA, false, true, false);
        setServiceCalendarExpectations(calendarB, false, false, true);

        // Service A begin: 9.30 end: 12:30, today
        EasyMock.expect(serviceA.getStartTime()).andStubReturn(TramTime.of(9,30));
        EasyMock.expect(serviceA.getFinishTime()).andStubReturn(TramTime.of(12,30));

        // Service B begin: 00.30 end: 4.15, but on the following day
        EasyMock.expect(serviceB.getStartTime()).andStubReturn(TramTime.of(0,30));
        EasyMock.expect(serviceB.getFinishTime()).andStubReturn(TramTime.of(4,15));

        setRouteAvailableExpectations(routeA, false, false, false);
        setRouteAvailableExpectations(routeB, false, false, false);

        replayAll();
        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(date, modes);
        verifyAll();

        int maxWait = 1;
        assertTrue(filter.isServiceRunningByTime(serviceAId, TramTime.of(10,45), maxWait));
        assertFalse(filter.isServiceRunningByTime(serviceAId, TramTime.of(12,45), maxWait));

        assertTrue(filter.isServiceRunningByTime(serviceAId, TramTime.of(9,25), 11));
        assertFalse(filter.isServiceRunningByTime(serviceAId, TramTime.of(12,40), 11));

        assertFalse(filter.isServiceRunningByTime(serviceAId, TramTime.of(8,45), maxWait));

        assertFalse(filter.isServiceRunningByTime(serviceBId, TramTime.of(23,50), maxWait));
        assertTrue(filter.isServiceRunningByTime(serviceBId, TramTime.nextDay(0,35), maxWait));

        assertFalse(filter.isServiceRunningByTime(serviceBId, TramTime.of(0,35), maxWait));
        assertTrue(filter.isServiceRunningByTime(serviceBId, TramTime.nextDay(4,0), maxWait));
        assertFalse(filter.isServiceRunningByTime(serviceBId, TramTime.nextDay(4,16), maxWait));
    }

    @Test
    void shouldHaveRunningForPreviousDay() {
        setServiceCalendarExpectations(calendarA, true, false, false);
        setServiceCalendarExpectations(calendarB, false, false, false);

        setRouteAvailableExpectations(routeA, false, false, false);
        setRouteAvailableExpectations(routeB, false, false, false);

        // Service A begin: 23.25 end: 00:30, starts on previous Day
        EasyMock.expect(serviceA.getStartTime()).andStubReturn(TramTime.of(23,25));
        EasyMock.expect(serviceA.getFinishTime()).andStubReturn(TramTime.nextDay(0,30));
        EasyMock.expect(serviceA.intoNextDay()).andReturn(true);

        replayAll();
        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(date, modes);
        verifyAll();

        int maxWait = 1;
        assertFalse(filter.isServiceRunningByTime(serviceAId, TramTime.of(23,25), maxWait));

        assertTrue(filter.isServiceRunningByTime(serviceAId, TramTime.of(0,30), maxWait));
    }

    @Test
    void shouldIncludeFollowingDayIfTramTimeIsNextDay() {

        setServiceCalendarExpectations(calendarA, false, true, false);
        setServiceCalendarExpectations(calendarB, false, false, true);

        setRouteAvailableExpectations(routeA, false, true, false);
        setRouteAvailableExpectations(routeB, false, false, true);

        replayAll();
        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(date, modes);
        verifyAll();

        assertTrue(filter.isRouteRunning(Route.createBasicRouteId("routeAId"), false));
        assertTrue(filter.isServiceRunningByDate(serviceAId, false));

        assertFalse(filter.isRouteRunning(Route.createBasicRouteId("routeBId"), false));
        assertFalse(filter.isServiceRunningByDate(serviceBId, false));

        assertTrue(filter.isRouteRunning(Route.createBasicRouteId("routeAId"), true));
        assertTrue(filter.isServiceRunningByDate(serviceAId, true));

        assertTrue(filter.isRouteRunning(Route.createBasicRouteId("routeBId"), true));
        assertTrue(filter.isServiceRunningByDate(serviceBId, true));
    }

    private void setRouteAvailableExpectations(Route route, boolean yesterday, boolean today, boolean tomorrow) {
        EasyMock.expect(route.isAvailableOn(previousDate)).andStubReturn(yesterday);
        EasyMock.expect(route.isAvailableOn(date)).andStubReturn(today);
        EasyMock.expect(route.isAvailableOn(nextDay)).andStubReturn(tomorrow);
    }

    private void setServiceCalendarExpectations(ServiceCalendar serviceCalendar, boolean yeserday, boolean today, boolean tomorrow) {
        EasyMock.expect(serviceCalendar.operatesOn(date)).andStubReturn(today);
        EasyMock.expect(serviceCalendar.operatesOn(nextDay)).andStubReturn(tomorrow);
        EasyMock.expect(serviceCalendar.operatesOn(previousDate)).andStubReturn(yeserday);
    }

}
