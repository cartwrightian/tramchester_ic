package com.tramchester.graph.search.selectors;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.geo.StationDistances;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@LazySingleton
public class BranchSelectorFactory {
    private static final Logger logger = LoggerFactory.getLogger(BranchSelectorFactory.class);

    private final TramchesterConfig config;
    private final StationDistances stationDistances;

    @Inject
    public BranchSelectorFactory(final TramchesterConfig config, final StationDistances stationDistances) {
        this.config = config;
        this.stationDistances = stationDistances;
    }

    public BranchOrderingPolicy getFor(final LocationSet destinations) {
        logger.info("creating for depthFirst " + config.getDepthFirst());

        return config.getDepthFirst() ? DepthFirstBranchSelector::new :
                (start, expander) -> new BreadthFirstBranchSelector(start, expander, stationDistances, destinations);
    }
}
