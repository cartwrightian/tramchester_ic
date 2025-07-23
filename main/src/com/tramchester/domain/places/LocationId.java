package com.tramchester.domain.places;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;

import java.util.Objects;

/***
 * Facade around IdFor<T> to assist with handling of Location<?> scenarios when we need to getId()
 * @param <T>
 */
public class LocationId<T extends Location<?>> implements HasId<T> {

    private final IdFor<T> theId;

    protected LocationId(final IdFor<T> id) {
        theId = id;
    }

    public static <S extends Location<?>> LocationId<S> wrap(final IdFor<S> baseId) {
        return new LocationId<>(baseId);
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

    public boolean isValid() {
        return theId.isValid();
    }

    public LocationType getLocationType() {
        return LocationType.getFor(theId);
    }

    public Class<?> getDomainType() {
        return theId.getDomainType();
    }

    public LocationId<?> copy() {
        return new LocationId<>(this.theId);
    }
}
