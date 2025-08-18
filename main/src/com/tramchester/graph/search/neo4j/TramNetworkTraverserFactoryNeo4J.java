package com.tramchester.graph.search.neo4j;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.search.TramNetworkTraverser;
import com.tramchester.graph.search.TramNetworkTraverserFactory;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;

public class TramNetworkTraverserFactoryNeo4J implements TramNetworkTraverserFactory {
    private final TramchesterConfig config;
    private final BranchOrderingPolicy orderingPolicy;
    private final boolean fullLogging;
    private final LocationCollection destinations;

    public TramNetworkTraverserFactoryNeo4J(TramchesterConfig config, boolean fullLogging, BranchOrderingPolicy orderingPolicy, LocationCollection destinations) {
        this.config = config;
        this.fullLogging = fullLogging;
        this.orderingPolicy = orderingPolicy;
        this.destinations = destinations;

    }

    @Override
    public TramNetworkTraverser get(final GraphTransaction txn) {
        return new TramNetworkTraverserNeo4J(txn, config, fullLogging, orderingPolicy, destinations);
    }
}
