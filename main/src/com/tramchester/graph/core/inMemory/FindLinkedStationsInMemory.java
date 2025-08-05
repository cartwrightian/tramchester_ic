package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.FindLinkedStations;

import java.util.Set;

public class FindLinkedStationsInMemory implements FindLinkedStations {
    @Override
    public Set<StationToStationConnection> findLinkedFor(TransportMode mode) {
        throw new RuntimeException("todo");
    }

    @Override
    public IdSet<Station> atLeastNLinkedStations(TransportMode mode, int threshhold) {
        throw new RuntimeException("todo");
    }
}
