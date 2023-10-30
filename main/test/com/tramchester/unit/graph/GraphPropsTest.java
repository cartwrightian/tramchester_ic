package com.tramchester.unit.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphNode;
import com.tramchester.graph.GraphTransaction;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import com.tramchester.unit.graph.calculation.SimpleGraphConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.GraphPropertyKey.DAY_OFFSET;
import static org.junit.jupiter.api.Assertions.*;

public class GraphPropsTest {

    private static ComponentContainer componentContainer;
    private static SimpleGraphConfig config;
    private GraphTransaction txn;
    private GraphNode node;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new SimpleGraphConfig("graphquerytests.db");
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
        txn = graphDatabase.beginTx();
        node = txn.createNode(GraphLabel.QUERY_NODE);
    }

    @AfterEach
    void afterEachTestRuns() {
        // no commit
        txn.close();
    }

    @Test
    void shouldBeAbleToSetRouteStationId() {
        GraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        GraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        Relationship relationship = nodeA.createRelationshipTo(nodeB, TransportRelationshipTypes.ON_ROUTE);

        IdFor<Route> routeId = StringIdFor.createId("routeId", Route.class);
        IdFor<RouteStation> id = RouteStation.createId(TramStations.ExchangeSquare.getId(), routeId);

        GraphProps.setRouteStationProp(relationship, id);

        IdFor<RouteStation> result = GraphProps.getRouteStationIdFrom(relationship);

        assertEquals(id, result);
    }

    @Test
    void shouldBeAbleToSetRailRouteStationId() {
        GraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        GraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        Relationship relationship = nodeA.createRelationshipTo(nodeB, TransportRelationshipTypes.ON_ROUTE);

        IdFor<Route> routeId = getRailRouteId();

        IdFor<RouteStation> id = RouteStation.createId(RailStationIds.Stockport.getId(), routeId);

        GraphProps.setRouteStationProp(relationship, id);

        IdFor<RouteStation> result = GraphProps.getRouteStationIdFrom(relationship);

        assertEquals(id, result);
    }

    @Test
    void shouldBeAbleToSetRoute() {

        Route route = TestEnv.getTramTestRoute();

        GraphProps.setProperty(node, route);

        IdFor<Route> result = GraphProps.getRouteIdFrom(node);

        assertEquals(route.getId(), result);
    }

    @Test
    void shouldBeAbleToSetRailRoute() {

        IdFor<Route> routeId = getRailRouteId();

        Route route = MutableRoute.getRoute(routeId, "routeCode", "routeName", TestEnv.MetAgency(), TransportMode.Tram);

        GraphProps.setProperty(node, route);

        IdFor<Route> result = GraphProps.getRouteIdFrom(node);

        assertEquals(route.getId(), result);
    }

    @Test
    void shouldSetTimeCorrectly() {

        GraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        GraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        Relationship relationship = nodeA.createRelationshipTo(nodeB, TransportRelationshipTypes.ON_ROUTE);

        TramTime time = TramTime.of(23,42);

        GraphProps.setTimeProp(relationship, time);

        TramTime result = GraphProps.getTime(relationship);

        assertEquals(time, result);
    }

    @Test
    void shouldSetTimeWithNextDayCorrectly() {

        GraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        GraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        Relationship relationship = nodeA.createRelationshipTo(nodeB, TransportRelationshipTypes.ON_ROUTE);

        TramTime time = TramTime.nextDay(9,53);

        GraphProps.setTimeProp(relationship, time);

        TramTime result = GraphProps.getTime(relationship);

        assertEquals(time, result);

        Boolean flag = (Boolean) relationship.getProperty(DAY_OFFSET.getText());
        assertNotNull(flag);
        assertTrue(flag);
    }

    @Test
    void shouldAddTransportModes() {

        GraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        GraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        Relationship relationship = nodeA.createRelationshipTo(nodeB, TransportRelationshipTypes.ON_ROUTE);

        GraphProps.addTransportMode(relationship, TransportMode.Train);

        Set<TransportMode> result = GraphProps.getTransportModes(relationship);
        assertEquals(1, result.size());
        assertTrue(result.contains(TransportMode.Train));

        GraphProps.addTransportMode(relationship, TransportMode.Bus);

        result = GraphProps.getTransportModes(relationship);
        assertEquals(2, result.size());
        assertTrue(result.contains(TransportMode.Train));
        assertTrue(result.contains(TransportMode.Bus));

    }

    @Test
    void shouldAddSingleTransportMode() {
        GraphProps.setProperty(node, TransportMode.Train);

        TransportMode result = GraphProps.getTransportMode(node);

        assertEquals(result, TransportMode.Train);
    }
    
    @Test
    void shouldSetPlatformId() {

        IdFor<NaptanArea> areaId = StringIdFor.createId("areaId", NaptanArea.class);
        Station station = TramStations.PiccadillyGardens.fakeWithPlatform("2", TestEnv.stPetersSquareLocation(),
                DataSourceID.tfgm, areaId);

        List<Platform> platforms = new ArrayList<>(station.getPlatforms());
        Platform platform = platforms.get(0);

        GraphProps.setProperty(node, station);
        GraphProps.setProperty(node, platform);
        GraphProps.setPlatformNumber(node, platform);

        IdFor<Platform> platformId = GraphProps.getPlatformIdFrom(node);

        assertEquals(platform.getId(), platformId);
    }

    @Test
    void shouldSetCost() {
        GraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        GraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        Relationship relationship = nodeA.createRelationshipTo(nodeB, TransportRelationshipTypes.ON_ROUTE);

        Duration duration = Duration.ofMinutes(42);

        GraphProps.setCostProp(relationship, duration);

        Duration result = GraphProps.getCost(relationship);

        assertEquals(duration, result);
    }

    @Test
    void shouldSetCostCeiling() {
        GraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        GraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        Relationship relationship = nodeA.createRelationshipTo(nodeB, TransportRelationshipTypes.ON_ROUTE);

        Duration duration = Duration.ofMinutes(42).plusSeconds(15);

        GraphProps.setCostProp(relationship, duration);

        Duration result = GraphProps.getCost(relationship);

        assertEquals(Duration.ofMinutes(43), result);
    }

    @Test
    void shouldSetCostRoundUp() {
        GraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        GraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        Relationship relationship = nodeA.createRelationshipTo(nodeB, TransportRelationshipTypes.ON_ROUTE);

        Duration duration = Duration.ofMinutes(42).plusSeconds(55);

        GraphProps.setCostProp(relationship, duration);

        Duration result = GraphProps.getCost(relationship);

        assertEquals(Duration.ofMinutes(43), result);
    }

    @NotNull
    private IdFor<Route> getRailRouteId() {
        IdFor<Station> begin = RailStationIds.Macclesfield.getId();
        IdFor<Station> end = RailStationIds.Wimbledon.getId();
        IdFor<Agency> agency = StringIdFor.createId("agencyId", Agency.class);

        return new RailRouteId(begin, end, agency, 1);
    }

}
