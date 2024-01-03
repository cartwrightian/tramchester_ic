package com.tramchester.graph.graphbuild.caching;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.facade.MutableGraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

@LazySingleton
public class GraphBuilderCache implements HourNodeCache, RouteStationNodeCache, ServiceNodeCache, BoardingDepartNodeCache, StationAndPlatformNodeCache {
    private static final Logger logger = LoggerFactory.getLogger(GraphBuilderCache.class);

    private boolean cleared;
    private final Map<IdFor<RouteStation>, GraphNodeId> routeStations;
    private final Map<IdFor<Station>, GraphNodeId> stationsToNodeId;
    private final Map<IdFor<Platform>, GraphNodeId> platforms;
    private final Map<String, GraphNodeId> svcNodes;
    private final Map<String, GraphNodeId> hourNodes;
    private final Map<GraphNodeId, Set<GraphNodeId>> boardings;
    private final Map<GraphNodeId, Set<GraphNodeId>> departs;

    @Inject
    public GraphBuilderCache() {
        cleared = false;
        stationsToNodeId = new ConcurrentHashMap<>();
        routeStations = new ConcurrentHashMap<>();
        platforms = new ConcurrentHashMap<>();
        svcNodes = new ConcurrentHashMap<>();
        hourNodes = new ConcurrentHashMap<>();
        boardings = new ConcurrentHashMap<>();
        departs = new ConcurrentHashMap<>();
    }

    public void fullClear() {
        if (cleared) {
            throw new RuntimeException("Already cleared");
        }
        routeStations.clear();
        stationsToNodeId.clear();
        platforms.clear();
        svcNodes.clear();
        hourNodes.clear();
        cleared = true;
        logger.info("Full cleared");
    }

//    // memory usage management
//    public void routeClear() {
//        svcNodes.clear();
//        hourNodes.clear();
//        logger.debug("Route Clear");
//    }

    @Override
    public void clearHourNodes() {
        hourNodes.clear();
    }

    @Override
    public void clearServiceNodes() {
        svcNodes.clear();
    }

    @Override
    public void putRouteStation(IdFor<RouteStation> id, GraphNode routeStationNode) {
        routeStations.put(id, routeStationNode.getId());
    }

    @Override
    public void putStation(IdFor<Station> station, GraphNode stationNode) {
        stationsToNodeId.put(station, stationNode.getId());
    }

    @Override
    public MutableGraphNode getRouteStation(MutableGraphTransaction txn, Route route, IdFor<Station> stationId) {
        IdFor<RouteStation> routeStationId = RouteStation.createId(stationId, route.getId());
        if (!routeStations.containsKey(routeStationId)) {
            String message = "Cannot find routestation node in cache " + routeStationId; // + " cache " + routeStations;
            logger.error(message);
            throw new RuntimeException(message);
        }
        //return GraphNode.fromTransaction(txn, routeStations.get(routeStationId));
        return txn.getNodeByIdMutable(routeStations.get(routeStationId));
    }

    public MutableGraphNode getRouteStation(MutableGraphTransaction txn, IdFor<RouteStation> id) {
        if (!routeStations.containsKey(id)) {
            String message = "Cannot find routestation node in cache " + id;
            logger.error(message);
            throw new RuntimeException(message);
        }
        return txn.getNodeByIdMutable(routeStations.get(id));
    }

    @Override
    public boolean hasRouteStation(Route route, IdFor<Station> stationId) {
        IdFor<RouteStation> routeStationId = RouteStation.createId(stationId, route.getId());
        return routeStations.containsKey(routeStationId);
    }

    @Override
    public MutableGraphNode getStation(MutableGraphTransaction txn, IdFor<Station> stationId) {
        if (!stationsToNodeId.containsKey(stationId)) {
            String message = "Missing station in cache, station: " + stationId + " Cache: " + stationsToNodeId;
            logger.error(message);
            throw new RuntimeException(message);
        }

        return txn.getNodeByIdMutable(stationsToNodeId.get(stationId));
    }

    @Override
    public MutableGraphNode getPlatform(MutableGraphTransaction txn, IdFor<Platform> platformId) {
        if (!platforms.containsKey(platformId)) {
            throw new RuntimeException("Missing platform id " + platformId);
        }
        return txn.getNodeByIdMutable(platforms.get(platformId));
    }

    @Override
    public void putPlatform(IdFor<Platform> platformId, GraphNode platformNode) {
        platforms.put(platformId, platformNode.getId());
    }

    @Override
    public void putService(IdFor<Route> routeId, Service service, IdFor<Station> begin, IdFor<Station> end, GraphNode svcNode) {
        String key = CreateKeys.getServiceKey(routeId, service.getId(), begin, end);
        svcNodes.put(key, svcNode.getId());
    }

    // TODO This has to be route station to route Station
    @Override
    public MutableGraphNode getServiceNode(MutableGraphTransaction txn, IdFor<Route> routeId, Service service, IdFor<Station> startStation, IdFor<Station> endStation) {
        String id = CreateKeys.getServiceKey(routeId, service.getId(), startStation, endStation);
        return txn.getNodeByIdMutable(svcNodes.get(id));
    }

    @Override
    public void putHour(IdFor<Route> routeId, Service service, IdFor<Station> station, Integer hour, GraphNode node) {
        String hourKey = CreateKeys.getHourKey(routeId, service.getId(), station, hour);
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
    public void putBoarding(GraphNodeId platformOrStation, GraphNodeId routeStationNodeId) {
        putRelationship(boardings, platformOrStation, routeStationNodeId);
    }

    @Override
    public boolean hasBoarding(GraphNodeId platformOrStation, GraphNodeId routeStationNodeId) {
        return hasRelationship(boardings, platformOrStation, routeStationNodeId);
    }

    @Override
    public boolean hasDeparts(GraphNodeId platformOrStation, GraphNodeId routeStationNodeId) {
        return hasRelationship(departs, platformOrStation, routeStationNodeId);
    }

    @Override
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

    @Override
    public boolean hasServiceNode(IdFor<Route> routeId, Service service, IdFor<Station> begin, IdFor<Station> end) {
        return svcNodes.containsKey(CreateKeys.getServiceKey(routeId, service.getId(), begin,end));
    }



    @Override
    public boolean hasHourNode(IdFor<Route> routeId, Service service, IdFor<Station> startId, Integer hour) {
        return hourNodes.containsKey(CreateKeys.getHourKey(routeId, service.getId(), startId, hour));
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
