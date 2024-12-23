package com.tramchester.domain;

import com.google.common.collect.Sets;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocationSet<T extends Location<T>> implements LocationCollection {

    private final Set<T> locations;

    public LocationSet() {
        locations = new HashSet<>();
    }

    public LocationSet(Collection<T> locations) {
        this.locations = new HashSet<>(locations);
    }

    public LocationSet(LocationSet<T> locationSet) {
        this.locations = new HashSet<>(locationSet.locations);
    }

    public static  <T extends Location<T>> LocationSet<T> of(final Collection<T> items) {
        return new LocationSet<>(items);
    }

    public static <S extends Location<S>> LocationSet<S> singleton(S location) {
        final LocationSet<S> locationSet = new LocationSet<>();
        locationSet.add(location);
        return locationSet;
    }

    public int size() {
        return locations.size();
    }

    public Stream<T> stream() {
        return locations.stream();
    }

    public void add(T location) {
        locations.add(location);
    }

    public boolean contains(T item) {
        return locations.contains(item);
    }

    /***
     * Need to sort out issue around id's
     * @return only those locations in the set that are stations
     */
    @Deprecated
    public Stream<Station> stationsOnlyStream() {
        return locations.stream().
                filter(location -> location.getLocationType()==LocationType.Station).
                map(location -> (Station) location);
    }

    @Override
    public EnumSet<TransportMode> getModes() {
        return locations.stream().
                flatMap(location -> location.getTransportModes().stream()).
                collect(Collectors.toCollection(() -> EnumSet.noneOf(TransportMode.class)));
    }

    @Override
    public Stream<Location<?>> locationStream() {
        return locations.stream().map(item -> item);
    }

    public void addAll(final LocationSet<T> other) {
        this.locations.addAll(other.locations);
    }

    private static <S extends Location<S>> LocationSet<S> combine(final LocationSet<S> setA, final LocationSet<S> setB) {
        final LocationSet<S> result = new LocationSet<>();
        result.locations.addAll(setA.locations);
        result.locations.addAll(setB.locations);
        return result;
    }

    @Override
    public String toString() {
        return "LocationSet{" +
                "locations=" + asIds() +
                '}';
    }

    public String asIds() {
        final StringBuilder ids = new StringBuilder();
        ids.append("[");
        locations.forEach(item -> ids.append(" '").append(item.getId()).append("'"));
        ids.append("]");
        return ids.toString();
    }

    public static <S extends Location<S>> Collector<S, LocationSet<S>, LocationSet<S>> stationCollector() {
        return new Collector<>() {
            @Override
            public Supplier<LocationSet<S>> supplier() {
                return LocationSet::new;
            }

            @Override
            public BiConsumer<LocationSet<S>, S> accumulator() {
                return LocationSet::add;
            }

            @Override
            public BinaryOperator<LocationSet<S>> combiner() {
                return LocationSet::combine;
            }

            @Override
            public Function<LocationSet<S>, LocationSet<S>> finisher() {
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

    @Override
    public boolean contains(LocationId<?> locationId) {
        return locations.stream().anyMatch(location -> location.getLocationId().equals(locationId));
    }


}
