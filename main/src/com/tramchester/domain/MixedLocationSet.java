package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.reference.TransportMode;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MixedLocationSet implements LocationCollection {
    private final Set<Location<?>> locations;

    public <T extends Location<T>> MixedLocationSet(Collection<T> value) {
        locations = new HashSet<>(value);
    }

    public MixedLocationSet() {
        locations = new HashSet<>();
    }

    public MixedLocationSet(final LocationCollection collection) {
        locations = collection.locationStream().collect(Collectors.toSet());
    }

    // TODO
    // MixedFor a single location?
    @Deprecated
    public static MixedLocationSet singleton(final Location<?> location) {
        final MixedLocationSet result = new MixedLocationSet();
        result.add(location);
        return result;
    }

    private void add(final Location<?> item) {
        locations.add(item);
    }

    public EnumSet<TransportMode> getModes() {
        return locations.stream().
                flatMap(location -> location.getTransportModes().stream()).
                collect(Collectors.toCollection(() -> EnumSet.noneOf(TransportMode.class)));
    }

    @Override
    public Stream<Location<?>> locationStream() {
        return locations.stream();
    }

    @Override
    public int size() {
        return locations.size();
    }

    @Override
    public boolean isEmpty() {
        return locations.isEmpty();
    }

    @Override
    public boolean contains(final LocationId<?> locationId) {
        return locations.stream().anyMatch(location -> location.getLocationId().equals(locationId));
    }

    public void addAll(LocationCollection toAdd) {
        toAdd.locationStream().forEach(locations::add);
    }

    @Override
    public String toString() {
        return "MixedLocationSet{" +
                "ids=" + HasId.asIds(locations) +
                '}';
    }


}
