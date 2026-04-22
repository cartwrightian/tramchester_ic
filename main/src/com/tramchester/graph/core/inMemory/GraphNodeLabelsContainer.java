package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.MutableGraphTransaction;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.reference.GraphLabels;

import java.util.EnumSet;

public class GraphNodeLabelsContainer {

    private GraphLabels labels;
    private final GraphNodeInMemory parent;

    public GraphNodeLabelsContainer(final GraphNodeInMemory parent, final GraphLabels labels) {
        this.labels = labels;
        this.parent = parent;
    }

    public boolean contains(final GraphLabel graphLabel) {
        return labels.contains(graphLabel);
    }

    public GraphLabels getLabels() {
        return labels;
    }

    public void add(final MutableGraphTransaction txn, final GraphLabel toAdd) {
        labels = txn.updateLabels(labels, toAdd); // label.addTo(labels);
        // needed to ensure Node marked as Dirty
        parent.invalidateCache();
    }

    @Override
    public String toString() {
        return "GraphNodeLabelsContainer{" +
                "labels=" + labels +
                ", parent=" + parent.getId() +
                '}';
    }

    public EnumSet<GraphLabel> createEnumSet() {
        return labels.createEnumSet();
    }
}
