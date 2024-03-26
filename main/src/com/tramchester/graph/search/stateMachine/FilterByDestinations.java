package com.tramchester.graph.search.stateMachine;

import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.internal.helpers.collection.Iterables;

import java.util.Collection;

public class FilterByDestinations<T> implements ResourceIterable<T> {

    private final ResourceIterable<T> contained;
    private final boolean empty;

    public static <T> FilterByDestinations<T> from(final Collection<T> collection) {
        return new FilterByDestinations<>(collection);
    }

    public FilterByDestinations(final Collection<T> collection) {
        empty = collection.isEmpty();
        contained = Iterables.asResourceIterable(collection);
    }

    @Override
    public @NotNull ResourceIterator<T> iterator() {
        return contained.iterator();
    }

    @Override
    public void close() {
        contained.close();
    }

    public boolean isEmpty() {
        return empty;
    }
}
