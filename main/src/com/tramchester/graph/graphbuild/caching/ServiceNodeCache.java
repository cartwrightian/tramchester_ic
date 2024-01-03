package com.tramchester.graph.graphbuild.caching;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;

public interface ServiceNodeCache {
    void putService(IdFor<Route> routeId, Service service, IdFor<Station> begin, IdFor<Station> end, GraphNode svcNode);

    MutableGraphNode getServiceNode(MutableGraphTransaction txn, IdFor<Route> routeId, Service service, IdFor<Station> startStation, IdFor<Station> endStation);

    boolean hasServiceNode(IdFor<Route> routeId, Service service, IdFor<Station> begin, IdFor<Station> end);

}
