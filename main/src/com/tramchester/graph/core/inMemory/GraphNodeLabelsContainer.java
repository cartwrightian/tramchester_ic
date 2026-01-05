package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.reference.GraphLabel;

import java.util.EnumSet;

public class GraphNodeLabelsContainer {

    private final EnumSet<GraphLabel> labels;
    private final GraphNodeInMemory parent;

    public GraphNodeLabelsContainer(final GraphNodeInMemory parent, final EnumSet<GraphLabel> labels) {
        this.labels = labels;
        this.parent = parent;
    }

    public boolean contains(final GraphLabel graphLabel) {
        return labels.contains(graphLabel);
    }

    public EnumSet<GraphLabel> getLabels() {
        return labels;
    }

    public void add(GraphLabel label) {
        labels.add(label);
        parent.invalidateCache();
    }
}
