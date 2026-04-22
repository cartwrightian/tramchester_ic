package com.tramchester.graph.reference;

import com.tramchester.domain.collections.ImmutableEnumSet;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class GraphLabels implements Iterable<GraphLabel> {
    private final ImmutableEnumSet<GraphLabel> theLabels;

    private GraphLabels(final ImmutableEnumSet<GraphLabel> theLabels) {
        this.theLabels = theLabels;
    }

    public static GraphLabels from(final EnumSet<GraphLabel> labels) {
        return new GraphLabels(ImmutableEnumSet.copyOf(labels));
    }

    public static GraphLabels from(final ImmutableEnumSet<GraphLabel> labels) {
        return new GraphLabels(labels);
    }

    public static GraphLabels empty() {
        return from(ImmutableEnumSet.noneOf(GraphLabel.class));
    }

    public boolean contains(GraphLabel label) {
        return theLabels.contains(label);
    }

//    public GraphLabels add(final GraphLabel graphLabel) {
//        final EnumSet<GraphLabel> updated = ImmutableEnumSet.createEnumSet(theLabels);
//        updated.add(graphLabel);
//        theLabels = ImmutableEnumSet.copyOf(updated);
//        return this;
//    }

    public EnumSet<GraphLabel> createEnumSet() {
        return ImmutableEnumSet.createEnumSet(theLabels);
    }

    public Stream<GraphLabel> stream() {
        return theLabels.stream();
    }

    @Override
    public @NotNull Iterator<GraphLabel> iterator() {
        return theLabels.iterator();
    }

    @Override
    public void forEach(final Consumer<? super GraphLabel> action) {
        theLabels.forEach(action);
    }
    @Override
    public String toString() {
        return "GraphLabels{" +
                theLabels +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        GraphLabels that = (GraphLabels) object;
        return Objects.equals(theLabels, that.theLabels);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(theLabels);
    }

    public int size() {
        return theLabels.size();
    }

    public GraphLabels without(final EnumSet<GraphLabel> toExclude) {
        return new GraphLabels(theLabels.without(toExclude));
    }

    public boolean anyIntersectionWith(final ImmutableEnumSet<GraphLabel> labels) {
        return theLabels.anyIntersectionWith(labels);
    }

    ImmutableEnumSet<GraphLabel> contained() {
        return theLabels;
    }

    public boolean isEmpty() {
        return theLabels.isEmpty();
    }
}
