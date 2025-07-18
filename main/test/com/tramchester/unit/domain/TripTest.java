package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TestRoute;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.time.TramTime.nextDay;
import static com.tramchester.domain.time.TramTime.of;
import static org.junit.jupiter.api.Assertions.*;

class TripTest {

    private TramStations stationA;
    private TramStations stationB;
    private TramStations stationC;

    private MutableTrip trip;
    private TestRoute route;

    @BeforeEach
    void beforeEachTestRuns() {
        Service service = MutableService.build(Service.createId("svcId"), DataSourceID.tfgm);

        trip = new MutableTrip(Trip.createId("tripId"),"headSign", service, TestEnv.getTramTestRoute(), Tram);

        stationA = TramStations.Ashton;
        stationB = TramStations.Broadway;
        stationC = TramStations.Cornbrook;

        route = KnownTramRoute.getGreen(TestEnv.testDay());
    }

    @Test
    void shouldModelCircularTripsCorrectly() {

        PlatformStopCall firstStop = TestEnv.createTramStopCall(trip, "statA1", stationA, (byte) 1,
                of(10, 0), of(10, 1), route);
        PlatformStopCall secondStop = TestEnv.createTramStopCall(trip, "statB1", stationB, (byte) 2,
                of(10, 5), of(10, 6), route);
        PlatformStopCall thirdStop = TestEnv.createTramStopCall(trip, "statA1", stationA, (byte) 3,
                of(10, 10), of(10, 10), route);

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        // sequence respected
        List<Integer> seqNums = new LinkedList<>();
        trip.getStopCalls().stream().forEach(stop -> seqNums.add(stop.getGetSequenceNumber()));
        assertEquals(1, seqNums.get(0).intValue());
        assertEquals(2, seqNums.get(1).intValue());
        assertEquals(3, seqNums.get(2).intValue());


    }

    @Test
    void shouldHaveTripDepartAndArrivalTimes() {

        PlatformStopCall firstStop = TestEnv.createTramStopCall(trip, "statA1", stationA, (byte) 1,
                of(10, 0), of(10, 1), route);
        PlatformStopCall secondStop = TestEnv.createTramStopCall(trip, "statB1", stationB, (byte) 2,
                of(10, 5), of(10, 6), route);
        PlatformStopCall thirdStop = TestEnv.createTramStopCall(trip, "statA1", stationA, (byte) 3,
                of(10, 10), of(10, 10), route);

        assertFalse(trip.hasStops());

        trip.addStop(firstStop);

        assertTrue(trip.hasStops());

        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        assertEquals(of(10, 1), trip.departTime());
        assertEquals(of(10,10), trip.arrivalTime());
    }

    @Test
    void shouldCheckIfNotCrossesIntoNextDay() {
        PlatformStopCall firstStop = TestEnv.createTramStopCall(trip, "stop1", stationA, (byte) 2,
                of(23, 45), of(23, 46), route);
        PlatformStopCall secondStop = TestEnv.createTramStopCall(trip, "stop2", stationB, (byte) 3,
                of(23, 59), of(0, 1), route);

        trip.addStop(firstStop);
        trip.addStop(secondStop);

        assertFalse(trip.intoNextDay());
    }

    @Test
    void shouldHaveOneStartAfterAnother() {
        PlatformStopCall firstStop = TestEnv.createTramStopCall(trip, "statA1", stationA, (byte) 1,
                of(10, 0), of(10, 1), route);
        PlatformStopCall secondStop = TestEnv.createTramStopCall(trip, "statB1", stationB, (byte) 2,
                of(10, 5), of(10, 6), route);
        PlatformStopCall thirdStop = TestEnv.createTramStopCall(trip, "statA1", stationC, (byte) 3,
                of(10, 10), of(10, 10), route);

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);

        assertTrue(trip.isAfter(stationA.getId(), stationB.getId()));
        assertTrue(trip.isAfter(stationA.getId(), stationC.getId()));
        assertTrue(trip.isAfter(stationB.getId(), stationC.getId()));

        assertFalse(trip.isAfter(stationA.getId(), stationA.getId()));
        assertFalse(trip.isAfter(stationB.getId(), stationA.getId()));
        assertFalse(trip.isAfter(stationC.getId(), stationA.getId()));
    }

    @Test
    void shouldFindEarliestDepartCorrectlyCrossingMidnight() {

        PlatformStopCall firstStop = TestEnv.createTramStopCall(trip, "stop1", stationA, (byte) 2,
                of(23, 45), of(23, 46), route);
        PlatformStopCall secondStop = TestEnv.createTramStopCall(trip, "stop2", stationB, (byte) 3,
                of(23, 59), of(0, 1), route);
        PlatformStopCall thirdStop = TestEnv.createTramStopCall(trip, "stop3", stationC, (byte) 4,
                nextDay(0, 10), nextDay(0, 11), route);
        PlatformStopCall fourthStop = TestEnv.createTramStopCall(trip, "stop4", stationC, (byte) 1,
                of(22, 45), of(22, 46), route);

        trip.addStop(firstStop);
        trip.addStop(secondStop);
        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        // trip uses seq number for earliest depart i.e. lowest seq is assumed to be the first depart
        assertEquals(of(22,46), trip.departTime());
        assertEquals(nextDay(0,10), trip.arrivalTime());

        assertTrue(trip.intoNextDay());
    }

    @Test
    void shouldFindDepartCorrectly() {

        PlatformStopCall thirdStop = TestEnv.createTramStopCall(trip, "stop3", stationC, (byte) 3,
                of(0, 10), of(0, 11), route);
        PlatformStopCall fourthStop = TestEnv.createTramStopCall(trip, "stop4", stationC, (byte) 1,
                of(6, 30), of(6, 31), route);

        trip.addStop(thirdStop);
        trip.addStop(fourthStop);

        assertEquals(of(6,31), trip.departTime());
    }

    @Test
    void shouldFindLatestDepartCorrectly() {
        TramStations deansgate = TramStations.Deansgate;
        trip.addStop(TestEnv.createTramStopCall(trip, "stopId3", deansgate, (byte) 3,
                of(10, 25), of(10, 26), route));
        trip.addStop(TestEnv.createTramStopCall(trip, "stopId4", deansgate, (byte) 4,
                of(0, 1), of(0, 1), route));

        assertEquals(of(0,1), trip.arrivalTime());

    }

    @Test
    void shouldKnowIfTramTrip() {
        Service service = MutableService.build(Service.createId("svcId"), DataSourceID.tfgm);

        Trip tripA = MutableTrip.build(Trip.createId("tripId"), "headSign", service, TestEnv.getTramTestRoute());
        assertTrue(TransportMode.isTram(tripA));
        final Agency agency = MutableAgency.build(DataSourceID.tfgm, Agency.createId("BUS"), "agencyName");
        Route busRoute = MutableRoute.getRoute(Route.createBasicRouteId("busRouteId"), "busRouteCode", "busRouteName",
                agency, TransportMode.Bus);
        Trip tripB = MutableTrip.build(Trip.createId("tripId"), "headSign", service, busRoute);
        assertFalse(TransportMode.isTram(tripB));
    }

}
