package com.tramchester.graph.graphbuild.caching;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AgencyBuilderNodeCache implements ServiceNodeCache, HourNodeCache {
    private final ConcurrentMap<String, GraphNodeId> svcNodes;
    private final ConcurrentMap<GraphNodeIdAndHour, GraphNodeId> hourNodes;

    public AgencyBuilderNodeCache() {
        svcNodes = new ConcurrentHashMap<>();
        hourNodes = new ConcurrentHashMap<>();
    }

    @Override
    public void putService(IdFor<Route> routeId, Service service, IdFor<Station> begin, IdFor<Station> end, GraphNode svcNode) {
        String key = getServiceKey(routeId, service.getId(), begin, end);
        svcNodes.put(key, svcNode.getId());
    }

    // TODO This has to be route station to route Station
    @Override
    public MutableGraphNode getServiceNode(MutableGraphTransaction txn, IdFor<Route> routeId, Service service,
                                           IdFor<Station> startStation, IdFor<Station> endStation) {
        String id = getServiceKey(routeId, service.getId(), startStation, endStation);
        return txn.getNodeByIdMutable(svcNodes.get(id));
    }

    @Override
    public boolean hasServiceNode(IdFor<Route> routeId, Service service, IdFor<Station> begin, IdFor<Station> end) {
        return svcNodes.containsKey(getServiceKey(routeId, service.getId(), begin,end));
    }

    @Override
    public void putHour(GraphNodeId serviceNodeId, int hour, GraphNode hourNode) {
        GraphNodeIdAndHour key = new GraphNodeIdAndHour(serviceNodeId, hour);
        if (hourNodes.containsKey(key)) {
            throw new RuntimeException("Attempt to create duplicate hour node for " + key);
        }
        hourNodes.put(key, hourNode.getId());
    }

    @Override
    public boolean hasHourNode(GraphNodeId serviceNodeId, int hour) {
        final GraphNodeIdAndHour key = new GraphNodeIdAndHour(serviceNodeId, hour);
        return hourNodes.containsKey(key);
    }

    @Override
    public MutableGraphNode getHourNode(MutableGraphTransaction tx, GraphNodeId serviceNodeId, int hour) {
        GraphNodeIdAndHour key = new GraphNodeIdAndHour(serviceNodeId, hour);
        if (hourNodes.containsKey(key)) {
            return tx.getNodeByIdMutable(hourNodes.get(key));
        }
        throw new RuntimeException("Could not find hour node for " + key);
    }

    public void clear() {
        svcNodes.clear();
        hourNodes.clear();
    }

    protected static String getServiceKey(IdFor<Route> routeId, IdFor<Service> service,
                                          IdFor<Station> startStation, IdFor<Station> endStation) {
        return routeId.getGraphId()+"_"+startStation.getGraphId()+"_"+endStation.getGraphId()+"_"+ service.getGraphId();
    }

    private record GraphNodeIdAndHour(GraphNodeId graphNodeId, int hour) {

        @Override
            public String toString() {
                return "GraphNodeIdAndHour{" +
                        "graphNodeId=" + graphNodeId +
                        ", hour=" + hour +
                        '}';
            }
        }
}
