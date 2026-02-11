package com.tramchester.domain.collections;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ImmutableEnumSet<T extends Enum<T>> implements Iterable<T> {
    private final EnumSet<T> contained;

    private ImmutableEnumSet(final EnumSet<T> contained) {
        this.contained = contained;
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> copyOf(final Set<S> set) {
        return new ImmutableEnumSet<>(EnumSet.copyOf(set));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> noneOf(Class<S> theClass) {
        return new ImmutableEnumSet<>(EnumSet.noneOf(theClass));
    }

    public static  <S extends Enum<S>> EnumSet<S> createEnumSet(final ImmutableEnumSet<S> labels) {
        return EnumSet.copyOf(labels.contained);
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> range(final S itemA, final S itemB) {
        return new ImmutableEnumSet<>(EnumSet.range(itemA, itemB));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> of(final S item) {
        return new ImmutableEnumSet<>(EnumSet.of(item));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> of(final S itemA, final S itemB) {
        return new ImmutableEnumSet<>(EnumSet.of(itemA, itemB));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> of(final S itemA, final S itemB, final S itemC) {
        return new ImmutableEnumSet<>(EnumSet.of(itemA, itemB, itemC));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> add(final ImmutableEnumSet<S> setA, final ImmutableEnumSet<S> setB) {
        final EnumSet<S> result = EnumSet.copyOf(setA.contained);
        result.addAll(setB.contained);
        return new ImmutableEnumSet<>(result);
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> allOf(Class<S> theClass) {
        return new ImmutableEnumSet<>(EnumSet.allOf(theClass));
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

    public Sets.SetView<T> intersectionWith(final Set<T> other) {
        return Sets.intersection(contained, other);
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

}
