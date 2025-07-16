package com.tramchester.graph.graphbuild.caching;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.neo4j.MutableGraphNode;
import com.tramchester.graph.facade.neo4j.MutableGraphTransactionNeo4J;

public interface ServiceNodeCache {
    void putService(IdFor<Route> routeId, Service service, IdFor<Station> begin, IdFor<Station> end, GraphNode svcNode);

    MutableGraphNode getServiceNode(MutableGraphTransactionNeo4J txn, IdFor<Route> routeId, Service service, IdFor<Station> startStation, IdFor<Station> endStation);

    boolean hasServiceNode(IdFor<Route> routeId, Service service, IdFor<Station> begin, IdFor<Station> end);

}
