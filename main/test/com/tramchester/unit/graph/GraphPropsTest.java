package com.tramchester.unit.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Agency;
import com.tramchester.domain.MutableRoute;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UnitTestOfGraphConfig;
import com.tramchester.testSupport.reference.KnownLocations;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
        MutableGraphRelationship relationship = createRelationship();

        IdFor<Route> routeId = StringIdFor.createId("routeId", Route.class);
        IdFor<RouteStation> id = RouteStation.createId(TramStations.ExchangeSquare.getId(), routeId);

        relationship.setRouteStationId(id);

        IdFor<RouteStation> result = relationship.getRouteStationId();

        assertEquals(id, result);
    }

    @Test
    void shouldBeAbleToSetRailRouteStationId() {
        MutableGraphRelationship relationship = createRelationship();

        IdFor<Route> routeId = getRailRouteId();

        IdFor<RouteStation> id = RouteStation.createId(RailStationIds.Stockport.getId(), routeId);

        relationship.setRouteStationId(id);

        IdFor<RouteStation> result = relationship.getRouteStationId();

        assertEquals(id, result);
    }

    @Test
    void shouldBeAbleToSetRoute() {

        Route route = TestEnv.getTramTestRoute();

        node.set(route);

        IdFor<Route> result = node.getRouteId();

        assertEquals(route.getId(), result);
    }

    @Test
    void shouldSetAndGetBounds() {
        BoundingBox boundingBox = TestEnv.getNationalTrainBounds();

        node.setBounds(boundingBox);

        BoundingBox result = node.getBounds();

        assertEquals(boundingBox, result);
    }

    @Test
    void shouldSetAndGetLatLongs() {
        LatLong latLong = KnownLocations.nearShudehill.latLong();

        node.setLatLong(latLong);

        LatLong result = node.getLatLong();

        assertEquals(latLong, result);
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

        MutableGraphRelationship relationship = createRelationship();

        TramTime time = TramTime.of(23,42);

        relationship.setTime(time);

        TramTime result = relationship.getTime();

        assertEquals(time, result);
    }

    @Test
    void shouldSetTimeRange() {
        MutableGraphRelationship relationship = createRelationship();

        TramTime begin = TramTime.of(9, 16);
        TramTime end = TramTime.of(17, 24);
        TimeRange timeRange = TimeRangePartial.of(begin, end);

        relationship.setTimeRange(timeRange);

        assertEquals(begin, relationship.getStartTime());
        assertEquals(end, relationship.getEndTime());
    }


    @Test
    void shouldSetDateTimeRangeCorrectly() {
        MutableGraphRelationship relationship = createRelationship();

        TramDate when = TestEnv.testDay();

        DateRange dateRange = DateRange.of(when, when.plusWeeks(1));
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(9,16), TramTime.of(17,24));
        DateTimeRange range = DateTimeRange.of(dateRange, timeRange);
        relationship.setDateTimeRange(range);

        assertEquals(dateRange, relationship.getDateRange());
        assertEquals(timeRange, relationship.getTimeRange());
    }


    @Test
    void shouldGetDateTimeRangeCorrectly() {
        MutableGraphRelationship relationship = createRelationship();

        TramDate when = TestEnv.testDay();

        DateRange dateRange = DateRange.of(when, when.plusWeeks(1));
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(9,16), TramTime.of(17,24));
        relationship.setDateRange(dateRange);
        relationship.setTimeRange(timeRange);

        DateTimeRange expected = DateTimeRange.of(dateRange, timeRange);

        assertEquals(expected, relationship.getDateTimeRange());
    }

    @Test
    void shouldGetDateTimeRangeCorrectlyAllDay() {
        MutableGraphRelationship relationship = createRelationship();

        TramDate when = TestEnv.testDay();

        DateRange dateRange = DateRange.of(when, when.plusWeeks(1));
        TimeRange timeRange = TimeRange.AllDay();
        relationship.setDateRange(dateRange);
        relationship.setTimeRange(timeRange);

        DateTimeRange result = relationship.getDateTimeRange();

        assertEquals(dateRange, result.getDateRange());
        assertTrue(result.allDay());
    }

    @Test
    void shouldSetTimeRangeCorrectly() {
        MutableGraphRelationship relationship = createRelationship();

        TramTime start = TramTime.of(9, 45);
        TramTime end = TramTime.of(13, 45);
        TimeRange timeRange = TimeRange.of(start, end);

        relationship.setTimeRange(timeRange);

        assertEquals(start, relationship.getStartTime());
        assertEquals(end, relationship.getEndTime());
    }


    @Test
    void shouldSetAndGetAllDayTimeRangeCorrectly() {
        MutableGraphRelationship relationship = createRelationship();

        TimeRange range = TimeRange.AllDay();

        relationship.setTimeRange(range);

        TimeRange result = relationship.getTimeRange();

        assertTrue(result.allDay());
    }

    @Test
    void shouldClearAllDayIfUpdatedWithSpecificTimesTimeRangeCorrectly() {
        MutableGraphRelationship relationship = createRelationship();

        TimeRange range = TimeRange.AllDay();

        relationship.setTimeRange(range);

        TimeRange resultA = relationship.getTimeRange();

        assertTrue(resultA.allDay());
        assertTrue(relationship.hasProperty(GraphPropertyKey.ALL_DAY));

        TramTime start = TramTime.of(9, 45);
        TramTime end = TramTime.of(13, 45);
        TimeRange timeRange = TimeRange.of(start, end);

        relationship.setTimeRange(timeRange);

        TimeRange resultB = relationship.getTimeRange();

        assertFalse(resultB.allDay());
        assertFalse(relationship.hasProperty(GraphPropertyKey.ALL_DAY));
        assertEquals(resultB, timeRange);
    }

    @Test
    void shouldClearSpecificTimesIfAllDayIsSetTimeRangeCorrectly() {
        MutableGraphRelationship relationship = createRelationship();

        TramTime start = TramTime.of(9, 45);
        TramTime end = TramTime.of(13, 45);
        TimeRange timeRange = TimeRange.of(start, end);

        relationship.setTimeRange(timeRange);

        TimeRange resultB = relationship.getTimeRange();

        assertFalse(resultB.allDay());

        TimeRange range = TimeRange.AllDay();

        relationship.setTimeRange(range);

        TimeRange resultA = relationship.getTimeRange();

        assertTrue(resultA.allDay());

        assertFalse(relationship.hasProperty(GraphPropertyKey.START_TIME));
        assertFalse(relationship.hasProperty(GraphPropertyKey.END_DATE));

    }

    @Test
    void shouldGetTimeRangeCorrectly() {
        MutableGraphRelationship relationship = createRelationship();

        TramTime start = TramTime.of(9, 45);
        TramTime end = TramTime.of(13, 45);
        TimeRange expected = TimeRange.of(start, end);

        relationship.setStartTime(start);
        relationship.setEndTime(end);

        TimeRange result = relationship.getTimeRange();

        assertEquals(expected, result);
    }

    @Test
    void shouldSetTimeWithNextDayCorrectly() {

        MutableGraphRelationship relationship = createRelationship();

        TramTime time = TramTime.nextDay(9,53);

        relationship.setTime(time);

        TramTime result = relationship.getTime();

        assertEquals(time, result);

        boolean flag = relationship.isDayOffset();
        assertTrue(flag);
    }

    @Test
    void shouldAddTransportModes() {

        MutableGraphRelationship relationship = createRelationship();

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
    void shouldFindRelationshipsBasedOnTripId() {
        IdFor<Trip> tripA = Trip.createId("tripA");
        IdFor<Trip> tripB = Trip.createId("tripB");

        MutableGraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        MutableGraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        MutableGraphRelationship serviceA = nodeA.createRelationshipTo(txn, nodeB, TransportRelationshipTypes.TO_SERVICE);

        serviceA.addTripId(tripB);

        assertFalse(nodeA.hasOutgoingServiceMatching(txn, tripA));
        assertEquals(0, nodeA.getOutgoingServiceMatching(txn, tripA).count());

        serviceA.addTripId(tripA);

        assertTrue(nodeA.hasOutgoingServiceMatching(txn, tripA));
        assertEquals(1, nodeA.getOutgoingServiceMatching(txn, tripA).count());
    }

//    @Test
//    void shouldHaveTestComparingTripListPerformance() {
//        fail();
//    }

    @Test
    void shouldAddTripIds() {
        MutableGraphRelationship relationship = createRelationship();

        IdFor<Trip> tripA = Trip.createId("tripA");
        IdFor<Trip> tripB = Trip.createId("tripB");

        relationship.addTripId(tripB);

        assertTrue(relationship.hasTripIdInList(tripB));

        relationship.addTripId(tripA);

        assertTrue(relationship.hasTripIdInList(tripA));

        IdSet<Trip> result = relationship.getTripIds();

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

        //IdFor<NPTGLocality> areaId = NPTGLocality.createId("areaId");
        Station station = TramStations.PiccadillyGardens.fakeWithPlatform(2, TestEnv.testDay());

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
        MutableGraphRelationship relationship = createRelationship();

        Duration duration = Duration.ofMinutes(42);

        relationship.setCost(duration);

        Duration result = relationship.getCost();

        assertEquals(duration, result);
    }

    @Test
    void shouldSetCostExact() {
        MutableGraphRelationship relationship = createRelationship();

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


    private MutableGraphRelationship createRelationship() {
        MutableGraphNode nodeA = txn.createNode(GraphLabel.ROUTE_STATION);
        MutableGraphNode nodeB = txn.createNode(GraphLabel.ROUTE_STATION);

        return nodeA.createRelationshipTo(txn, nodeB, TransportRelationshipTypes.ON_ROUTE);
    }

}
