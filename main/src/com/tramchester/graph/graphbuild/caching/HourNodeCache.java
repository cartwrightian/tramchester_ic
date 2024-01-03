package com.tramchester.graph.graphbuild.caching;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;

public interface HourNodeCache {
    void putHour(IdFor<Route> routeId, Service service, IdFor<Station> station, Integer hour, GraphNode node);

    MutableGraphNode getHourNode(MutableGraphTransaction txn, IdFor<Route> routeId, Service service, IdFor<Station> station, Integer hour);

    boolean hasHourNode(IdFor<Route> routeId, Service service, IdFor<Station> startId, Integer hour);

}
