package com.tramchester.unit.domain;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RunningRoutesAndServicesTest extends EasyMockSupport {

    private TramDate date;
    private ServiceRepository serviceRepository;
    private RouteRepository routeRepository;
    private RunningRoutesAndServices runningRoutesAndServices;
    private Route routeA;
    private Route routeB;
    private Service serviceA;
    private Service serviceB;

    @BeforeEach
    void beforeEachTestRuns() {
        date = TramDate.of(2021, 12, 5);
        serviceRepository = createMock(ServiceRepository.class);
        routeRepository = createMock(RouteRepository.class);

        serviceA = createMock(Service.class);
        serviceB = createMock(Service.class);
        EasyMock.expect(serviceA.getId()).andStubReturn(Service.createId("serviceAId"));
        EasyMock.expect(serviceB.getId()).andStubReturn(Service.createId("serviceBId"));

        runningRoutesAndServices = new RunningRoutesAndServices(serviceRepository, routeRepository);

        routeA = TestEnv.getTramTestRoute(Route.createBasicRouteId("routeAId"), "route name a");
        routeB = TestEnv.getTramTestRoute(Route.createBasicRouteId("routeBId"), "route name b");
    }

    @Test
    void shouldHaveRunningForDate() {

        EnumSet<TransportMode> modes = EnumSet.of(TransportMode.Tram);

        EasyMock.expect(routeRepository.getRoutesRunningOn(date,modes)).andReturn(singleton(routeA));
        EasyMock.expect(serviceRepository.getServicesOnDate(date, modes)).andReturn(singleton(serviceA));

        TramDate tomorrow = date.plusDays(1);
        EasyMock.expect(routeRepository.getRoutesRunningOn(tomorrow,modes)).andReturn(singleton(routeB));
        EasyMock.expect(serviceRepository.getServicesOnDate(tomorrow, modes)).andReturn(singleton(serviceB));

        TramDate yesterday = date.minusDays(1);
        EasyMock.expect(routeRepository.getRoutesRunningOn(yesterday,modes)).andReturn(Collections.emptySet());
        EasyMock.expect(serviceRepository.getServicesOnDate(yesterday, modes)).andReturn(Collections.emptySet());

        replayAll();
        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(date,modes);
        verifyAll();

        TramTime time = TramTime.of(10,45);
        assertFalse(filter.isRouteRunning(Route.createBasicRouteId("routeBId"), false));
        assertTrue(filter.isRouteRunning(Route.createBasicRouteId("routeAId"), false));

        assertFalse(filter.isServiceRunningByDate(Service.createId("serviceBId"), time.isNextDay()));
        assertTrue(filter.isServiceRunningByDate(Service.createId("serviceAId"), time.isNextDay()));
    }

    @Test
    void shouldHaveRunningForTimeSameDay() {

        EnumSet<TransportMode> modes = EnumSet.of(TransportMode.Tram);

        final IdFor<Service> serviceAId = Service.createId("serviceAId");
        final IdFor<Service> serviceBId = Service.createId("serviceBId");

        EasyMock.expect(routeRepository.getRoutesRunningOn(date,modes)).andReturn(new HashSet<>(Arrays.asList(routeA, routeB)));
        EasyMock.expect(routeRepository.getRoutesRunningOn(date.plusDays(1), modes)).andReturn(Collections.emptySet());

        EasyMock.expect(serviceRepository.getServicesOnDate(date, modes)).andReturn(new HashSet<>(Arrays.asList(serviceA, serviceB)));
        EasyMock.expect(serviceRepository.getServicesOnDate(date.plusDays(1), modes)).andReturn(Collections.emptySet());

        TramDate yesterday = date.minusDays(1);
        EasyMock.expect(routeRepository.getRoutesRunningOn(yesterday, modes)).andReturn(Collections.emptySet());
        EasyMock.expect(serviceRepository.getServicesOnDate(yesterday, modes)).andReturn(Collections.emptySet());

        // Service A begin: 9.30 end: 12:30
        EasyMock.expect(serviceA.getStartTime()).andStubReturn(TramTime.of(9,30));
        EasyMock.expect(serviceA.getFinishTime()).andStubReturn(TramTime.of(12,30));

        // Service B begin: 23.45 end: 1.15 , runs from today over midnight into next day
        EasyMock.expect(serviceB.getStartTime()).andStubReturn(TramTime.of(23,45));
        EasyMock.expect(serviceB.getFinishTime()).andStubReturn(TramTime.nextDay(1,15));

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
        final IdFor<Service> serviceAId = Service.createId("serviceAId");
        final IdFor<Service> serviceBId = Service.createId("serviceBId");

        EnumSet<TransportMode> modes = EnumSet.of(TransportMode.Tram);

        EasyMock.expect(routeRepository.getRoutesRunningOn(date, modes)).andReturn(new HashSet<>(Arrays.asList(routeA, routeB)));
        EasyMock.expect(routeRepository.getRoutesRunningOn(date.plusDays(1), modes)).andReturn(Collections.emptySet());

        EasyMock.expect(serviceRepository.getServicesOnDate(date, modes)).andReturn(singleton(serviceA));
        EasyMock.expect(serviceRepository.getServicesOnDate(date.plusDays(1), modes)).andReturn(singleton(serviceB));

        TramDate yesterday = date.minusDays(1);
        EasyMock.expect(routeRepository.getRoutesRunningOn(yesterday, modes)).andReturn(Collections.emptySet());
        EasyMock.expect(serviceRepository.getServicesOnDate(yesterday, modes)).andReturn(Collections.emptySet());

        // Service A begin: 9.30 end: 12:30, today
        EasyMock.expect(serviceA.getStartTime()).andStubReturn(TramTime.of(9,30));
        EasyMock.expect(serviceA.getFinishTime()).andStubReturn(TramTime.of(12,30));

        // Service B begin: 00.30 end: 4.15, but on the following day
        EasyMock.expect(serviceB.getStartTime()).andStubReturn(TramTime.of(0,30));
        EasyMock.expect(serviceB.getFinishTime()).andStubReturn(TramTime.of(4,15));

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
        final IdFor<Service> serviceAId = Service.createId("serviceAId");

        EnumSet<TransportMode> modes = EnumSet.of(TransportMode.Tram);


        EasyMock.expect(routeRepository.getRoutesRunningOn(date, modes)).andReturn(Collections.emptySet());
        EasyMock.expect(serviceRepository.getServicesOnDate(date, modes)).andReturn(Collections.emptySet());

        TramDate tomorrow = this.date.plusDays(1);
        EasyMock.expect(routeRepository.getRoutesRunningOn(tomorrow, modes)).andReturn(Collections.emptySet());
        EasyMock.expect(serviceRepository.getServicesOnDate(tomorrow, modes)).andReturn(Collections.emptySet());

        TramDate yesterday = date.minusDays(1);
        EasyMock.expect(routeRepository.getRoutesRunningOn(yesterday, modes)).andReturn(singleton(routeA));
        EasyMock.expect(serviceRepository.getServicesOnDate(yesterday, modes)).andReturn(singleton(serviceA));

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

        EnumSet<TransportMode> modes = EnumSet.of(TransportMode.Tram);

        EasyMock.expect(routeRepository.getRoutesRunningOn(date, modes)).andReturn(singleton(routeA));
        EasyMock.expect(serviceRepository.getServicesOnDate(date, modes)).andReturn(singleton(serviceA));

        TramDate tomorrow = date.plusDays(1);
        EasyMock.expect(routeRepository.getRoutesRunningOn(tomorrow, modes)).andReturn(singleton(routeB));
        EasyMock.expect(serviceRepository.getServicesOnDate(tomorrow, modes)).andReturn(singleton(serviceB));

        TramDate yesterday = date.minusDays(1);
        EasyMock.expect(routeRepository.getRoutesRunningOn(yesterday, modes)).andReturn(Collections.emptySet());
        EasyMock.expect(serviceRepository.getServicesOnDate(yesterday, modes)).andReturn(Collections.emptySet());

        replayAll();
        RunningRoutesAndServices.FilterForDate filter = runningRoutesAndServices.getFor(date, modes);
        verifyAll();

        assertTrue(filter.isRouteRunning(Route.createBasicRouteId("routeAId"), false));
        assertTrue(filter.isServiceRunningByDate(Service.createId("serviceAId"), false));

        assertFalse(filter.isRouteRunning(Route.createBasicRouteId("routeBId"), false));
        assertFalse(filter.isServiceRunningByDate(Service.createId("serviceBId"), false));

        assertTrue(filter.isRouteRunning(Route.createBasicRouteId("routeAId"), true));
        assertTrue(filter.isServiceRunningByDate(Service.createId("serviceAId"), true));

        assertTrue(filter.isRouteRunning(Route.createBasicRouteId("routeBId"), true));
        assertTrue(filter.isServiceRunningByDate(Service.createId("serviceBId"), true));
    }

}
