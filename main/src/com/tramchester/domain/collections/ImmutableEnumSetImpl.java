package com.tramchester.domain.collections;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ImmutableEnumSetImpl<T extends Enum<T>> implements ImmutableEnumSet<T> {

    protected abstract EnumSet<T> getContainedUnsafe();

    static <C extends Enum<C>> ImmutableEnumSetImpl<C> createFrom(final EnumSet<C> source) {
        if (source.isEmpty()) {
            throw new RuntimeException("Empty" + source);
        }
        if (source.size()==1) {
            C element = source.iterator().next();
            return new One<>(element);

        }
        return new Many<>(source);
    }

    static <S extends Enum<S>> ImmutableEnumSetImpl<S> copyOf(final Set<S> set) {
        return createFrom(EnumSet.copyOf(set));
    }

    static <S extends Enum<S>> ImmutableEnumSetImpl<S> noneOf(final Class<S> theClass) {
        return new None<>(theClass);
        //return createFrom(EnumSet.noneOf(theClass));
    }

    static <S extends Enum<S>> EnumSet<S> createEnumSet(final ImmutableEnumSetImpl<S> items) {
        return EnumSet.copyOf(items.getContainedUnsafe());
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> join(final ImmutableEnumSetImpl<S> setA, final ImmutableEnumSetImpl<S> setB) {
        if (setA.isEmpty()) {
            return setB;
        }
        if (setB.isEmpty()) {
            return setA;
        }
        final EnumSet<S> result = EnumSet.copyOf(setA.getContainedUnsafe());
        result.addAll(setB.getContainedUnsafe());
        return createFrom(result);
    }

    static <S extends Enum<S>> ImmutableEnumSet<S> allOf(final Class<S> theClass) {
        return createFrom(EnumSet.allOf(theClass));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj==null) {
            return false;
        }
        if (obj instanceof ImmutableEnumSetImpl<?> otherSet) {
            return otherSet.getContainedUnsafe().equals(this.getContainedUnsafe());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getContainedUnsafe().hashCode();
    }

    static class None<T extends Enum<T>> extends ImmutableEnumSetImpl<T> {

        private final EnumSet<T> noneOf;

        None(Class<T> theClass) {
            noneOf = EnumSet.noneOf(theClass);
        }

        @Override
        protected EnumSet<T> getContainedUnsafe() {
            return noneOf;
        }

        @Override
        public Stream<T> stream() {
            return Stream.empty();
        }

        @Override
        public boolean contains(T item) {
            return false;
        }

        @Override
        public Set<T> intersectionWith(final ImmutableEnumSet<T> other) {
            return Collections.emptySet();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean anyIntersectionWith(ImmutableEnumSet<T> other) {
            return false;
        }

        @Override
        public boolean anyIntersectionWith(EnumSet<T> other) {
            return false;
        }

        @Override
        public <D extends Enum<D>> ImmutableEnumSet<D> convertTo(Class<D> targetClass, Function<T, D> convert) {
            return ImmutableEnumSetImpl.noneOf(targetClass);
        }

        @Override
        public void addAllTo(final EnumSet<T> mutableTarget) {
            // no-op
        }

        @Override
        public ImmutableEnumSet<T> without(Set<T> remove) {
            return this;
        }

        @Override
        public @NotNull Iterator<T> iterator() {
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public T next() {
                    return null;
                }
            };
        }
    }

    static class One<T extends Enum<T>> extends ImmutableEnumSetImpl<T> {
        private final T item;
        private EnumSet<T> oneOf;

        public One(T item) {
            this.item = item;
            oneOf = null; // have to do this 'lazy' due to static inits elsewhere
        }

        @Override
        public String toString() {
            return "ImmutableEnumSet{" + item + '}';
        }

        @Override
        public EnumSet<T> getContainedUnsafe() {
            if (oneOf==null) {
                oneOf = EnumSet.of(item);
            }
            return oneOf;
        }

        @Override
        public Stream<T> stream() {
            return Stream.of(item);
        }

        @Override
        public boolean contains(final T item) {
            return this.item.equals(item);
        }

        @Override
        public Set<T> intersectionWith(final ImmutableEnumSet<T> other) {
            // only intersection can be if the other set contains item
            if (other.isEmpty()) {
                return Collections.emptySet();
            }

            if (other.contains(item)) {
                return Collections.singleton(item);
            } else {
                return Collections.emptySet();
            }
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
        public boolean anyIntersectionWith(final ImmutableEnumSet<T> other) {
            return other.contains(item);
        }

        @Override
        public boolean anyIntersectionWith(final EnumSet<T> other) {
            return other.contains(item);
        }

        @Override
        public <D extends Enum<D>> ImmutableEnumSetImpl<D> convertTo(final Class<D> targetClass, Function<T, D> convert) {
            return new One<>(convert.apply(item));
        }

        @Override
        public void addAllTo(final EnumSet<T> mutableTarget) {
            mutableTarget.add(item);
        }

        @Override
        public ImmutableEnumSet<T> without(final Set<T> remove) {
            if (remove.contains(item)) {
                final EnumSet<T> keepTypeInfo = EnumSet.of(item); // workaround
                keepTypeInfo.clear();
                return new Many<>(keepTypeInfo);
            } else {
                return new One<>(item);
            }
        }

        @Override
        public @NotNull Iterator<T> iterator() {
            return Collections.singleton(item).iterator();
        }

        @Override
        public void forEach(final Consumer<? super T> action) {
            action.accept(item);
        }
    }

    static class Many<T extends Enum<T>> extends ImmutableEnumSetImpl<T> {

        private final EnumSet<T> contained;

        @Override
        public EnumSet<T> getContainedUnsafe() {
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
            return Sets.intersection(contained, ((ImmutableEnumSetImpl<T>)other).getContainedUnsafe());
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
            return anyIntersectionWith(((ImmutableEnumSetImpl<T>)other).getContainedUnsafe());
        }

        @Override
        public boolean anyIntersectionWith(final EnumSet<T> other) {
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
        public <D extends Enum<D>> ImmutableEnumSetImpl<D> convertTo(final Class<D> targetClass, final Function<T, D> convert) {
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
                return ImmutableEnumSetImpl.createFrom(EnumSet.copyOf(filtered));
            }
        }

    }
}
