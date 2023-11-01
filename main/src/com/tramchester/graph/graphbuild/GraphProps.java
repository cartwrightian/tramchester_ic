package com.tramchester.graph.graphbuild;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import static com.tramchester.domain.id.StringIdFor.getIdFromGraphEntity;
import static com.tramchester.graph.GraphPropertyKey.*;

// NOTE: these support query invocations where end up with 'raw' nodes or relationships

public class GraphProps {

    ///// RELATIONSHIP //////////

    public static Set<TransportMode> getTransportModes(Relationship relationship) {
        if (!relationship.hasProperty(TRANSPORT_MODES.getText())) {
            return Collections.emptySet();
        }
        
        // TODO - auto conversation to/from ENUM arrays now available?
        short[] existing = (short[]) relationship.getProperty(TRANSPORT_MODES.getText());
        return TransportMode.fromNumbers(existing);
    }

    // TODO Change to seconds, not minutes
    public static Duration getCost(Relationship relationship) {
        final int value = (int) relationship.getProperty(COST.getText());
        return Duration.ofMinutes(value);
    }

    public static IdFor<Route> getRouteIdFrom(Relationship relationship) {
        return getIdFromGraphEntity(relationship, ROUTE_ID, Route.class);
    }

    ///// NODE //////////

    public static IdFor<RouteStation> getRouteStationIdFrom(Node node) {
        String value = node.getProperty(ROUTE_STATION_ID.getText()).toString();
        return RouteStationId.parse(value);
    }

    public static IdFor<NaptanArea> getAreaIdFromGrouped(Node node) {
        return getIdFromGraphEntity(node, AREA_ID, NaptanArea.class);
    }

    public static IdFor<Station> getStationId(Node node) {
        return getIdFromGraphEntity(node, STATION_ID, Station.class);
    }

}