package com.tramchester.domain;

import com.google.common.collect.Sets;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class LocationSet {

    private final Set<Location<?>> locations;

    public LocationSet() {
        locations = new HashSet<>();
    }

    public <T extends Location<T>> LocationSet(Collection<T> stations) {
        this.locations = new HashSet<>(stations);
    }

    public int size() {
        return locations.size();
    }

    public Stream<Location<?>> stream() {
        return locations.stream();
    }

    public void add(Location<?> location) {
        locations.add(location);
    }

    public boolean contains(Location<?> start) {
        return locations.contains(start);
    }

    public String asIds() {
        StringBuilder ids = new StringBuilder();
        ids.append("[");
        locations.forEach(item -> ids.append(" '").append(item.getId()).append("'"));
        ids.append("]");
        return ids.toString();
    }

    @Override
    public String toString() {
        return "LocationSet{" +
                "locations=" + this.asIds() +
                '}';
    }

    public Stream<Station> stationsOnlyStream() {
        return locations.stream().
                filter(location -> location.getLocationType()==LocationType.Station).
                map(location -> (Station) location);
    }

    public static LocationSet of(final Set<Station> stations) {
        return new LocationSet(stations);
    }

    public static LocationSet singleton(Location<?> location) {
        final LocationSet locationSet = new LocationSet();
        locationSet.add(location);
        return locationSet;
    }

    public void addAll(LocationSet other) {
        this.locations.addAll(other.locations);
    }

    private static LocationSet combine(LocationSet setA, LocationSet setB) {
        LocationSet result = new LocationSet();
        result.locations.addAll(setA.locations);
        result.locations.addAll(setB.locations);
        return result;
    }

    public static Collector<Station, LocationSet, LocationSet> stationCollector() {
        return new Collector<>() {
            @Override
            public Supplier<LocationSet> supplier() {
                return LocationSet::new;
            }

            @Override
            public BiConsumer<LocationSet, Station> accumulator() {
                return LocationSet::add;
            }

            @Override
            public BinaryOperator<LocationSet> combiner() {
                return LocationSet::combine;
            }

            @Override
            public Function<LocationSet, LocationSet> finisher() {
                return items -> items;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Sets.immutableEnumSet(Characteristics.UNORDERED);
            }
        };
    }

    public boolean isEmpty() {
        return locations.isEmpty();
    }

}
