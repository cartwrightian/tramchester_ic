package com.tramchester.graph.graphbuild.caching;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@LazySingleton
public class GraphBuilderCache implements RouteStationNodeCache, StationAndPlatformNodeCache {
    private static final Logger logger = LoggerFactory.getLogger(GraphBuilderCache.class);

    private boolean cleared;
    private final ConcurrentMap<IdFor<RouteStation>, GraphNodeId> routeStations;
    private final ConcurrentMap<IdFor<Station>, GraphNodeId> stationsToNodeId;
    private final ConcurrentMap<IdFor<Platform>, GraphNodeId> platforms;

    @Inject
    public GraphBuilderCache() {
        cleared = false;
        stationsToNodeId = new ConcurrentHashMap<>();
        routeStations = new ConcurrentHashMap<>();
        platforms = new ConcurrentHashMap<>();
    }

    public void fullClear() {
        if (cleared) {
            throw new RuntimeException("Already cleared");
        }
        routeStations.clear();
        stationsToNodeId.clear();
        platforms.clear();
        cleared = true;
        logger.info("Full cleared");
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


}
