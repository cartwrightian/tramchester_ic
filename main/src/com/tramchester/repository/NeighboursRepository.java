package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;

import java.util.Set;

@ImplementedBy(Neighbours.class)
public interface NeighboursRepository {
    boolean differentModesOnly();

    Set<StationToStationConnection> getAll();
    Set<Station> getNeighboursFor(IdFor<Station> id);
    Set<StationToStationConnection> getNeighbourLinksFor(IdFor<Station> id);
    boolean hasNeighbours(IdFor<Station> id);
    boolean areNeighbours(Location<?> locationA, Location<?> locationB);
    boolean areNeighbours(LocationSet<Station> starts, LocationSet<Station> destinations);
}
