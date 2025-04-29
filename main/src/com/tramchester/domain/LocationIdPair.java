package com.tramchester.domain;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;

import java.util.Objects;

public class LocationIdPair<TYPE extends Location<TYPE>> {

    private final IdPair<TYPE> pair;

    public LocationIdPair(TYPE begin, TYPE end) {
        this(begin.getId(), end.getId());
        if (begin.getLocationType()!=end.getLocationType()) {
            throw new RuntimeException(String.format("Attempt to create locationIdPair for two mismatched types %s and %s",
                    begin, end));
        }
    }

    public LocationIdPair(IdFor<TYPE> begin, IdFor<TYPE> end) {
        pair = new IdPair<>(begin, end);
    }

    public static <T extends Location<T>> LocationIdPair<T> of(T locA, T locB) {
        return new LocationIdPair<>(locA, locB);
    }

    public IdFor<TYPE> getBeginId() {
        return pair.getFirst();
    }

    public IdFor<TYPE> getEndId() {
        return pair.getSecond();
    }

    public boolean same() {
        return pair.same();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationIdPair<?> that = (LocationIdPair<?>) o;
        return Objects.equals(pair, that.pair);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pair);
    }

    @Override
    public String toString() {
        return "LocationIdPair{" +
                pair.getFirst() +
                ", " + pair.getSecond() +
                '}';
    }

}
