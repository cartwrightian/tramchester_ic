package com.tramchester.unit.graph.inMemory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.inMemory.*;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.graph.reference.GraphLabel.STATION;
import static com.tramchester.testSupport.reference.TramStations.Bury;
import static org.junit.jupiter.api.Assertions.*;


public class GraphSerializationTest {
    private ObjectMapper mapper;
    private final TramDate when = TestEnv.testDay();

    @BeforeEach
    void beforeEachTestRuns() {
        mapper = SaveGraph.createMapper();
    }

    @Disabled("old format, re-instate with new sample text")
    @Test
    void deserializeFromSample() throws JsonProcessingException {
        String text = """
                {"relationshipType":"TO_SERVICE","relationshipId":{"id":18547},"startId":{"id":351},"endId":{"id":17703},
                "properties":[{"key":"cost","value":{"duration":0.0}},{"key":"route_id","value":"Blue>>2119"},
                {"key":"trip_id_list","value":{"idSet":{"ids":["13707_1054","13707_1066","13707_1156","13707_1069","13707_1048","13707_1147",
                "13707_605","13707_1080","13707_602","13707_624","13707_1071","13707_1060","13707_611","13707_1063","13707_1042",
                "13707_1075","13707_1152","13707_608","13707_618"]} }} ] }
                """;

        GraphRelationshipInMemory result = mapper.readValue(text, GraphRelationshipInMemory.class);

        assertEquals(Route.createBasicRouteId("Blue>>2119"),result.getRouteId());
        assertEquals(Duration.ZERO, result.getCost());
        assertEquals(new RelationshipIdInMemory(18547), result.getId());
        assertEquals(new NodeIdInMemory(351), result.getStartId());
        assertEquals(new NodeIdInMemory(17703), result.getEndId());

        assertFalse(result.getTripIds().isEmpty());
    }

    @Test
    void shouldRoundTripGraphNode()  {
        NodeIdInMemory id = new NodeIdInMemory(678);
        EnumSet<GraphLabel> labels = EnumSet.of(STATION, GraphLabel.INTERCHANGE);
        GraphNodeInMemory graphNodeInMemory = new GraphNodeInMemory(id, labels);

        TramTime tramTime = TramTime.of(11, 42);
        graphNodeInMemory.setTime(tramTime);
        graphNodeInMemory.setLatLong(KnownLocations.nearBury.latLong());
        graphNodeInMemory.set(TestEnv.getTramTestRoute());
        graphNodeInMemory.setTransportMode(Tram);

        String text = null;
        try {
            text = mapper.writeValueAsString(graphNodeInMemory);
        } catch (JsonProcessingException e) {
            fail("failed to serialise", e);
        }

        GraphNodeInMemory result = null;
        try {
            result = mapper.readValue(text, GraphNodeInMemory.class);
        } catch (JsonProcessingException e) {
                fail("Unable to deserialize " + text,e);
        }

        assertEquals(graphNodeInMemory, result);

        try {
            assertEquals(tramTime, result.getTime());
            assertEquals(KnownLocations.nearBury.latLong(), result.getLatLong());
            assertEquals(TestEnv.getTramTestRoute().getId(), result.getRouteId());
            assertEquals(Tram, result.getTransportMode());
        }
        catch(ClassCastException e) {
            fail("Unable to fetch property from " + text, e);
        }
    }

