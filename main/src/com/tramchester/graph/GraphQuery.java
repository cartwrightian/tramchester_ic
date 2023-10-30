package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
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
    public GraphNode getRouteStationNode(GraphTransaction txn, RouteStation routeStation) {
        return txn.findNode(routeStation);
    }

    /**
     * When calling from tests make sure relevant DB is fully built
     */
    public GraphNode getStationNode(GraphTransaction txn, Station station) {
        return txn.findNode(station);
    }

    public GraphNode getGroupedNode(GraphTransaction txn, StationGroup stationGroup) {
        // uses Area Id, not station Id
        // TODO make this change to GroupedStations?
        return txn.findNode(stationGroup);
    }

    public GraphNode getLocationNode(GraphTransaction txn, Location<?> location) {
        return txn.findNode(location);
    }

    /**
     * When calling from tests make sure relevant DB is fully built
     */
    public List<GraphRelationship> getRouteStationRelationships(GraphTransaction txn, RouteStation routeStation, Direction direction) {
        GraphNode routeStationNode = getRouteStationNode(txn, routeStation);
        if (routeStationNode==null) {
            return Collections.emptyList();
        }
        return routeStationNode.getRelationships(direction, TransportRelationshipTypes.forPlanning()).toList();
    }

    public boolean hasAnyNodesWithLabelAndId(GraphTransaction txn, GraphLabel label, String field, String value) {
        return txn.hasAnyMatching(label, field, value);

    }

    public GraphNode getPlatformNode(GraphTransaction txn, Platform platform) {
        return txn.findNode(platform);
    }

}
