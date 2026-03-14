package com.tramchester.domain.collections;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ImmutableEnumSet<T extends Enum<T>> implements Iterable<T> {

    abstract EnumSet<T> getContained();

    private static <C extends Enum<C>> ImmutableEnumSet<C> createFrom(final EnumSet<C> source) {
        return new Many<>(source);
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> copyOf(final Set<S> set) {
        return createFrom(EnumSet.copyOf(set));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> noneOf(final Class<S> theClass) {
        return createFrom(EnumSet.noneOf(theClass));
    }

    public static <S extends Enum<S>> EnumSet<S> createEnumSet(final ImmutableEnumSet<S> labels) {
        return EnumSet.copyOf(labels.getContained());
    }

    /***
     * Use singleton() where possible
     * @param item item to place into Set
     * @return Immutable Enum Set
     * @param <S> Must be an Enum
     */
    public static <S extends Enum<S>> ImmutableEnumSet<S> of(final S item) {
        return new One<>(item);
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> of(final S itemA, final S itemB) {
        return createFrom(EnumSet.of(itemA, itemB));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> of(final S itemA, final S itemB, final S itemC) {
        return createFrom(EnumSet.of(itemA, itemB, itemC));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> join(final ImmutableEnumSet<S> setA, final ImmutableEnumSet<S> setB) {
        final EnumSet<S> result = EnumSet.copyOf(setA.getContained());
        result.addAll(setB.getContained());
        return createFrom(result);
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> allOf(final Class<S> theClass) {
        return createFrom(EnumSet.allOf(theClass));
    }

    public static <S extends Enum<S>> ImmutableEnumSet<S> range(final S begin, final S end) {
        return createFrom(EnumSet.range(begin, end));
    }

    public abstract Stream<T> stream();

    public abstract boolean contains(T item);

    public abstract Sets.SetView<T> intersectionWith(ImmutableEnumSet<T> other);

    public abstract int size();

    public abstract boolean isEmpty();

    public abstract boolean anyIntersectionWith(ImmutableEnumSet<T> other);

    public abstract boolean anyIntersectionWith(Set<T> other);

    public abstract <D extends Enum<D>> ImmutableEnumSet<D> convertTo(Class<D> targetClass, Function<T, D> convert);

    public abstract void addAllTo(EnumSet<T> mutableTarget);

    public abstract ImmutableEnumSet<T> without(Set<T> remove);

    @Override
    public boolean equals(Object obj) {
        if (obj==null) {
            return false;
        }
        if (obj instanceof ImmutableEnumSet<?> otherSet) {
            return otherSet.getContained().equals(this.getContained());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getContained().hashCode();
    }

    static class One<T extends Enum<T>> extends ImmutableEnumSet<T> {
        private final T item;

        public One(T item) {
            this.item = item;
        }

        @Override
        public String toString() {
            return "ImmutableEnumSet{" + item + '}';
        }

        @Override
        EnumSet<T> getContained() {
            return EnumSet.of(item);
        }

        @Override
        public Stream<T> stream() {
            return Stream.of(item);
        }

        @Override
        public boolean contains(T item) {
            return this.item.equals(item);
        }

        @Override
        public Sets.SetView<T> intersectionWith(ImmutableEnumSet<T> other) {
            return other.intersectionWith(this);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean anyIntersectionWith(ImmutableEnumSet<T> other) {
            return other.contains(item);
        }

        @Override
        public boolean anyIntersectionWith(Set<T> other) {
            return other.contains(item);
        }

        @Override
        public <D extends Enum<D>> ImmutableEnumSet<D> convertTo(Class<D> targetClass, Function<T, D> convert) {
            return new One<>(convert.apply(item));
        }

        @Override
        public void addAllTo(final EnumSet<T> mutableTarget) {
            mutableTarget.add(item);
        }

        @Override
        public ImmutableEnumSet<T> without(final Set<T> remove) {
            if (remove.contains(item)) {
                final EnumSet<T> keepTypeInfo = EnumSet.of(item);
                keepTypeInfo.clear();
                return new Many<>(keepTypeInfo);
            } else {
                return new One<>(item);
            }
        }

        @Override
        public @NotNull Iterator<T> iterator() {
            return EnumSet.of(item).iterator();
        }

        @Override
        public void forEach(Consumer<? super T> action) {
            action.accept(item);
        }
    }

    static class Many<T extends Enum<T>> extends ImmutableEnumSet<T> {

        private final EnumSet<T> contained;

        @Override
        EnumSet<T> getContained() {
            return contained;
        }

        private Many(final EnumSet<T> contained) {
            this.contained = contained;
        }

        @Override
        public Stream<T> stream() {
            return contained.stream();
        }

        @Override
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

        @Override
        public Sets.SetView<T> intersectionWith(final ImmutableEnumSet<T> other) {
            return Sets.intersection(contained, other.getContained());
        }

        @Override
        public int size() {
            return contained.size();
        }

        @Override
        public boolean isEmpty() {
            return contained.isEmpty();
        }

        @Override
        public String toString() {
            return "ImmutableEnumSet{" +
                    contained.toString() +
                    '}';
        }

        @Override
        public boolean anyIntersectionWith(final ImmutableEnumSet<T> other) {
            return anyIntersectionWith(other.getContained());
        }

        @Override
        public boolean anyIntersectionWith(final Set<T> other) {
            for (final T item : other) {
                if (contains(item)) {
                    return true;
                }
            }
            return false;

            // slow
            //return !SetUtils.intersection(modesA, modesB).isEmpty();
        }

        @Override
        public <D extends Enum<D>> ImmutableEnumSet<D> convertTo(final Class<D> targetClass, final Function<T, D> convert) {
            final EnumSet<D> converted = contained.stream().
                    map(convert).
                    collect(Collectors.toCollection(() -> EnumSet.noneOf(targetClass)));
            return new Many<>(converted);
        }

        @Override
        public void addAllTo(final EnumSet<T> mutableTarget) {
            mutableTarget.addAll(contained);
        }

        @Override
        public ImmutableEnumSet<T> without(final Set<T> remove) {
            final List<T> filtered = new ArrayList<>(contained);
            filtered.removeAll(remove);
            if (filtered.size()==1) {
                return new One<>(filtered.getFirst());
            } else {
                return ImmutableEnumSet.createFrom(EnumSet.copyOf(filtered));
            }
        }

    }
}
