package com.tramchester.graph.search;

import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;

public interface FindLinkedStations {
    // supports visualisation of the transport network
    Set<StationToStationConnection> findLinkedFor(TransportMode mode);

    IdSet<Station> atLeastNLinkedStations(TransportMode mode, int threshhold);
}
