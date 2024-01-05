package com.tramchester.graph.graphbuild.caching;

import com.tramchester.graph.facade.GraphNodeId;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BoardingDepartNodeCache {

    private final ConcurrentMap<GraphNodeId, Set<GraphNodeId>> boardings;
    private final ConcurrentMap<GraphNodeId, Set<GraphNodeId>> departs;

    public BoardingDepartNodeCache() {
        boardings = new ConcurrentHashMap<>();
        departs = new ConcurrentHashMap<>();
    }

    public void clear() {
        boardings.clear();
        departs.clear();
    }

    public void putBoarding(GraphNodeId platformOrStation, GraphNodeId routeStationNodeId) {
        putRelationship(boardings, platformOrStation, routeStationNodeId);
    }

    public boolean hasBoarding(GraphNodeId platformOrStation, GraphNodeId routeStationNodeId) {
        return hasRelationship(boardings, platformOrStation, routeStationNodeId);
    }

    public boolean hasDeparts(GraphNodeId platformOrStation, GraphNodeId routeStationNodeId) {
        return hasRelationship(departs, platformOrStation, routeStationNodeId);
    }

    public void putDepart(GraphNodeId boardingNodeId, GraphNodeId routeStationNodeId) {
        putRelationship(departs, boardingNodeId, routeStationNodeId);
    }

    private void putRelationship(Map<GraphNodeId, Set<GraphNodeId>> relationshipCache, GraphNodeId boardingNodeId, GraphNodeId routeStationNodeId) {
        if (relationshipCache.containsKey(boardingNodeId)) {
            relationshipCache.get(boardingNodeId).add(routeStationNodeId);
        } else {
            HashSet<GraphNodeId> set = new HashSet<>();
            set.add(routeStationNodeId);
            relationshipCache.put(boardingNodeId, set);
        }
    }

    private boolean hasRelationship(Map<GraphNodeId, Set<GraphNodeId>> relationshipCache,  GraphNodeId boardingNodeId, GraphNodeId routeStationNodeId) {
        if (relationshipCache.containsKey(boardingNodeId)) {
            return relationshipCache.get(boardingNodeId).contains(routeStationNodeId);
        }
        return false;
    }
}
