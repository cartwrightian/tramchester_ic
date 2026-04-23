package com.tramchester.domain.collections;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;
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

    static <S extends Enum<S>> ImmutableEnumSet<S> copyOf(Set<S> items) {
        return ImmutableEnumSetImpl.copyOf(items);
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> noneOf(Class<S> theClass) {
        return ImmutableEnumSetImpl.noneOf(theClass);
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> allOf(Class<S> theClass) {
        return ImmutableEnumSetImpl.allOf(theClass);
    }

    Stream<T> stream();

    boolean contains(T item);

    Set<T> intersectionWith(ImmutableEnumSet<T> other);

    int size();

    boolean isEmpty();

    boolean anyIntersectionWith(ImmutableEnumSet<T> other);

    boolean anyIntersectionWith(EnumSet<T> other);

    <D extends Enum<D>> ImmutableEnumSet<D> convertTo(Class<D> targetClass, Function<T, D> convert);

    void addAllTo(EnumSet<T> mutableTarget);

    ImmutableEnumSet<T> without(Set<T> remove);

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
}
