package com.tramchester.domain.collections;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImmutableEnumSet<T extends Enum<T>> implements Iterable<T> {
    private final EnumSet<T> contained;

    private ImmutableEnumSet(final EnumSet<T> contained) {
        this.contained = contained;
    }

    private static <C extends Enum<C>> ImmutableEnumSet<C> createFrom(final EnumSet<C> source) {
        return new ImmutableEnumSet<>(source);
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> copyOf(final Set<S> set) {
        return createFrom(EnumSet.copyOf(set));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> noneOf(Class<S> theClass) {
        return createFrom(EnumSet.noneOf(theClass));
    }

    public static  <S extends Enum<S>> EnumSet<S> createEnumSet(final ImmutableEnumSet<S> labels) {
        return EnumSet.copyOf(labels.contained);
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> range(final S begin, final S end
    ) {
        return createFrom(EnumSet.range(begin, end));
    }

    /***
     * Use singleton() where possible
     * @param item item to place into Set
     * @return Immutable Enum Set
     * @param <S> Must be an Enum
     */
    public static <S extends Enum<S>> ImmutableEnumSet<S> of(final S item) {
        return createFrom(EnumSet.of(item));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> of(final S itemA, final S itemB) {
        return createFrom(EnumSet.of(itemA, itemB));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> of(final S itemA, final S itemB, final S itemC) {
        return createFrom(EnumSet.of(itemA, itemB, itemC));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> join(final ImmutableEnumSet<S> setA, final ImmutableEnumSet<S> setB) {
        final EnumSet<S> result = EnumSet.copyOf(setA.contained);
        result.addAll(setB.contained);
        return createFrom(result);
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> allOf(final Class<S> theClass) {
        return createFrom(EnumSet.allOf(theClass));
    }

    public Stream<T> stream() {
        return contained.stream();
    }

    public boolean contains(final T item) {
        return contained.contains(item);
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return contained.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        contained.forEach(action);
    }

    public Sets.SetView<T> intersectionWith(final ImmutableEnumSet<T> other) {
        return Sets.intersection(contained, other.contained);
    }

    public int size() {
        return contained.size();
    }

    public boolean isEmpty() {
        return contained.isEmpty();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        ImmutableEnumSet<?> that = (ImmutableEnumSet<?>) object;
        return Objects.equals(contained, that.contained);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(contained);
    }

    @Override
    public String toString() {
        return "ImmutableEnumSet{" +
                contained.toString() +
                '}';
    }

    public boolean anyIntersectionWith(final ImmutableEnumSet<T> other) {
        return anyIntersectionWith(other.contained);
    }

    public boolean anyIntersectionWith(final Set<T> other) {
        for (final T item:other) {
            if (contains(item)) {
                return true;
            }
        }
        return false;

        // slow
        //return !SetUtils.intersection(modesA, modesB).isEmpty();
    }

    public <D extends Enum<D>> ImmutableEnumSet<D> convertTo(final Class<D> targetClass, final Function<T,D> convert) {
        final EnumSet<D> converted = contained.stream().
                map(convert).
                collect(Collectors.toCollection(() -> EnumSet.noneOf(targetClass)));
        return new ImmutableEnumSet<>(converted);
    }

    public void addAllTo(final EnumSet<T> mutableTarget) {
        mutableTarget.addAll(contained);
    }

    public ImmutableEnumSet<T> without(final Set<T> remove) {
        final EnumSet<T> filtered = EnumSet.copyOf(contained);
        filtered.removeAll(remove);
        return new ImmutableEnumSet<>(filtered);
    }
}
