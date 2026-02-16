package com.tramchester.domain;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.reference.TransportMode;

import java.util.stream.Stream;

public interface LocationCollection {
    ImmutableEnumSet<TransportMode> getModes();

    Stream<Location<?>> locationStream();

    int size();

    boolean isEmpty();

    boolean contains(LocationId<?> locationId);
}
