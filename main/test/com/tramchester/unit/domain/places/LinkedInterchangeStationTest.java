package com.tramchester.unit.domain.places;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.TestEnv.getTrainTestRoute;
import static com.tramchester.testSupport.reference.KnownLocations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LinkedInterchangeStationTest {

    private IdFor<Station> tramId;
    private IdFor<Station> trainId;

    private MutableStation tramStation;
    private MutableStation trainStation;

    private Route trainPickup;
    private Route trainDropoff;
    private Route tramDropoff;
    private Route tramPickup;

    @BeforeEach
    public void onceBeforeEachTestRuns() {
        tramDropoff = TestEnv.getTramTestRoute(Route.createBasicRouteId("routeTram1"), "tram route 1 name");
        tramPickup = TestEnv.getTramTestRoute(Route.createBasicRouteId("routeTram2"), "tram route 2 name");

        tramId = Station.createId("tramStationId");
        tramStation = new MutableStation(tramId, NPTGLocality.createId("naptanId1"),
                "station name", nearStPetersSquare.latLong(), nearStPetersSquare.grid(),  DataSourceID.tfgm, true);
        tramStation.addRouteDropOff(tramDropoff);
        tramStation.addRoutePickUp(tramPickup);

        trainId = Station.createId("railStationId");
        trainStation = new MutableStation(trainId, NPTGLocality.createId("napranId2"),
                "rail stations", nearPiccGardens.latLong(), nearPiccGardens.grid(), DataSourceID.openRailData, true);

        trainDropoff = getTrainTestRoute(Route.createBasicRouteId("routeTrain1"), "train route 1 name");
        trainPickup = getTrainTestRoute(Route.createBasicRouteId("routeTrain2"), "train route 2 name");
        trainStation.addRouteDropOff(trainDropoff);
        trainStation.addRoutePickUp(trainPickup);
    }

    @Test
    void shouldHaveCreateLinkedInterchangeAndAddLink() {

        EnumSet<TransportMode> modes = EnumSet.of(Walk);
        Quantity<Length> distance = Quantities.getQuantity(200, Units.METRE);
        Duration walkingTime = Duration.ofMinutes(4);

        StationToStationConnection.LinkType linkType = StationToStationConnection.LinkType.Linked;
        StationToStationConnection tramToTrain = new StationToStationConnection(tramStation, trainStation, modes, linkType, distance, walkingTime);

        IdFor<Station> busStationId = Station.createId("busStation1");
        MutableStation busStation = new MutableStation(busStationId, NPTGLocality.createId("napranId2"),
                "bus station", nearShudehill.latLong(), nearShudehill.grid(), DataSourceID.openRailData, true);
        IdFor<Route> routeId = Route.createBasicRouteId("busRouteA");
        Route busRoute = TestEnv.getBusTestRoute(routeId, "busRouteNameA");
        busStation.addRoutePickUp(busRoute);

        StationToStationConnection tramToBus = new StationToStationConnection(tramStation, busStation, modes, linkType, distance, walkingTime);

        LinkedInterchangeStation tramInterchange = new LinkedInterchangeStation(tramToTrain);

        assertTrue(tramInterchange.isMultiMode());
        assertEquals(EnumSet.of(Tram, Train), tramInterchange.getTransportModes());

        assertEquals(tramId, tramInterchange.getStationId());
        assertEquals(tramStation, tramInterchange.getStation());

        Set<Route> pickupRoutes = tramInterchange.getPickupRoutes();
        assertEquals(2, pickupRoutes.size());
        assertTrue(pickupRoutes.contains(tramPickup));
        assertTrue(pickupRoutes.contains(trainPickup));

        assertEquals(2, tramInterchange.getDropoffRoutes().size());
        assertTrue(tramInterchange.getDropoffRoutes().contains(tramDropoff));

        tramInterchange.addLink(tramToBus);

        pickupRoutes = tramInterchange.getPickupRoutes();
        assertEquals(3, pickupRoutes.size());

        assertEquals(EnumSet.of(Tram, Train, Bus), tramInterchange.getTransportModes());


//        InterchangeStation trainInterchange = new LinkedInterchangeStation(trainToTram);
//
//        assertTrue(trainInterchange.isMultiMode());
//
//        assertEquals(trainId, trainInterchange.getStationId());
//        assertEquals(trainStation, trainInterchange.getStation());
//
//        Set<Route> pickupRoutesTrain = trainInterchange.getPickupRoutes();
//        assertEquals(2, pickupRoutesTrain.size());
//        assertTrue(pickupRoutesTrain.contains(tramPickup));
//        assertTrue(pickupRoutesTrain.contains(trainPickup));
//
//        assertEquals(2, trainInterchange.getDropoffRoutes().size());
//        assertTrue(trainInterchange.getDropoffRoutes().contains(trainDropoff));
    }

    @Test
    void shouldHaveCreateLinkedInterchange() {

        EnumSet<TransportMode> modes = EnumSet.of(Walk);
        Quantity<Length> distance = Quantities.getQuantity(200, Units.METRE);
        Duration walkingTime = Duration.ofMinutes(4);

        StationToStationConnection.LinkType linkType = StationToStationConnection.LinkType.Linked;
        StationToStationConnection tramToTrain = new StationToStationConnection(tramStation, trainStation, modes, linkType, distance, walkingTime);
        StationToStationConnection trainToTram = new StationToStationConnection(trainStation, tramStation, modes, linkType, distance, walkingTime);

        InterchangeStation tramInterchange = new LinkedInterchangeStation(tramToTrain);

        assertTrue(tramInterchange.isMultiMode());

        assertEquals(tramId, tramInterchange.getStationId());
        assertEquals(tramStation, tramInterchange.getStation());

        Set<Route> pickupRoutes = tramInterchange.getPickupRoutes();
        assertEquals(2, pickupRoutes.size());
        assertTrue(pickupRoutes.contains(tramPickup));
        assertTrue(pickupRoutes.contains(trainPickup));

        assertEquals(2, tramInterchange.getDropoffRoutes().size());
        assertTrue(tramInterchange.getDropoffRoutes().contains(tramDropoff));

        InterchangeStation trainInterchange = new LinkedInterchangeStation(trainToTram);

        assertTrue(trainInterchange.isMultiMode());

        assertEquals(trainId, trainInterchange.getStationId());
        assertEquals(trainStation, trainInterchange.getStation());

        Set<Route> pickupRoutesTrain = trainInterchange.getPickupRoutes();
        assertEquals(2, pickupRoutesTrain.size());
        assertTrue(pickupRoutesTrain.contains(tramPickup));
        assertTrue(pickupRoutesTrain.contains(trainPickup));

        assertEquals(2, trainInterchange.getDropoffRoutes().size());
        assertTrue(trainInterchange.getDropoffRoutes().contains(trainDropoff));
    }

    @Test
    void shouldHaveCreateLinkedInterchangeMultipleLinks() {
        EnumSet<TransportMode> modes = EnumSet.of(Walk);

        Quantity<Length> distance = Quantities.getQuantity(200, Units.METRE);
        Duration walkingTime = Duration.ofMinutes(4);

        Route tramPickupB = TestEnv.getTramTestRoute(Route.createBasicRouteId("routeTram3"), "tram route 3 name");
        Route tramDropoffB = TestEnv.getTramTestRoute(Route.createBasicRouteId("routeTram4"), "tram route 4 name");

        IdFor<Station> tramIdB = Station.createId("tramStationBId");
        MutableStation tramStationB = new MutableStation(tramIdB, NPTGLocality.createId("naptanId1"),
                "station B name", nearStPetersSquare.latLong(), nearStPetersSquare.grid(), DataSourceID.tfgm,
                true);
        tramStationB.addRoutePickUp(tramPickupB);
        tramStationB.addRouteDropOff(tramDropoffB);

        StationToStationConnection.LinkType linkType = StationToStationConnection.LinkType.Linked;
        StationToStationConnection trainToTramA = new StationToStationConnection(trainStation, tramStation, modes, linkType, distance, walkingTime);
        StationToStationConnection trainToTramB = new StationToStationConnection(trainStation, tramStationB, modes, linkType, distance, walkingTime);

        LinkedInterchangeStation trainInterchange = new LinkedInterchangeStation(trainToTramA);
        trainInterchange.addLink(trainToTramB);

        assertEquals(trainId, trainInterchange.getStationId());
        assertEquals(trainStation, trainInterchange.getStation());

        assertEquals(3, trainInterchange.getDropoffRoutes().size());
        assertTrue(trainInterchange.getDropoffRoutes().contains(trainDropoff));

        Set<Route> pickupRoutes = trainInterchange.getPickupRoutes();
        assertEquals(3, pickupRoutes.size());
        assertTrue(pickupRoutes.contains(tramPickup));
        assertTrue(pickupRoutes.contains(tramPickupB));
        assertTrue(pickupRoutes.contains(trainPickup));

    }
}
