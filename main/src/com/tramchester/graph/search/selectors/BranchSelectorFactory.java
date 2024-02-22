package com.tramchester.graph.search.selectors;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.geo.LocationDistances;
import com.tramchester.geo.StationsBoxSimpleGrid;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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

        return config.getDepthFirst() ? DepthFirstBranchSelector::new :
                (start, expander) -> new BreadthFirstBranchSelector(start, expander, locationDistances, destinations);
    }

    public BranchOrderingPolicy getFor(StationsBoxSimpleGrid destinationBox, List<StationsBoxSimpleGrid> startingBoxes) {
        return (startBranch, expander) -> new BreadthFirstBranchSelectorForGridSearch(startBranch, expander,
                destinationBox, startingBoxes);
    }
}
