package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;

import javax.inject.Inject;

/***
 * TODO Into GraphTransaction Facade??
 * Make sure have correct dependencies on "Ready" tokens alongside this class, it makes no guarantees for any
 * data having put in the DB.
 * It can't have a ready token injected as this would create circular dependencies.
 */
@LazySingleton
public class GraphQuery {

    @Inject
    public GraphQuery() {
    }

    public boolean hasAnyNodesWithLabelAndId(GraphTransaction txn, GraphLabel label, String field, String value) {
        return txn.hasAnyMatching(label, field, value);

    }
}