    @Test
    void shouldRoundTripGraphRelationship()  {
        GraphRelationshipInMemory relationship = createRelationship();

        IdFor<Trip> tripA = Trip.createId("tripA");
        IdFor<Trip> tripB = Trip.createId("tripB");

        Duration cost = Duration.of(65, ChronoUnit.SECONDS);
        IdFor<RouteStation> routeStationId = RouteStationId.createId(KnownTramRoute.getBlue(when).getId(), Bury.getId());
        TramTime tramTime = TramTime.of(11, 42);

        relationship.setTime(tramTime);
        relationship.set(TestEnv.getTramTestRoute());
        relationship.setHour(17);
        relationship.setStopSeqNum(42);
        relationship.setCost(cost);
        relationship.setRouteStationId(routeStationId);
        relationship.addTripId(tripA);
        relationship.addTripId(tripB);
        relationship.addTransportMode(Bus);
        relationship.addTransportMode(Tram);

        String text = serializeToString(relationship);

        GraphRelationshipInMemory result = deserializeFromString(text);

        assertEquals(relationship, result);

        try {
            assertEquals(tramTime, result.getTime());
            assertEquals(TestEnv.getTramTestRoute().getId(), result.getRouteId());
            assertEquals(17, result.getHour());
            assertEquals(42, result.getStopSeqNumber());
            assertEquals(cost, result.getCost());
            assertEquals(routeStationId, result.getRouteStationId());
            IdSet<Trip> trips = result.getTripIds();
            assertEquals(2, trips.size());
            assertTrue(trips.contains(tripA));
            assertTrue(trips.contains(tripB));
            assertEquals(EnumSet.of(Bus,Tram), result.getTransportModes());

        }
        catch(ClassCastException e) {
            fail("Unable to fetch property from " + text, e);
        }

    }

    @Test
    void shouldRoundTripGraphRelationshipIdSet()  {
        GraphRelationshipInMemory relationship = createRelationship();

        IdFor<Trip> tripA = Trip.createId("tripA");
        IdFor<Trip> tripB = Trip.createId("tripB");

        relationship.addTripId(tripA);
        relationship.addTripId(tripB);

        String text = serializeToString(relationship);

        GraphRelationshipInMemory result = deserializeFromString(text);

        assertEquals(relationship, result);

        try {
            IdSet<Trip> trips = result.getTripIds();
            assertEquals(2, trips.size());
            assertTrue(trips.contains(tripA));
            assertTrue(trips.contains(tripB));
        }
        catch(ClassCastException e) {
            fail("Unable to fetch property from " + text, e);
        }
    }

    @Test
    void shouldRoundTripGraphRelationshipEnumSet()  {
        GraphRelationshipInMemory relationship = createRelationship();

        relationship.addTransportMode(Bus);
        relationship.addTransportMode(Tram);

        String text = serializeToString(relationship);

        GraphRelationshipInMemory result = deserializeFromString(text);

        assertEquals(relationship, result);

        try {
            assertEquals(EnumSet.of(Bus,Tram), result.getTransportModes());
        }
        catch(ClassCastException e) {
            fail("Unable to fetch property from " + text, e);
        }

    }

    @Test
    void shouldRoundTripGraphRelationshipTime() {
        GraphRelationshipInMemory relationship = createRelationship();

        TramTime tramTime = TramTime.of(11, 42);
        relationship.setTime(tramTime);

        String text = serializeToString(relationship);

        GraphRelationshipInMemory result = deserializeFromString(text);

        assertEquals(relationship, result);

        try {
            assertEquals(tramTime, result.getTime());
        } catch (ClassCastException e) {
            fail("Unable to fetch property from " + text, e);
        }
    }

    @Test
    void shouldRoundTripGraphRelationshipTimeNextDay() {
        GraphRelationshipInMemory relationship = createRelationship();

        TramTime tramTime = TramTime.nextDay(11, 42);
        relationship.setTime(tramTime);

        String text = serializeToString(relationship);

        GraphRelationshipInMemory result = deserializeFromString(text);

        assertEquals(relationship, result);

        try {
            assertEquals(tramTime, result.getTime());
        } catch (ClassCastException e) {
            fail("Unable to fetch property from " + text, e);
        }
    }

    private String serializeToString(GraphRelationshipInMemory relationship) {
        try {
            return mapper.writeValueAsString(relationship);
        } catch (JsonProcessingException e) {
            fail("failed to serialise", e);
            return null;
        }
    }

    private GraphRelationshipInMemory deserializeFromString(final String text) {
        try {
            return mapper.readValue(text, GraphRelationshipInMemory.class);
        } catch (JsonProcessingException e) {
            fail("Unable to deserialize " + text,e);
            return null;
        }
    }

    private static @NotNull GraphRelationshipInMemory createRelationship() {
        NodeIdInMemory idA = new NodeIdInMemory(678);
        NodeIdInMemory idB = new NodeIdInMemory(679);
        RelationshipIdInMemory id = new RelationshipIdInMemory(42);
        return new GraphRelationshipInMemory(TransportRelationshipTypes.BOARD, id,
                idA, idB);
    }

}
