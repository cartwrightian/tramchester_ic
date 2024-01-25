package com.tramchester.graph.search.selectors;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.geo.StationDistances;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BranchSelectorFactory {
    private static final Logger logger = LoggerFactory.getLogger(BranchSelectorFactory.class);

    public static BranchOrderingPolicy getFor(TramchesterConfig config, StationDistances stationDistances, LocationSet destinations) {
        logger.info("creating for depthFirst " + config.getDepthFirst());

        return config.getDepthFirst() ? DepthFirstBranchSelector::new :
                (start, expander) -> new BreadthFirstBranchSelector(start, expander, stationDistances, destinations);
    }
}
