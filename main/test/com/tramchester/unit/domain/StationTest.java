package com.tramchester.unit.domain;


import com.tramchester.domain.*;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.MutableNormalServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.StationHelper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.domain.MutableAgency.Walking;
import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

class StationTest {

    private final IdFor<NPTGLocality> areaId = NPTGLocality.createId("area");

    @Test
    void testShouldCreateCorrecly() {
        Station tramStation = StationHelper.forTest("id", "area", "stopName",
                new LatLong(-2.0, 2.3), DataSourceID.tfgm);

        assertEquals("stopName", tramStation.getName());
        assertEquals(Station.createId("id"), tramStation.getId());
        assertEquals(-2.0, tramStation.getLatLong().getLat(),0);
        assertEquals(2.3, tramStation.getLatLong().getLon(),0);
        assertEquals(areaId, tramStation.getLocalityId());
        assertEquals(DataSourceID.tfgm, tramStation.getDataSourceID());
//        assertEquals("stationCode", tramStation.getCode());
    }

    @Test
    void testShouldSetBusNameCorrecly() {
        Station busStation = StationHelper.forTest("id", "area", "stopName",
                new LatLong(-2.0, 2.3), DataSourceID.tfgm);

        assertEquals("stopName", busStation.getName());
        assertEquals(Station.createId("id"), busStation.getId());
        assertEquals(-2.0, busStation.getLatLong().getLat(),0);
        assertEquals(2.3, busStation.getLatLong().getLon(),0);
        assertEquals(areaId, busStation.getLocalityId());
        //assertTrue(TransportMode.isBus(busStation));
    }

    @Test
    void shouldHaveCorrectTransportModes() {

        MutableStation station = new MutableStation(Station.createId("stationId"), areaId, "name", nearPiccGardens.latLong(),
                nearPiccGardens.grid(), DataSourceID.tfgm, false);

        assertTrue(station.getTransportModes().isEmpty());

        final Route route = MutableRoute.getRoute(Route.createId("routeIdA"), "shortName", "name",
                TestEnv.MetAgency(), Tram);
        station.addRouteDropOff(route);
        assertTrue(station.servesMode(Tram));
//        assertEquals("stationCode", station.getCode());

        station.addRouteDropOff(MutableRoute.getRoute(Route.createId("routeIdB"), "trainShort", "train",
                Walking, Train));
        assertTrue(station.servesMode(Train));

        assertEquals(2, station.getTransportModes().size());
    }

    @Test
    void shouldHavePickupAndDropoffRoutes() {
        MutableStation station = new MutableStation(Station.createId("stationId"), areaId, "name", nearPiccGardens.latLong(),
                nearPiccGardens.grid(), DataSourceID.tfgm, false);

        final Route routeA = MutableRoute.getRoute(Route.createId("routeIdA"), "shortNameA", "nameA",
                TestEnv.MetAgency(), Tram);
        final Route routeB = MutableRoute.getRoute(Route.createId("routeIdB"), "shortNameB", "nameB",
                TestEnv.StagecoachManchester, Bus);

        assertFalse(station.hasPickup());
        assertFalse(station.hasDropoff());

        station.addRoutePickUp(routeA);
        assertTrue(station.hasPickup());

        station.addRouteDropOff(routeB);
        assertTrue(station.hasDropoff());

        assertTrue(station.servesMode(Tram));
        assertTrue(station.servesMode(Bus));

        Set<Route> dropOffRoutes = station.getDropoffRoutes();
        assertEquals(1, dropOffRoutes.size());
        assertTrue(dropOffRoutes.contains(routeB));

        Set<Route> pickupRoutes = station.getPickupRoutes();
        assertEquals(1, pickupRoutes.size());
        assertTrue(pickupRoutes.contains(routeA));

        assertTrue(station.servesRoutePickup(routeA));
        assertFalse(station.servesRoutePickup(routeB));

        assertTrue(station.servesRouteDropOff(routeB));
        assertFalse(station.servesRouteDropOff(routeA));

        // TODO Routes for platforms?
    }

    @Test
    void shouldHavePickupAndDropoffRoutesForSpecificDates() {
        EnumSet<DayOfWeek> days = EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY);

        MutableStation station = new MutableStation(Station.createId("stationId"), areaId, "name", nearPiccGardens.latLong(),
                nearPiccGardens.grid(), DataSourceID.tfgm, true);

        final MutableRoute routeA = new MutableRoute(Route.createId("routeIdA"), "shortNameA", "nameA", TestEnv.MetAgency(), Tram);

        DateRange dateRangeA = new DateRange(TramDate.of(2022, 7, 15), TramDate.of(2022, 8, 24));

        MutableService serviceA = createService(days, dateRangeA, "serviceAId");

        DateRange dateRangeB = new DateRange(TramDate.of(2022, 7, 16), TramDate.of(2022,7,17));

        MutableService serviceB = createService(days, dateRangeB, "serviceBId");

        routeA.addService(serviceA);
        routeA.addService(serviceB);

        // only add routeA for serviceB , so should respect serviceB dates over the routes range

        station.addRoutePickUp(routeA);

        assertTrue(station.hasPickup());

        assertTrue(station.servesRoutePickup(routeA));

        assertTrue(station.isCentral());
    }

    @NotNull
    private MutableService createService(EnumSet<DayOfWeek> days, DateRange dateRange, String serviceId) {
        MutableService service = new MutableService(Service.createId(serviceId), DataSourceID.tfgm);
        MutableNormalServiceCalendar serviceCalendar = new MutableNormalServiceCalendar(dateRange, days);
        service.setCalendar(serviceCalendar);
        return service;
    }




}
