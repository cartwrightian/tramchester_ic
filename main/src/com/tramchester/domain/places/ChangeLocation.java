package com.tramchester.domain.places;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

public record ChangeLocation<TYPE extends Location<TYPE>>(Location<TYPE> location, TransportMode fromMode) implements HasId<TYPE> {
    @Override
    public IdFor<TYPE> getId() {
        return location.getId();
    }

    @Override
    public String toString() {
        return "ChangeLocation{" +
                "location=" + location.getId() +
                ", fromMode=" + fromMode +
                '}';
    }
}
