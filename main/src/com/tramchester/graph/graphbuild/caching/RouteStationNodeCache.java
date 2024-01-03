package com.tramchester.graph.graphbuild.caching;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;

public interface RouteStationNodeCache {
    void putRouteStation(IdFor<RouteStation> id, GraphNode routeStationNode);

    MutableGraphNode getRouteStation(MutableGraphTransaction txn, Route route, IdFor<Station> stationId);
    MutableGraphNode getRouteStation(MutableGraphTransaction txn, IdFor<RouteStation> routeStationId);

    boolean hasRouteStation(Route route, IdFor<Station> stationId);
}
