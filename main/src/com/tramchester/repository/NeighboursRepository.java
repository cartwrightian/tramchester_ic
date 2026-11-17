package com.tramchester.repository;

import com.google.common.collect.ImmutableSet;
import com.google.inject.ImplementedBy;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;

@ImplementedBy(Neighbours.class)
public interface NeighboursRepository {
    boolean differentModesOnly();
    boolean isEnabled();

    ImmutableSet<StationToStationConnection> getAll();
    ImmutableSet<Station> getNeighboursFor(IdFor<Station> id);
    ImmutableSet<StationToStationConnection> getNeighbourLinksFor(IdFor<Station> id);
    boolean hasNeighbours(IdFor<Station> id);
    boolean areNeighbours(Location<?> locationA, Location<?> locationB);
    boolean areNeighbours(LocationSet<Station> starts, LocationSet<Station> destinations);

}
