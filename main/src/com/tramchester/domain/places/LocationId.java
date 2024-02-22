package com.tramchester.domain.places;

import com.tramchester.domain.id.IdFor;

import java.util.Objects;

public class LocationId implements IdFor<Location<?>> {

    private final IdFor<?> theId;

    public <TYPE extends Location<?>> LocationId(IdFor<TYPE> id) {
        theId = id;
    }

    @Override
    public String getGraphId() {
        return theId.getGraphId();
    }

    @Override
    public boolean isValid() {
        return theId.isValid();
    }

    @Override
    public Class<Location<?>> getDomainType() {
        return (Class<Location<?>>) theId.getDomainType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationId that = (LocationId) o;
        return Objects.equals(theId, that.theId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(theId);
    }

    @Override
    public String toString() {
        return "LocationId{" +
                "theId=" + theId +
                '}';
    }
}
