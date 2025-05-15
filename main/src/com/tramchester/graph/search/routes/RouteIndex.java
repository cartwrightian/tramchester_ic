package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.ComponentThatCaches;
import com.tramchester.caching.DataCache;
import com.tramchester.caching.FileDataCache;
import com.tramchester.dataexport.HasDataSaver;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.RouteIndexPairFactory;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.RouteRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.String.format;

/***
 * Provides a map to/from an integer index for each route, which facilitates the computation of route interchanges
 * via bitmaps. Expensive to create for buses and trains, so cacheable.
 */
@LazySingleton
public class RouteIndex extends ComponentThatCaches<RouteIndexData, RouteIndex.RouteIndexes> {
    private static final Logger logger = LoggerFactory.getLogger(RouteIndex.class);

    private final GraphFilterActive graphFilter;
    private final RouteIndexPairFactory pairFactory;
    private final RouteIndexes routeIndexes;

    @Inject
    public RouteIndex(RouteRepository routeRepository, GraphFilterActive graphFilter, DataCache dataCache, RouteIndexPairFactory pairFactory) {
        super(dataCache, RouteIndexData.class);
        this.graphFilter = graphFilter;
        this.pairFactory = pairFactory;

        routeIndexes = new RouteIndexes(routeRepository);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        if (graphFilter.isActive()) {
            logger.warn("Filtering is enabled, skipping all caching");
            createIndex();
        } else {
            if (super.loadFromCache(routeIndexes)) {
                logger.info("Loaded from cache");
            } else {
                logger.info("Not in cache, creating");
                createIndex();
            }
        }

        routeIndexes.validate();
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        if (!graphFilter.isActive()) {
            super.saveCacheIfNeeded(routeIndexes);
        }
        routeIndexes.clear();
        logger.info("Stopped");
    }

    private void createIndex() {
        logger.info("Creating index");
        routeIndexes.populate();
        logger.info("Added " + routeIndexes.size() + " index entries");
    }

    public short indexFor(final IdFor<Route> routeId) {
        return routeIndexes.getIndexFor(routeId);
    }

    public Route getRouteFor(final short index) {
        return routeIndexes.getRouteFor(index);
    }

    public RoutePair getPairFor(final RouteIndexPair indexPair) {
        return routeIndexes.getPairFor(indexPair);
    }

    public RouteIndexPair getPairFor(final RoutePair routePair) {
        final short a = indexFor(routePair.first().getId());
        final short b = indexFor(routePair.second().getId());
        return pairFactory.get(a, b);
    }

    public boolean hasIndexFor(final IdFor<Route> routeId) {
        return routeIndexes.hasIndexFor(routeId);
    }

    public long sizeFor(final TransportMode mode) {
        return routeIndexes.sizeFor(mode);
    }

    static public class RouteIndexes implements FileDataCache.CachesData<RouteIndexData> {

        private final RouteRepository routeRepository;
        private final int numberOfRoutes;
        private final Map<Route, Short> mapRouteIdToIndex;
        private final Map<Short, Route> mapIndexToRouteId;

        public RouteIndexes(RouteRepository routeRepository) {
            this.routeRepository = routeRepository;
            numberOfRoutes = routeRepository.numberOfRoutes();
            mapRouteIdToIndex = new HashMap<>();
            mapIndexToRouteId = new HashMap<>();
        }

        public void populate() {
            final List<Route> routeList = new ArrayList<>(routeRepository.getRoutes());

            routeList.sort(Comparator.comparing(Route::getId));
            for (short i = 0; i < routeList.size(); i++) {
                mapRouteIdToIndex.put(routeList.get(i), i);
                mapIndexToRouteId.put(i, routeList.get(i));
            }
        }

        @Override
        public void cacheTo(HasDataSaver<RouteIndexData> hasDataSaver) {
            Stream<RouteIndexData> toCache = mapRouteIdToIndex.entrySet().stream().
                    map(entry -> new RouteIndexData(entry.getValue(), entry.getKey().getId()));
            hasDataSaver.cacheStream(toCache);
        }

        @Override
        public String getFilename() {
            return RouteToRouteCosts.INDEX_FILE;
        }

        @Override
        public void loadFrom(Stream<RouteIndexData> stream) throws FileDataCache.CacheLoadException {
            logger.info("Loading from cache");
            IdSet<Route> missingRouteIds = new IdSet<>();
            stream.forEach(item -> {
                final IdFor<Route> routeId = item.getRouteId();
                if (!routeRepository.hasRouteId(routeId)) {
                    String message = "RouteId not found in repository: " + routeId;
                    logger.error(message);
                    missingRouteIds.add(routeId);
                    //throw new RuntimeException(message);
                }
                Route route = routeRepository.getRouteById(routeId);
                mapRouteIdToIndex.put(route, item.getIndex());
                mapIndexToRouteId.put(item.getIndex(), route);
            });
            if (!missingRouteIds.isEmpty()) {
                String msg = format("The following routeIds present in index file but not the route repository (size %s) %s",
                        routeRepository.numberOfRoutes(), missingRouteIds);
                // TODO debug?
                logger.warn("Routes in repo: " + HasId.asIds(routeRepository.getRoutes()));
                throw new FileDataCache.CacheLoadException(msg);
            }
            if (mapRouteIdToIndex.size() != numberOfRoutes) {
                String msg = "Mismatch on number of routes, from index got: " + mapRouteIdToIndex.size() +
                        " but repository has: " + numberOfRoutes;
                logger.error(msg);
                throw new FileDataCache.CacheLoadException(msg);
            }
        }

        public boolean hasIndexFor(final IdFor<Route> routeId) {
            final Route route = routeRepository.getRouteById(routeId);
            return mapRouteIdToIndex.containsKey(route);
        }

        public void validate() {
            // here for past serialisation issues
            if (mapRouteIdToIndex.size()!=mapIndexToRouteId.size()) {
                String msg = format("Constraints on mapping violated mapRouteIdToIndex %s != mapIndexToRouteId %s"
                        , mapRouteIdToIndex.size(), mapIndexToRouteId.size());
                logger.error(msg);
                throw new RuntimeException(msg);
            }
        }

        public void clear() {
            mapRouteIdToIndex.clear();
            mapIndexToRouteId.clear();
        }

        public long size() {
            return mapRouteIdToIndex.size();
        }

        public short getIndexFor(final IdFor<Route> routeId) {
            final Route route = routeRepository.getRouteById(routeId);
            return getIndexFor(route);
        }

        public short getIndexFor(final Route route) {
            if (!(mapRouteIdToIndex.containsKey(route))) {
                String message = format("No index for route %s, is cache file %s outdated? ",
                        route.getId(), getFilename());
                logger.error(message);
                throw new RuntimeException(message);
            }
            return mapRouteIdToIndex.get(route);
        }

        public Route getRouteFor(final short index) {
            return mapIndexToRouteId.get(index);
        }

        public RoutePair getPairFor(final RouteIndexPair indexPair) {
            final Route first = mapIndexToRouteId.get(indexPair.first());
            if (first==null) {
                throw new RuntimeException("Could not find first Route for index " + indexPair);
            }
            final Route second = mapIndexToRouteId.get(indexPair.second());
            if (second==null) {
                throw new RuntimeException("Could not find second Route for index " + indexPair);
            }

            return new RoutePair(first, second);
        }

        public long sizeFor(final TransportMode mode) {
            return mapRouteIdToIndex.keySet().stream().filter(route -> route.getTransportMode().equals(mode)).count();
        }

    }
}
