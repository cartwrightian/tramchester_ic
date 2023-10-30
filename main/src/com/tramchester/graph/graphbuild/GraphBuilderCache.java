package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphNode;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

@LazySingleton
public class GraphBuilderCache {
    private static final Logger logger = LoggerFactory.getLogger(GraphBuilderCache.class);

    private boolean cleared;
    private final Map<IdFor<RouteStation>, Long> routeStations;
    private final Map<IdFor<Station>, Long> stationsToNodeId;
    private final Map<IdFor<Platform>, Long> platforms;
    private final Map<String, Long> svcNodes;
    private final Map<String, Long> hourNodes;
    private final Map<Long, Set<Long>> boardings;
    private final Map<Long, Set<Long>> departs;

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

    protected void fullClear() {
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

    // memory usage management
    protected void routeClear() {
        svcNodes.clear();
        hourNodes.clear();
        logger.debug("Route Clear");
    }

    public void putRouteStation(IdFor<RouteStation> id, GraphNode routeStationNode) {
        routeStations.put(id, routeStationNode.getId());
    }

    protected void putStation(IdFor<Station> station, GraphNode stationNode) {
        stationsToNodeId.put(station, stationNode.getId());
    }

    protected GraphNode getRouteStation(Transaction txn, Route route, IdFor<Station> stationId) {
        IdFor<RouteStation> routeStationId = RouteStation.createId(stationId, route.getId());
        if (!routeStations.containsKey(routeStationId)) {
            String message = "Cannot find routestation node in cache " + routeStationId; // + " cache " + routeStations;
            logger.error(message);
            throw new RuntimeException(message);
        }
        return GraphNode.fromTransaction(txn, routeStations.get(routeStationId));
        //return txn.getNodeById(routeStations.get(routeStationId));
    }

    protected GraphNode getRouteStation(Transaction txn, IdFor<RouteStation> id) {
        if (!routeStations.containsKey(id)) {
            String message = "Cannot find routestation node in cache " + id;
            logger.error(message);
            throw new RuntimeException(message);
        }
        return GraphNode.fromTransaction(txn, routeStations.get(id));
        //return txn.getNodeById(routeStations.get(id));
    }

    public boolean hasRouteStation(Route route, IdFor<Station> stationId) {
        IdFor<RouteStation> routeStationId = RouteStation.createId(stationId, route.getId());
        return routeStations.containsKey(routeStationId);
    }

    protected GraphNode getStation(Transaction txn, IdFor<Station> stationId) {
        if (!stationsToNodeId.containsKey(stationId)) {
            String message = "Missing station in cache, station: " + stationId + " Cache: " + stationsToNodeId;
            logger.error(message);
            throw new RuntimeException(message);
        }
        Long id = stationsToNodeId.get(stationId);
        return GraphNode.fromTransaction(txn, id);
        //return txn.getNodeById(id);
    }

    protected GraphNode getPlatform(Transaction txn, IdFor<Platform> platformId) {
        if (!platforms.containsKey(platformId)) {
            throw new RuntimeException("Missing platform id " + platformId);
        }
        return GraphNode.fromTransaction(txn, platforms.get(platformId));
        //return txn.getNodeById(platforms.get(platformId));
    }

    protected void putPlatform(IdFor<Platform> platformId, GraphNode platformNode) {
        platforms.put(platformId, platformNode.getId());
    }

    protected void putService(IdFor<Route> routeId, Service service, IdFor<Station> begin, IdFor<Station> end, GraphNode svcNode) {
        String key = CreateKeys.getServiceKey(routeId, service.getId(), begin, end);
        svcNodes.put(key, svcNode.getId());
    }

    // TODO This has to be route station to route Station
    protected GraphNode getServiceNode(Transaction txn, IdFor<Route> routeId, Service service, IdFor<Station> startStation, IdFor<Station> endStation) {
        String id = CreateKeys.getServiceKey(routeId, service.getId(), startStation, endStation);
        return GraphNode.fromTransaction(txn, svcNodes.get(id));
        //return txn.getNodeById(svcNodes.get(id));
    }

    protected void putHour(IdFor<Route> routeId, Service service, IdFor<Station> station, Integer hour, GraphNode node) {
        String hourKey = CreateKeys.getHourKey(routeId, service.getId(), station, hour);
        hourNodes.put(hourKey, node.getId());
    }

    protected GraphNode getHourNode(Transaction txn, IdFor<Route> routeId, Service service, IdFor<Station> station, Integer hour) {
        String key = CreateKeys.getHourKey(routeId, service.getId(), station, hour);
        if (!hourNodes.containsKey(key)) {
            throw new RuntimeException(format("Missing hour node for key %s service %s station %s hour %s",
                    key, service.getId(), station, hour));
        }
        return GraphNode.fromTransaction(txn, hourNodes.get(key));
        //return txn.getNodeById(hourNodes.get(key));
    }

    protected void putBoarding(long platformOrStation, long routeStationNodeId) {
        putRelationship(boardings, platformOrStation, routeStationNodeId);
    }

    protected boolean hasBoarding(long platformOrStation, long routeStationNodeId) {
        return hasRelationship(boardings, platformOrStation, routeStationNodeId);
    }

    protected boolean hasDeparts(long platformOrStation, long routeStationNodeId) {
        return hasRelationship(departs, platformOrStation, routeStationNodeId);
    }

    protected void putDepart(long boardingNodeId, long routeStationNodeId) {
        putRelationship(departs, boardingNodeId, routeStationNodeId);
    }

    private void putRelationship(Map<Long, Set<Long>> relationshipCache, long boardingNodeId, long routeStationNodeId) {
        if (relationshipCache.containsKey(boardingNodeId)) {
            relationshipCache.get(boardingNodeId).add(routeStationNodeId);
        } else {
            HashSet<Long> set = new HashSet<>();
            set.add(routeStationNodeId);
            relationshipCache.put(boardingNodeId, set);
        }
    }

    private boolean hasRelationship(Map<Long, Set<Long>> relationshipCache,  long boardingNodeId, long routeStationNodeId) {
        if (relationshipCache.containsKey(boardingNodeId)) {
            return relationshipCache.get(boardingNodeId).contains(routeStationNodeId);
        }
        return false;
    }

    public boolean hasServiceNode(IdFor<Route> routeId, Service service, IdFor<Station> begin, IdFor<Station> end) {
        return svcNodes.containsKey(CreateKeys.getServiceKey(routeId, service.getId(), begin,end));
    }

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
