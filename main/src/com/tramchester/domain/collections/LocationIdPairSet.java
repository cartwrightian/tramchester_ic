package com.tramchester.domain.collections;

import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.places.Location;
import org.apache.commons.collections4.SetUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class LocationIdPairSet<T extends Location<T>>  {
    private final Set<LocationIdPair<T>> pairs;

    public LocationIdPairSet() {
        pairs = new HashSet<>();
    }

    private LocationIdPairSet<T> addAll(LocationIdPairSet<T> other) {
        this.pairs.addAll(other.pairs);
        return this;
    }

    public boolean isEmpty() {
        return pairs.isEmpty();
    }

    public Stream<LocationIdPair<T>> stream() {
        return pairs.stream();
    }

    public void add(LocationIdPair<T> locationIdPair) {
        pairs.add(locationIdPair);
    }

    public Stream<LocationIdPair<T>> parallelStream() {
        return pairs.parallelStream();
    }

    public int size() {
        return pairs.size();
    }

    public static <T extends Location<T>> Collector<LocationIdPair<T>, LocationIdPairSet<T>, LocationIdPairSet<T>> collector() {
        return new Collector<>() {
            @Override
            public Supplier<LocationIdPairSet<T>> supplier() {
                return LocationIdPairSet::new;
            }

            @Override
            public BiConsumer<LocationIdPairSet<T>, LocationIdPair<T>> accumulator() {
                return LocationIdPairSet::add;
            }

            @Override
            public BinaryOperator<LocationIdPairSet<T>> combiner() {
                return LocationIdPairSet::addAll;
            }

            @Override
            public Function<LocationIdPairSet<T>, LocationIdPairSet<T>> finisher() {
                return items -> items;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return SetUtils.unmodifiableSet(Characteristics.UNORDERED);
            }
        };
    }

    @Override
    public String toString() {
        return "LocationIdPairSet{" +
                "pairs=" + pairs +
                '}';
    }
}
