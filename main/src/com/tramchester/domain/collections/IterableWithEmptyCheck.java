package com.tramchester.domain.collections;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;


// TODO Only ever use with GraphRelationship for T?
public class IterableWithEmptyCheck<T> implements Iterable<T> {

    private final Collection<T> contained;

    public IterableWithEmptyCheck(final Collection<T> contained) {
        this.contained = contained;
    }

    public static <R> IterableWithEmptyCheck<R> from(final Collection<R> collection) {
        return new IterableWithEmptyCheck<>(collection);
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return contained.iterator();
    }

    public boolean isEmpty() {
        return contained.isEmpty();
    }

    public Stream<T> stream() {
        return contained.stream();
    }

    @Override
    public String toString() {
        return "IterableWithEmptyCheck{" +
                "contained=" + contained +
                '}';
    }
}
