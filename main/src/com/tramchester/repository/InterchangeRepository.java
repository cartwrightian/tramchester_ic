package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

@ImplementedBy(Interchanges.class)
public interface InterchangeRepository {
    boolean isInterchange(Location<?> location);
    Set<InterchangeStation> getAllInterchanges();
    int size();

    InterchangeStation getInterchange(Location<?> location);

//    boolean isInterchange(IdFor<Station> stationId);

    boolean hasInterchangeFor(RouteIndexPair indexPair);

    Stream<InterchangeStation> getInterchangesFor(RouteIndexPair indexPair);

    EnumSet<TransportMode> getInterchangeModes(LocationCollection destinations);

    EnumSet<TransportMode> getInterchangeModes(Location<?> location);
}
