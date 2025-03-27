package com.tramchester.domain.places;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;

import java.util.Objects;

/***
 * Facade around IdFor<T> to assist with handling of Location<?> scenarios when we need to getId()
 * @param <T>
 */
public class LocationId<T extends Location<?>> implements HasId<T> {

    private final IdFor<T> theId;

    public LocationId(final IdFor<T> id) {
        theId = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationId<?> that = (LocationId<?>) o;
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

    @Override
    public IdFor<T> getId() {
        return theId;
    }
}
