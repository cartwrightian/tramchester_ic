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

import static java.lang.String.format;

public class AgencyBuilderNodeCache implements ServiceNodeCache, HourNodeCache {
    private final ConcurrentMap<String, GraphNodeId> svcNodes;
    private final ConcurrentMap<String, GraphNodeId> hourNodes;

    public AgencyBuilderNodeCache() {
        svcNodes = new ConcurrentHashMap<>();
        hourNodes = new ConcurrentHashMap<>();
    }

    @Override
    public void putService(IdFor<Route> routeId, Service service, IdFor<Station> begin, IdFor<Station> end, GraphNode svcNode) {
        String key = CreateKeys.getServiceKey(routeId, service.getId(), begin, end);
        svcNodes.put(key, svcNode.getId());
    }

    // TODO This has to be route station to route Station
    @Override
    public MutableGraphNode getServiceNode(MutableGraphTransaction txn, IdFor<Route> routeId, Service service,
                                           IdFor<Station> startStation, IdFor<Station> endStation) {
        String id = CreateKeys.getServiceKey(routeId, service.getId(), startStation, endStation);
        return txn.getNodeByIdMutable(svcNodes.get(id));
    }

    @Override
    public void putHour(final IdFor<Route> routeId, final Service service, final IdFor<Station> startId, final Integer hour, final GraphNode node) {
        final String hourKey = CreateKeys.getHourKey(routeId, service.getId(), startId, hour);
        hourNodes.put(hourKey, node.getId());
    }

    @Override
    public MutableGraphNode getHourNode(MutableGraphTransaction txn, IdFor<Route> routeId, Service service, IdFor<Station> station, Integer hour) {
        String key = CreateKeys.getHourKey(routeId, service.getId(), station, hour);
        if (!hourNodes.containsKey(key)) {
            throw new RuntimeException(format("Missing hour node for key %s service %s station %s hour %s",
                    key, service.getId(), station, hour));
        }
        return txn.getNodeByIdMutable(hourNodes.get(key));
    }

    @Override
    public boolean hasServiceNode(IdFor<Route> routeId, Service service, IdFor<Station> begin, IdFor<Station> end) {
        return svcNodes.containsKey(CreateKeys.getServiceKey(routeId, service.getId(), begin,end));
    }

    @Override
    public boolean hasHourNode(IdFor<Route> routeId, Service service, IdFor<Station> startId, Integer hour) {
        return hourNodes.containsKey(CreateKeys.getHourKey(routeId, service.getId(), startId, hour));
    }

    public void clear() {
        svcNodes.clear();
        hourNodes.clear();
    }

    private static class CreateKeys {

        protected static String getServiceKey(IdFor<Route> routeId, IdFor<Service> service,
                                              IdFor<Station> startStation, IdFor<Station> endStation) {
            return routeId.getGraphId()+"_"+startStation.getGraphId()+"_"+endStation.getGraphId()+"_"+ service.getGraphId();
        }

        @Deprecated
        protected static String getHourKey(IdFor<Route> routeId, IdFor<Service> service, IdFor<Station> station, Integer hour) {
            return routeId.getGraphId()+"_"+service.getGraphId()+"_"+station.getGraphId()+"_"+hour.toString();
        }

    }
}
