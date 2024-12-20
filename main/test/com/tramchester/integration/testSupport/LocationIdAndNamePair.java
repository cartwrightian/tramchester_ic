package com.tramchester.integration.testSupport;

import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
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

    public LocationIdAndNamePair(LocationIdPair<T> idPair, Function<IdFor<T>, String> resolvesName) {
        this(idPair, resolveNames(resolvesName, idPair));
    }

    private static <T extends Location<T>> Pair<String, String> resolveNames(Function<IdFor<T>, String> resolves, LocationIdPair<T> idPair) {
        String firstName = resolves.apply(idPair.getBeginId());
        String secondName = resolves.apply(idPair.getEndId());
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

}
