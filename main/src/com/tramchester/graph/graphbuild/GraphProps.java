package com.tramchester.graph.graphbuild;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphPropertyKey;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import static com.tramchester.domain.id.StringIdFor.getIdFromGraphEntity;
import static com.tramchester.graph.GraphPropertyKey.*;

public class GraphProps {

    // TODO - auto conversation to/from ENUM arrays now available?
    public static Set<TransportMode> getTransportModes(Entity entity) {
        if (!entity.hasProperty(TRANSPORT_MODES.getText())) {
            return Collections.emptySet();
        }

        short[] existing = (short[]) entity.getProperty(TRANSPORT_MODES.getText());
        return TransportMode.fromNumbers(existing);
    }

    public static IdFor<Station> getStationId(Entity entity) {
        return getStationIdFrom(entity);
    }

    public static void setTowardsProp(Node node, IdFor<Station> id) {
        node.setProperty(TOWARDS_STATION_ID.getText(), id.getGraphId());
    }

    private static Object getProperty(Entity entity, GraphPropertyKey graphPropertyKey) {
        return entity.getProperty(graphPropertyKey.getText());
    }

    // TODO Change to seconds, not minutes
    public static Duration getCost(Entity entity) {
        final int value = (int) getProperty(entity, COST);
        return Duration.ofMinutes(value);
    }

    public static IdFor<Route> getRouteIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, ROUTE_ID, Route.class);
    }

    public static IdFor<Station> getStationIdFrom(Entity entity) {
        return getIdFromGraphEntity(entity, STATION_ID, Station.class);
    }

    public static IdFor<RouteStation> getRouteStationIdFrom(Entity entity) {
        String value = entity.getProperty(ROUTE_STATION_ID.getText()).toString();
        return RouteStationId.parse(value);
    }

    public static IdFor<NaptanArea> getAreaIdFromGrouped(Entity entity) {
        return getIdFromGraphEntity(entity, AREA_ID, NaptanArea.class);
    }


}