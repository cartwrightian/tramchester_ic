package com.tramchester.graph.search.neo4j.selectors;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.geo.LocationDistances;
import com.tramchester.geo.StationsBoxSimpleGrid;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

    public BranchOrderingPolicy getFor(final LocationCollection destinations) {
        logger.info("creating for depthFirst " + config.getDepthFirst());

        //return getBreadthFirstBranchSelector(destinations);
        return config.getDepthFirst() ? SimpleDepthFirstBranchSelector::new : getBreadthFirstBranchSelector(destinations);
    }

    @SuppressWarnings("unchecked")
    private @NotNull BranchOrderingPolicy getBreadthFirstBranchSelector(final LocationCollection destinations) {
        return (start, expander) -> new DestinationDistanceBranchSelector(start, expander, locationDistances, destinations);
    }

    @SuppressWarnings("unchecked")
    public @NotNull BranchOrderingPolicy getForGrid(final StationsBoxSimpleGrid destinationBox, final List<StationsBoxSimpleGrid> boxes) {
        return (startBranch, expander) -> new BreadthFirstBranchSelectorForGridSearch(startBranch, expander,
                destinationBox, boxes);
    }


}
