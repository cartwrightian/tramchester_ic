package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.StationHelper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
import static org.junit.jupiter.api.Assertions.*;

class StationLocalityGroupTest {

    private final DataSourceID dataSourceID = DataSourceID.tfgm;

    @Test
    void shouldHaveCorrectValuesForOneStation() {
        LatLong latLong = new LatLong(-2.0, 2.3);
        MutableStation stationA = StationHelper.forTestMutable("id", "area", "stopName",
                latLong, dataSourceID, false);

        Route route = TestEnv.getTramTestRoute();
        stationA.addRoutePickUp(route);

        Platform platform = MutablePlatform.buildForTFGMTram("platformId", stationA, latLong, DataSourceID.unknown, NPTGLocality.InvalidId());
        stationA.addPlatform(platform);

        IdFor<NPTGLocality> areaId = NPTGLocality.createId("areaId");
        StationLocalityGroup groupedStations = new StationLocalityGroup(Collections.singleton(stationA), areaId,
                "compName", NPTGLocality.createId("parentId"), latLong);

        assertEquals(LocationType.StationGroup, groupedStations.getLocationType());

        assertEquals("compName", groupedStations.getName());
        assertIdEquals("areaId", groupedStations.getId());
        assertEquals(-2.0, groupedStations.getLatLong().getLat(),0);
        assertEquals(2.3, groupedStations.getLatLong().getLon(),0);

        assertEquals(areaId, groupedStations.getLocalityId());
        assertEquals(StationLocalityGroup.createId("areaId"), groupedStations.getId());
        assertEquals(StationLocalityGroup.createId("parentId"), groupedStations.getParentId());
        assertEquals("areaId", groupedStations.getId().getGraphId());

        assertTrue(groupedStations.hasPlatforms());
        assertEquals(Collections.singleton(platform), groupedStations.getPlatforms());
        assertEquals(Collections.singleton(route), groupedStations.getPickupRoutes());

        final Set<TransportMode> transportModes = groupedStations.getTransportModes();
        assertEquals(1, transportModes.size());
        assertTrue(transportModes.contains(Tram));

        LocationSet<Station> contained = groupedStations.getAllContained();
        assertEquals(1, contained.size());
        assertTrue(contained.contains(stationA));
    }

    @Test
    void shouldHaveCorrectValuesForTwoStation() {

        MutableStation stationA = StationHelper.forTestMutable("idA", "areaA", "stopNameA",
                new LatLong(2, 4), dataSourceID, false);
        Route routeA = TestEnv.getTramTestRoute(Route.createBasicRouteId("routeA"), "routeName");

        stationA.addRouteDropOff(routeA);
        Platform platformA = MutablePlatform.buildForTFGMTram("platformIdA", stationA,
                new LatLong(2, 4), DataSourceID.unknown, NPTGLocality.InvalidId());
        stationA.addPlatform(platformA);

        MutableStation stationB = StationHelper.forTestMutable("idB", "areaB", "stopNameB",
                new LatLong(4, 8), dataSourceID, true);
        Route routeB = MutableRoute.getRoute(Route.createBasicRouteId("routeB"), "routeCodeB", "routeNameB",
                TestEnv.BEE_A, Bus);
        stationB.addRouteDropOff(routeB);
        stationB.addRoutePickUp(routeA);
        Platform platformB = MutablePlatform.buildForTFGMTram("platformIdB", stationB,
                new LatLong(4, 8), DataSourceID.unknown, NPTGLocality.InvalidId());
        stationB.addPlatform(platformB);

        Set<Station> stations = new HashSet<>(Arrays.asList(stationA, stationB));
        IdFor<NPTGLocality> areaId = NPTGLocality.createId("areaId");
        StationLocalityGroup stationGroup = new StationLocalityGroup(stations, areaId, "compName", NPTGLocality.InvalidId(), new LatLong(53, -2));

        assertEquals(LocationType.StationGroup, stationGroup.getLocationType());

        assertEquals("compName", stationGroup.getName());
        //IdSet<Station> expected = Stream.of("idB", "idA").map(Station::createId).collect(IdSet.idCollector());
        assertIdEquals("areaId", stationGroup.getId());
        //assertEquals("areaId", stationGroup.forDTO());
        assertEquals("areaId", stationGroup.getId().getGraphId());
        //assertEquals("areaId", stationGroup.getId().forDTO());

        assertEquals(53, stationGroup.getLatLong().getLat(),0);
        assertEquals(-2, stationGroup.getLatLong().getLon(),0);
        assertEquals(areaId, stationGroup.getLocalityId());

        final Set<TransportMode> transportModes = stationGroup.getTransportModes();
        assertEquals(2, transportModes.size());
        assertTrue(transportModes.contains(Tram));
        assertTrue(transportModes.contains(Bus));

        assertEquals(2, stationGroup.getDropoffRoutes().size());
        assertEquals(1, stationGroup.getPickupRoutes().size());
//        assertEquals(2, stationGroup.getAgencies().size());

        LocationSet<Station> contained = stationGroup.getAllContained();
        assertEquals(2, contained.size());
        assertTrue(contained.contains(stationA));
        assertTrue(contained.contains(stationB));

    }

    @Test
    void shouldHaveCorrectPickupAndDropoff() {

        // Service service = MutableService.build(Service.createId("serviceId"));

        MutableStation stationA = StationHelper.forTestMutable("idA", "areaA", "stopNameA",
                new LatLong(2, 4), dataSourceID, false);
        Route routeA = TestEnv.getTramTestRoute(Route.createBasicRouteId("routeA"), "routeName");

        MutableStation stationB = StationHelper.forTestMutable("idB", "areaB", "stopNameB",
                new LatLong(4, 8), dataSourceID, false);
        Route routeB = MutableRoute.getRoute(Route.createBasicRouteId("routeB"), "routeCodeB",
                "routeNameB", TestEnv.BEE_A, Bus);

        Set<Station> stations = new HashSet<>(Arrays.asList(stationA, stationB));
        IdFor<NPTGLocality> areaId = NPTGLocality.createId("areaId");
        StationLocalityGroup stationGroup = new StationLocalityGroup(stations, areaId, "compName", NPTGLocality.InvalidId(), new LatLong(53, -2));

        assertFalse(stationGroup.hasPickup());
        assertFalse(stationGroup.hasDropoff());

        stationA.addRouteDropOff(routeA);
        assertTrue(stationGroup.hasDropoff());

        stationB.addRoutePickUp(routeB);
        assertTrue(stationGroup.hasPickup());

    }


}
