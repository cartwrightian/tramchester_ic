package com.tramchester.graph.graphbuild;

import com.tramchester.domain.*;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static com.tramchester.domain.id.StringIdFor.getIdFromGraphEntity;
import static com.tramchester.graph.GraphPropertyKey.*;

public class GraphProps {

    public static void setProp(Node node, DataSourceInfo dataSourceInfo) {
        DataSourceID sourceID = dataSourceInfo.getID();
        node.setProperty(sourceID.name(), dataSourceInfo.getVersion());
    }

    public static void setProp(GraphNode node, DataSourceInfo dataSourceInfo) {
        setProp(node.getNode(), dataSourceInfo);
    }

    public static <C extends GraphProperty & CoreDomain & HasId<C>> void setProperty(Entity entity, C item) {
        entity.setProperty(item.getProp().getText(), item.getId().getGraphId());
    }

    public static <C extends GraphProperty & CoreDomain & HasId<C>> void setProperty(GraphRelationship relationship, C item) {
        relationship.set(item);
    }

    public static <C extends GraphProperty & CoreDomain & HasId<C>> void setProperty(GraphNode graphNode, C item) {
        Node entity = graphNode.getNode();
        entity.setProperty(item.getProp().getText(), item.getId().getGraphId());
    }

    public static LocalDate getEndDate(Relationship relationship) {
        return (LocalDate) relationship.getProperty(END_DATE.getText());
    }

    public static LocalDate getStartDate(Relationship relationship) {
        return (LocalDate) relationship.getProperty(START_DATE.getText());
    }

    public static void setProperty(Entity entity, TransportMode mode) {
        entity.setProperty(TRANSPORT_MODE.getText(), mode.getNumber());
    }

    public static void setProperty(GraphRelationship relationship, TransportMode mode) {
        relationship.setTransportMode(mode);
    }

    public static void setProperty(GraphNode graphNode, TransportMode mode) {
        setProperty(graphNode.getNode(), mode);
    }

    public static TransportMode getTransportMode(GraphNode graphNode) {
        return getTransportMode(graphNode.getNode());
    }

    public static TransportMode getTransportMode(Entity entity) {
        short number = (short) entity.getProperty(TRANSPORT_MODE.getText());
        return TransportMode.fromNumber(number);
    }

    public static EnumSet<TransportMode> getTransportModes(GraphRelationship graphRelationship) {
        return graphRelationship.getTransportModes();
    }

    // TODO - auto conversation to/from ENUM arrays now available?
    public static Set<TransportMode> getTransportModes(Entity entity) {
        if (!entity.hasProperty(TRANSPORT_MODES.getText())) {
            return Collections.emptySet();
        }

        short[] existing = (short[]) entity.getProperty(TRANSPORT_MODES.getText());
        return TransportMode.fromNumbers(existing);
    }

    public static void addTransportMode(GraphRelationship graphRelationship, TransportMode mode) {
        graphRelationship.addTransportMode(mode);
    }

    public static IdFor<Station> getStationId(Entity entity) {
        return getStationIdFrom(entity);
    }

    public static IdFor<Station> getStationId(GraphNode node) {
        return getStationIdFrom(node.getNode());
    }

    public static void setTowardsProp(Node node, IdFor<Station> id) {
        node.setProperty(TOWARDS_STATION_ID.getText(), id.getGraphId());
    }

    public static void setTowardsProp(GraphNode node, IdFor<Station> id) {
        setTowardsProp(node.getNode(), id);
    }

    private static Object getProperty(Entity entity, GraphPropertyKey graphPropertyKey) {
        return entity.getProperty(graphPropertyKey.getText());
    }

    // TODO Change to seconds, not minutes
    public static Duration getCost(Entity entity) {
        final int value = (int) getProperty(entity, COST);
        return Duration.ofMinutes(value);
    }

    public static Duration getCost(GraphRelationship relationship) {
        return relationship.getCost();
    }

    public static void setCostProp(GraphRelationship relationship, Duration duration) {
        relationship.setCost(duration);
    }

    public static void setMaxCostProp(GraphRelationship relationship, Duration duration) {
        relationship.setMaxCost(duration);
    }

    // TOOD MOVE
    public static int roundUpNearestMinute(Duration duration) {
        @SuppressWarnings("WrapperTypeMayBePrimitive")
        Double minutes = Math.ceil(duration.toSeconds()/60D);
        return minutes.intValue();
    }

    public static TramTime getTime(GraphNode graphNode) {
        return getTime(graphNode.getNode());
    }

