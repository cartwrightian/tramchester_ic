package com.tramchester.unit.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import com.tramchester.testSupport.UnitTestOfGraphConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphPropsTest {

    // TODO Split into GraphNodeTest and GraphRelationshipTest

    private static ComponentContainer componentContainer;
    private static UnitTestOfGraphConfig config;
    private MutableGraphTransaction txn;
    private MutableGraphNode node;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new UnitTestOfGraphConfig();
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        txn = graphDatabase.beginTxMutable();
        node = txn.createNode(GraphLabel.QUERY_NODE);
    }

    @AfterEach
    void afterEachTestRuns() {
        // no commit
        txn.close();
    }

    @Test
    void shouldBeAbleToSetRouteStationId() {
        MutableGraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        MutableGraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        MutableGraphRelationship relationship = nodeA.createRelationshipTo(txn, nodeB, TransportRelationshipTypes.ON_ROUTE);

        IdFor<Route> routeId = StringIdFor.createId("routeId", Route.class);
        IdFor<RouteStation> id = RouteStation.createId(TramStations.ExchangeSquare.getId(), routeId);

        //GraphProps.setRouteStationProp(relationship, id);
        relationship.setRouteStationId(id);

        IdFor<RouteStation> result = relationship.getRouteStationId(); //GraphProps.getRouteStationIdFrom(relationship);

        assertEquals(id, result);
    }

    @Test
    void shouldBeAbleToSetRailRouteStationId() {
        MutableGraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        MutableGraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        MutableGraphRelationship relationship = nodeA.createRelationshipTo(txn, nodeB, TransportRelationshipTypes.ON_ROUTE);

        IdFor<Route> routeId = getRailRouteId();

        IdFor<RouteStation> id = RouteStation.createId(RailStationIds.Stockport.getId(), routeId);

        relationship.setRouteStationId(id);

        IdFor<RouteStation> result = relationship.getRouteStationId(); //GraphProps.getRouteStationIdFrom(relationship);

        assertEquals(id, result);
    }

    @Test
    void shouldBeAbleToSetRoute() {

        Route route = TestEnv.getTramTestRoute();

        //GraphProps.setProperty(node, route);
        node.set(route);

        IdFor<Route> result = node.getRouteId();

        assertEquals(route.getId(), result);
    }

    @Test
    void shouldBeAbleToSetRailRoute() {

        IdFor<Route> routeId = getRailRouteId();

        Route route = MutableRoute.getRoute(routeId, "routeCode", "routeName", TestEnv.MetAgency(), TransportMode.Tram);

        node.set(route);

        IdFor<Route> result = ((GraphNode) node).getRouteId();

        assertEquals(route.getId(), result);
    }

    @Test
    void shouldSetTimeCorrectly() {

        MutableGraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        MutableGraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        MutableGraphRelationship relationship = nodeA.createRelationshipTo(txn, nodeB, TransportRelationshipTypes.ON_ROUTE);

        TramTime time = TramTime.of(23,42);

        relationship.setTime(time);

        TramTime result = relationship.getTime();

        assertEquals(time, result);
    }

    @Test
    void shouldSetTimeWithNextDayCorrectly() {

        MutableGraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        MutableGraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        MutableGraphRelationship relationship = nodeA.createRelationshipTo(txn, nodeB, TransportRelationshipTypes.ON_ROUTE);

        TramTime time = TramTime.nextDay(9,53);

        relationship.setTime(time);

        TramTime result = relationship.getTime();

        assertEquals(time, result);

        boolean flag = relationship.isDayOffset();
        assertTrue(flag);
    }

    @Test
    void shouldAddTransportModes() {

        MutableGraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        MutableGraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        MutableGraphRelationship relationship = nodeA.createRelationshipTo(txn, nodeB, TransportRelationshipTypes.ON_ROUTE);

        relationship.addTransportMode(TransportMode.Train);

        Set<TransportMode> result = relationship.getTransportModes();
        assertEquals(1, result.size());
        assertTrue(result.contains(TransportMode.Train));

        relationship.addTransportMode(TransportMode.Bus);

        result = relationship.getTransportModes();
        assertEquals(2, result.size());
        assertTrue(result.contains(TransportMode.Train));
        assertTrue(result.contains(TransportMode.Bus));

    }

    @Test
    void shouldAddTripIds() {
        MutableGraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        MutableGraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        MutableGraphRelationship relationship = nodeA.createRelationshipTo(txn, nodeB, TransportRelationshipTypes.ON_ROUTE);

        IdFor<Trip> tripA = Trip.createId("tripA");
        IdFor<Trip> tripB = Trip.createId("tripB");

        relationship.addTripId(tripB);

        assertTrue(relationship.hasTripId(tripB));

        relationship.addTripId(tripA);

        assertTrue(relationship.hasTripId(tripA));

        List<IdFor<Trip>> result = relationship.getTripIds();

        assertEquals(2, result.size());
        assertTrue(result.contains(tripA));
        assertTrue(result.contains(tripB));

        relationship.addTripId(tripA);

        result = relationship.getTripIds();

        assertEquals(2, result.size());

    }

    @Test
    void shouldAddSingleTransportMode() {
        node.setTransportMode(TransportMode.Train);

        TransportMode result = ((GraphNode) node).getTransportMode();

        assertEquals(result, TransportMode.Train);
    }
    
    @Test
    void shouldSetPlatformId() {

        IdFor<NPTGLocality> areaId = NPTGLocality.createId("areaId");
        Station station = TramStations.PiccadillyGardens.fakeWithPlatform("2", KnownLocations.nearPiccGardens.latLong(),
                DataSourceID.tfgm, areaId);

        List<Platform> platforms = new ArrayList<>(station.getPlatforms());
        Platform platform = platforms.get(0);

        node.set(station);
        node.set(platform);
        node.setPlatformNumber(platform);

        IdFor<Platform> platformId = ((GraphNode) node).getPlatformId();

        assertEquals(platform.getId(), platformId);
    }

    @Test
    void shouldSetCost() {
        MutableGraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        MutableGraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        MutableGraphRelationship relationship = nodeA.createRelationshipTo(txn, nodeB, TransportRelationshipTypes.ON_ROUTE);

        Duration duration = Duration.ofMinutes(42);

        relationship.setCost(duration);

        Duration result = relationship.getCost();

        assertEquals(duration, result);
    }

    @Test
    void shouldSetCostExact() {
        MutableGraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        MutableGraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        MutableGraphRelationship relationship = nodeA.createRelationshipTo(txn, nodeB, TransportRelationshipTypes.ON_ROUTE);

        Duration duration = Duration.ofMinutes(42).plusSeconds(15);

        relationship.setCost(duration);

        Duration result = relationship.getCost();

        assertEquals(duration, result);
    }

    @NotNull
    private IdFor<Route> getRailRouteId() {
        IdFor<Station> begin = RailStationIds.Macclesfield.getId();
        IdFor<Station> end = RailStationIds.Wimbledon.getId();
        IdFor<Agency> agency = StringIdFor.createId("agencyId", Agency.class);

        return new RailRouteId(begin, end, agency, 1);
    }

}
