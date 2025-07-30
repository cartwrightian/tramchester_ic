package com.tramchester.graph.graphbuild.caching;

import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.facade.neo4j.MutableGraphNodeNeo4J;

public interface StationAndPlatformNodeCache {
    void putStation(IdFor<Station> station, GraphNode stationNode);
    void putPlatform(IdFor<Platform> platformId, GraphNode platformNode);

    MutableGraphNodeNeo4J getStation(MutableGraphTransaction txn, IdFor<Station> stationId);
    MutableGraphNodeNeo4J getPlatform(MutableGraphTransaction txn, IdFor<Platform> platformId);

}
