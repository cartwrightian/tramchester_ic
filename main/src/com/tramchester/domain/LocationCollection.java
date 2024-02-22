package com.tramchester.domain;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;
import java.util.stream.Stream;

public interface LocationCollection {
    EnumSet<TransportMode> getModes();

    Stream<Location<?>> locationStream();

//    boolean contains(IdFor<Station> stationId);

    int size();

    boolean isEmpty();

    boolean contains(LocationId locationId);
}
