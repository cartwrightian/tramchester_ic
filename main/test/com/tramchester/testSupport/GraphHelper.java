package com.tramchester.testSupport;

import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.core.GraphDirection;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.reference.TransportRelationshipTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class GraphHelper {
    private static final Logger logger = LoggerFactory.getLogger(GraphHelper.class);

    public static List<GraphRelationship> getRouteStationRelationships(GraphTransaction txn, RouteStation routeStation, GraphDirection graphDirection,
                                                                       EnumSet<TransportRelationshipTypes> relationshipTypes) {
        final GraphNode node = txn.findNode(routeStation);
        if (node==null) {
            logger.info("Did not find node for " + routeStation.getId());
            return Collections.emptyList();
        }

        return node.getRelationships(txn, graphDirection, relationshipTypes).toList();

    }

}
