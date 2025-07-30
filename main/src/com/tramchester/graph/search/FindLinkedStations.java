package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.core.neo4j.FindLinkedStationsNeo4J;

import java.util.Set;

@ImplementedBy(FindLinkedStationsNeo4J.class)
public interface FindLinkedStations {
    // supports visualisation of the transport network
    Set<StationToStationConnection> findLinkedFor(TransportMode mode);

    IdSet<Station> atLeastNLinkedStations(TransportMode mode, int threshhold);
}