    public static TramTime getTime(Entity entity) {
        LocalTime localTime = (LocalTime) getProperty(entity, TIME);
        boolean nextDay = entity.hasProperty(DAY_OFFSET.getText());
        if (nextDay) {
            return TramTime.nextDay(localTime.getHour(), localTime.getMinute());
        }
        return TramTime.of(localTime.getHour(), localTime.getMinute());
    }

    public static IdFor<Trip> getTripId(GraphNode graphNode) {
        return getTripIdFrom(graphNode.getNode());
    }

    public static IdFor<Trip> getTripId(Entity entity) {
        return getTripIdFrom(entity);
    }

    public static IdFor<Service> getServiceId(GraphNode graphNode) {
        return getServiceIdFrom(graphNode.getNode());
    }

    public static IdFor<Service> getServiceId(Entity entity) {
        return getServiceIdFrom(entity);
    }

    public static Integer getHour(GraphNode graphNode) {
        return getHour(graphNode.getNode());
    }

    public static Integer getHour(Entity node) {
        return (int) getProperty(node, HOUR);
    }

    public static void setLatLong(Entity entity, LatLong latLong) {
        entity.setProperty(LATITUDE.getText(), latLong.getLat());
        entity.setProperty(LONGITUDE.getText(), latLong.getLon());
    }

    public static void setLatLong(GraphNode graphNode, LatLong latLong) {
        setLatLong(graphNode.getNode(), latLong);
    }

    public static LatLong getLatLong(Entity entity) {
        final double lat = (double) getProperty(entity, LATITUDE);
        final double lon = (double) getProperty(entity, LONGITUDE);
        return new LatLong(lat, lon);
    }

    public static void setWalkId(Entity entity, LatLong origin, UUID uid) {
        entity.setProperty(GraphPropertyKey.WALK_ID.getText(), origin.toString() + "_" + uid.toString());
    }

    public static void setWalkId(GraphNode graphNode, LatLong origin, UUID uid) {
        setWalkId(graphNode.getNode(), origin, uid);
    }

    public static void setStopSequenceNumber(GraphRelationship relationship, int stopSequenceNumber) {
        relationship.setStopSeqNum(stopSequenceNumber);
    }

    public static IdFor<Route> getRouteIdFrom(GraphNode graphNode) {
        return getRouteIdFrom(graphNode.getNode());
    }

    public static IdFor<Route> getRouteIdFrom(GraphRelationship graphRelationship) {
        return graphRelationship.getRouteId();
    }

    public static IdFor<Route> getRouteIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, ROUTE_ID, Route.class);
    }

    public static IdFor<Station> getStationIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, STATION_ID, Station.class);
    }

    public static IdFor<Service> getServiceIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, SERVICE_ID, Service.class);
    }

    public static IdFor<Trip> getTripIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, TRIP_ID, Trip.class);
    }

    public static IdFor<RouteStation> getRouteStationIdFrom(Entity entity) {
        String value = entity.getProperty(ROUTE_STATION_ID.getText()).toString();
        return RouteStationId.parse(value);
    }

    public static PlatformId getPlatformIdFrom(Entity entity) {
        IdFor<Station> stationId = getStationIdFrom(entity);
        String platformNumber = entity.getProperty(PLATFORM_NUMBER.getText()).toString();
        return PlatformId.createId(stationId, platformNumber);
    }

    public static void setPlatformNumber(Entity entity, Platform platform) {
        entity.setProperty(PLATFORM_NUMBER.getText(), platform.getPlatformNumber());
    }

    public static void setPlatformNumber(GraphNode graphNode, Platform platform) {
        setPlatformNumber(graphNode.getNode(), platform);
    }

    public static IdFor<NaptanArea> getAreaIdFromGrouped(GraphNode graphNode) {
        return getAreaIdFromGrouped(graphNode.getNode());
    }

    public static IdFor<NaptanArea> getAreaIdFromGrouped(Entity entity) {
        return getIdFromGraphEntity(entity, AREA_ID, NaptanArea.class);
    }

    public static void setProperty(Entity entity, IdFor<NaptanArea> areaId) {
        entity.setProperty(AREA_ID.getText(), areaId.getGraphId());
    }

    public static void setProperty(GraphNode entity, IdFor<NaptanArea> areaId) {
        setProperty(entity.getNode(), areaId);
    }

    public static void setDateRange(GraphRelationship graphRelationship, DateRange range) {
        graphRelationship.setDateRange(range);
    }

    public static IdFor<Platform> getPlatformIdFrom(GraphNode graphNode) {
        return getPlatformIdFrom(graphNode.getNode());
    }


}