package com.tramchester.graph.graphbuild;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.domain.id.StringIdFor.getIdFromGraphEntity;
import static com.tramchester.graph.GraphPropertyKey.*;

// NOTE: these support query invocations where end up with 'raw' nodes or relationships

public class GraphProps {

    ///// RELATIONSHIP //////////

    public static IdFor<Route> getRouteIdFrom(Relationship relationship) {
        return getIdFromGraphEntity(relationship, ROUTE_ID, Route.class);
    }

    ///// NODE //////////

    public static IdFor<RouteStation> getRouteStationIdFrom(Node node) {
        String value = node.getProperty(ROUTE_STATION_ID.getText()).toString();
        return RouteStationId.parse(value);
    }

    public static IdFor<Station> getStationId(Node node) {
        return getIdFromGraphEntity(node, STATION_ID, Station.class);
    }

}