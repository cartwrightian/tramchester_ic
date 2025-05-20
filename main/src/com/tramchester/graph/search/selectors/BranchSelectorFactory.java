package com.tramchester.graph.search.selectors;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.geo.LocationDistances;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LazySingleton
public class BranchSelectorFactory {
    private static final Logger logger = LoggerFactory.getLogger(BranchSelectorFactory.class);

    private final TramchesterConfig config;
    private final LocationDistances locationDistances;

    @Inject
    public BranchSelectorFactory(final TramchesterConfig config, final LocationDistances locationDistances) {
        this.config = config;
        this.locationDistances = locationDistances;
    }

    @SuppressWarnings("unchecked")
    public BranchOrderingPolicy getFor(final LocationCollection destinations) {
        logger.info("creating for depthFirst " + config.getDepthFirst());

        return config.getDepthFirst() ? SimpleDepthFirstBranchSelector::new : getBreadthFirstBranchSelector(destinations);
    }

    @SuppressWarnings("unchecked")
    private @NotNull BranchOrderingPolicy getBreadthFirstBranchSelector(final LocationCollection destinations) {
        return (start, expander) -> new DestinationDistanceBranchSelector(start, expander, locationDistances, destinations);
    }


}
