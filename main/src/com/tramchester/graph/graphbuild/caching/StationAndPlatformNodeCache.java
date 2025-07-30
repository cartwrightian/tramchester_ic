package com.tramchester.graph.graphbuild.caching;

import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;

public interface StationAndPlatformNodeCache {
    void putStation(IdFor<Station> station, GraphNode stationNode);
    void putPlatform(IdFor<Platform> platformId, GraphNode platformNode);

    MutableGraphNode getStation(MutableGraphTransaction txn, IdFor<Station> stationId);
    MutableGraphNode getPlatform(MutableGraphTransaction txn, IdFor<Platform> platformId);

}
