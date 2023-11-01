package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphdb.Direction;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

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

    /**
     * When calling from tests make sure relevant DB is fully built
     */
    public List<GraphRelationship> getRouteStationRelationships(GraphTransaction txn, RouteStation routeStation, Direction direction) {
        GraphNode routeStationNode = txn.findNode(routeStation);
        if (routeStationNode==null) {
            return Collections.emptyList();
        }
        return routeStationNode.getRelationships(txn, direction, TransportRelationshipTypes.forPlanning()).toList();
    }

    public boolean hasAnyNodesWithLabelAndId(GraphTransaction txn, GraphLabel label, String field, String value) {
        return txn.hasAnyMatching(label, field, value);

    }
}
