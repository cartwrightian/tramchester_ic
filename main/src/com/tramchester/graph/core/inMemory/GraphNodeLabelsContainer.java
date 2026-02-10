package com.tramchester.graph.core.inMemory;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.graph.reference.GraphLabel;

import java.util.EnumSet;

public class GraphNodeLabelsContainer {

    private ImmutableEnumSet<GraphLabel> labels;
    private final GraphNodeInMemory parent;

    public GraphNodeLabelsContainer(final GraphNodeInMemory parent, final ImmutableEnumSet<GraphLabel> labels) {
        this.labels = labels;
        this.parent = parent;
    }

    public boolean contains(final GraphLabel graphLabel) {
        return labels.contains(graphLabel);
    }

    public ImmutableEnumSet<GraphLabel> getLabels() {
        return labels;
    }

    public void add(final GraphLabel label) {
        final EnumSet<GraphLabel> updated = ImmutableEnumSet.createEnumSet(labels);
        updated.add(label);
        labels = ImmutableEnumSet.copyOf(updated);
        parent.invalidateCache();
    }

    @Override
    public String toString() {
        return "GraphNodeLabelsContainer{" +
                "labels=" + labels +
                ", parent=" + parent.getId() +
                '}';
    }
}
