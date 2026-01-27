package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.CoreDomain;
import org.apache.commons.collections4.SetUtils;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

public interface ImmutableIdSet<T extends CoreDomain> extends Iterable<IdFor<T>> {
    int size();

    boolean contains(IdFor<T> id);

    ImmutableIdSet<T> createAppend(IdFor<T> id);

    ImmutableIdSet<T> createRemove(IdFor<T> id);

    @JsonIgnore
    boolean isEmpty();

    Stream<IdFor<T>> stream();

    boolean containsAll(ImmutableIdSet<T> other);

    boolean containsNoneOf(ImmutableIdSet<T> other);

    static <T extends HasId<T> & CoreDomain> Collector<T, IdSet<T>, ImmutableIdSet<T>> collector() {
        return new Collector<>() {
            @Override
            public Supplier<IdSet<T>> supplier() {
                return IdSet::new;
            }

            @Override
            public BiConsumer<IdSet<T>, T> accumulator() {
                return (theSet, item) -> theSet.add(item.getId());
            }

            @Override
            public BinaryOperator<IdSet<T>> combiner() {
                return IdSet::addAll;
            }

            @Override
            public Function<IdSet<T>, ImmutableIdSet<T>> finisher() {
                return items -> items;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return SetUtils.unmodifiableSet(Characteristics.UNORDERED);
            }
        };
    }

}
