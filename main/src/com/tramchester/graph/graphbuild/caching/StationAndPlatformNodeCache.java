package com.tramchester.graph.graphbuild.caching;

import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.neo4j.MutableGraphNode;
import com.tramchester.graph.facade.neo4j.MutableGraphTransactionNeo4J;

public interface StationAndPlatformNodeCache {
    void putStation(IdFor<Station> station, GraphNode stationNode);
    void putPlatform(IdFor<Platform> platformId, GraphNode platformNode);

    MutableGraphNode getStation(MutableGraphTransactionNeo4J txn, IdFor<Station> stationId);
    MutableGraphNode getPlatform(MutableGraphTransactionNeo4J txn, IdFor<Platform> platformId);

}
