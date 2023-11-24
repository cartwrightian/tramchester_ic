package com.tramchester.graph.graphbuild;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.places.RouteStation;
import org.neo4j.graphdb.Node;

import static com.tramchester.graph.GraphPropertyKey.ROUTE_STATION_ID;

// NOTE: these support query invocations where end up with 'raw' nodes or relationships

public class GraphProps {

    ///// NODE //////////

    public static IdFor<RouteStation> getRouteStationIdFrom(Node node) {
        String value = node.getProperty(ROUTE_STATION_ID.getText()).toString();
        return RouteStationId.parse(value);
    }

}