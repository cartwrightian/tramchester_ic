package com.tramchester.integration.testSupport;

import com.tramchester.domain.places.Location;
import org.apache.commons.collections4.SetUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class LocationIdsAndNames<T extends Location<T>> {
    private final Set<LocationIdAndNamePair<T>> items;

    public LocationIdsAndNames() {
        this(new HashSet<>());
    }

    public LocationIdsAndNames(Set<LocationIdAndNamePair<T>> items) {
        this.items = items;
    }

    public static <T extends Location<T>> Collector<LocationIdAndNamePair<T>, LocationIdsAndNames<T>, LocationIdsAndNames<T>> collect() {
        return new Collector<>() {
            @Override
            public Supplier<LocationIdsAndNames<T>> supplier() {
                return LocationIdsAndNames::new;
            }

            @Override
            public BiConsumer<LocationIdsAndNames<T>, LocationIdAndNamePair<T>> accumulator() {
                return LocationIdsAndNames::add;
            }

            @Override
            public BinaryOperator<LocationIdsAndNames<T>> combiner() {
                return LocationIdsAndNames::addAll;
            }

            @Override
            public Function<LocationIdsAndNames<T>, LocationIdsAndNames<T>> finisher() {
                return items -> items;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return SetUtils.unmodifiableSet(Characteristics.UNORDERED);
            }
        };
    }

    private LocationIdsAndNames<T> addAll(LocationIdsAndNames<T> otherItems) {
        this.items.addAll(otherItems.items);
        return this;
    }

    private void add(LocationIdAndNamePair<T> item) {
        items.add(item);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
