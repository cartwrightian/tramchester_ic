package com.tramchester.integration.testSupport;

import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationId;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Function;

// useful for diagnosing issue
public class LocationIdAndNamePair<T extends Location<T>> {
    private final LocationIdPair<T> idPair;
    private final Pair<String, String> namePair;

    public LocationIdAndNamePair(LocationIdPair<T> idPair, Pair<String, String> namePair) {
        this.idPair = idPair;
        this.namePair = namePair;
    }

    public LocationIdAndNamePair(LocationIdPair<T> idPair, Function<LocationId<T>, String> resolvesName) {
        this(idPair, resolveNames(resolvesName, idPair));
    }

    private static <T extends Location<T>> Pair<String, String> resolveNames(Function<LocationId<T>, String> resolves, LocationIdPair<T> idPair) {
        String firstName = resolves.apply(idPair.getBeginLocationId());
        String secondName = resolves.apply(idPair.getEndLocationId());
        return Pair.of(firstName, secondName);
    }

    @Override
    public String toString() {
        return "StationIdAndNamePair{" +
                namePair.getLeft() + "[" + idPair.getBeginId() + "], " +
                namePair.getRight() + "[" + idPair.getEndId() + "]}";

    }

    public IdFor<T> getBeginId() {
        return idPair.getBeginId();
    }

    public IdFor<T> getEndId() {
        return idPair.getEndId();
    }

    public LocationId<T> getBeginLocationId() {
        return idPair.getBeginLocationId();
    }

    public LocationId<T> getEndLocationId() {
        return idPair.getEndLocationId();
    }

}
