package com.tramchester.graph.search;

import com.tramchester.graph.core.GraphTransaction;

public interface TramNetworkTraverserFactory {
    TramNetworkTraverser get(GraphTransaction txn);
}
