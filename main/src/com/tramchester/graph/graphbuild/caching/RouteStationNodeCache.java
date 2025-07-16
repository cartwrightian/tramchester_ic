package com.tramchester.graph.graphbuild.caching;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.neo4j.MutableGraphNode;
import com.tramchester.graph.facade.neo4j.MutableGraphTransactionNeo4J;

public interface RouteStationNodeCache {
    void putRouteStation(IdFor<RouteStation> id, GraphNode routeStationNode);

    MutableGraphNode getRouteStation(MutableGraphTransactionNeo4J txn, Route route, IdFor<Station> stationId);
    MutableGraphNode getRouteStation(MutableGraphTransactionNeo4J txn, IdFor<RouteStation> routeStationId);

    boolean hasRouteStation(Route route, IdFor<Station> stationId);
}
