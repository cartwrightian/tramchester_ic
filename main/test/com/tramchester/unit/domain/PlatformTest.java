package com.tramchester.unit.domain;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformTest {

    @Test
    void shouldCreatePlatformCorrectly() {
        IdFor<NaptanArea> areaId = NaptanArea.createId("area55");
        boolean isMarkedInterchange = true;
        Station station = TramStations.Altrincham.fake();
        PlatformId platformId = PlatformId.createId(station, "2");
        MutablePlatform platform = new MutablePlatform(platformId,
                station, "StationName", DataSourceID.tfgm, "2",
                areaId, nearAltrincham.latLong(), nearAltrincham.grid(), isMarkedInterchange);

        assertEquals("StationName platform 2", platform.getName());
        assertEquals(platformId, platform.getId());
        assertEquals( "2", platform.getPlatformNumber());
        assertEquals(nearAltrincham.latLong(), platform.getLatLong());
        assertEquals(nearAltrincham.grid(), platform.getGridPosition());
        assertEquals(DataSourceID.tfgm, platform.getDataSourceID());
        assertEquals(areaId, platform.getAreaId());
        assertTrue(isMarkedInterchange);

//        Service service = MutableService.build(Service.createId("serviceId"));
//        TramTime dropOffTime = TramTime.of(8,15);
//        TramTime pickupTime = TramTime.of(8, 20);

        assertTrue(platform.getDropoffRoutes().isEmpty());
        final Route tramTestRoute = TestEnv.getTramTestRoute();
        platform.addRouteDropOff(tramTestRoute);
        assertEquals(1, platform.getDropoffRoutes().size());
        assertTrue(platform.getDropoffRoutes().contains(tramTestRoute));

        assertEquals(1, platform.getDropoffRoutes().size());

        Route anotherRoute = TestEnv.getTramTestRoute(Route.createId("anotherRoute"), "routeNameB");

        platform.addRoutePickUp(anotherRoute);
        assertEquals(1, platform.getDropoffRoutes().size());
        assertTrue(platform.getPickupRoutes().contains(anotherRoute));

        final Set<Route> routes = platform.getPickupRoutes();
        assertEquals(1, routes.size());
        assertTrue(routes.contains(anotherRoute));

        final Set<TransportMode> transportModes = platform.getTransportModes();
        assertEquals(1, transportModes.size());
        assertTrue(transportModes.contains(tramTestRoute.getTransportMode()));

    }


}
