package com.tramchester.domain;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;

public record DestinationAndCallingPoints(IdFor<Station> destination, ImmutableIdSet<Station> callingPoints) {
    @Override
    public String toString() {
        return "DestinationAndCallingPoints{" +
                "destination=" + destination +
                ", callingPoints=" + callingPoints +
                '}';
    }

    public static DestinationAndCallingPoints None() {
        return new DestinationAndCallingPoints(Station.InvalidId(), IdSet.emptySet());
    }

    public boolean isNone() {
        return callingPoints.isEmpty() && !destination.isValid();
    }
}
