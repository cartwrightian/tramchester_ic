package com.tramchester.domain.collections;

import org.apache.commons.collections4.SetUtils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

public interface ImmutableEnumSet<T extends Enum<T>> extends Iterable<T> {

    static <S extends Enum<S>> EnumSet<S> createEnumSet(final ImmutableEnumSet<S> items) {
        return ImmutableEnumSetImpl.createEnumSet((ImmutableEnumSetImpl<S>)items);
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> join(ImmutableEnumSet<S> itemsA, ImmutableEnumSet<S> itemsB) {
        if (itemsB.isEmpty()) {
            return itemsA;
        }
        if (itemsA.isEmpty()) {
            return itemsB;
        }
        return ImmutableEnumSetImpl.join((ImmutableEnumSetImpl<S>)itemsA, (ImmutableEnumSetImpl<S>)itemsB);
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> copyOf(final Set<S> items) {
        return ImmutableEnumSetImpl.copyOf(items);
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> noneOf(Class<S> theClass) {
        return ImmutableEnumSetImpl.noneOf(theClass);
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> allOf(Class<S> theClass) {
        return ImmutableEnumSetImpl.allOf(theClass);
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> from(final List<S> list) {
        HashSet<S> set = new HashSet<>(list);
        return copyOf(set);
    }

    Stream<T> stream();

    boolean contains(T item);

    boolean containsAll(Collection<T> items);

    Set<T> intersectionWith(ImmutableEnumSet<T> other);

    int size();

    boolean isEmpty();

    boolean anyIntersectionWith(ImmutableEnumSet<T> other);

    boolean anyIntersectionWith(EnumSet<T> other);

    <D extends Enum<D>> ImmutableEnumSet<D> convertTo(Class<D> targetClass, Function<T, D> convert);

    void addAllTo(EnumSet<T> mutableTarget);

    ImmutableEnumSet<T> without(Set<T> remove);

    ImmutableEnumSet<T> without(ImmutableEnumSet<T> remove);

    static <S extends Enum<S>> ImmutableEnumSet<S> of(final S item) {
        return new ImmutableEnumSetImpl.One<>(item);
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> of(final S itemA, final S itemB) {
        return ImmutableEnumSetImpl.createFrom(EnumSet.of(itemA, itemB));
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> of(final S itemA, final S itemB, final S itemC) {
        return ImmutableEnumSetImpl.createFrom(EnumSet.of(itemA, itemB, itemC));
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> of(final S itemA, final S itemB, final S itemC, final S itemD) {
        return ImmutableEnumSetImpl.createFrom(EnumSet.of(itemA, itemB, itemC, itemD));
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> of(final S itemA, final S itemB, final S itemC, final S itemD, final S itemE) {
        return ImmutableEnumSetImpl.createFrom(EnumSet.of(itemA, itemB, itemC, itemD, itemE));
    }

    static <T extends Enum<T>> Collector<T, EnumSet<T>, ImmutableEnumSet<T>> collect(Class<T> klass) {
        return new Collector<>() {
            @Override
            public Supplier<EnumSet<T>> supplier() {
                return () -> EnumSet.noneOf(klass);
            }

            @Override
            public BiConsumer<EnumSet<T>, T> accumulator() {
                return AbstractCollection::add;
            }

            @Override
            public BinaryOperator<EnumSet<T>> combiner() {
                return (setA, setB) -> {
                    setA.addAll(setB);
                    return setA;
                };
            }

            @Override
            public Function<EnumSet<T>, ImmutableEnumSet<T>> finisher() {
                return set -> set.isEmpty() ? ImmutableEnumSet.noneOf(klass) : ImmutableEnumSetImpl.copyOf(set);
            }

            @Override
            public Set<Characteristics> characteristics() {
                return SetUtils.unmodifiableSet(Characteristics.UNORDERED);
            }
        };
    }


}
