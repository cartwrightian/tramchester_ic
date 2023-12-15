package com.tramchester.repository;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.CachableData;
import com.tramchester.caching.ComponentThatCaches;
import com.tramchester.caching.FileDataCache;
import com.tramchester.dataexport.HasDataSaver;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.routes.FindRouteStationsWithPathToInterchange;
import com.tramchester.metrics.Timing;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@LazySingleton
public class RouteInterchangeRepository extends ComponentThatCaches<RouteInterchangeRepository.RouteInterchangeWithPath,
        RouteInterchangeRepository.RouteStationWithPathToInterchange> {
    private static final Logger logger = LoggerFactory.getLogger(RouteInterchangeRepository.class);

    private final RouteRepository routeRepository;
    private final InterchangeRepository interchangeRepository;
    private final FindRouteStationsWithPathToInterchange findRouteStationsWithPathToInterchange;

    private final Map<Route, Set<InterchangeStation>> interchangesForRoute;
    private RouteStationWithPathToInterchange withPathToInterchange;

    @Inject
    public RouteInterchangeRepository(RouteRepository routeRepository, InterchangeRepository interchangeRepository,
                                      @SuppressWarnings("unused") StagedTransportGraphBuilder.Ready ready,
                                      FileDataCache fileDataCache, FindRouteStationsWithPathToInterchange findRouteStationsWithPathToInterchange) {
        super(fileDataCache, RouteInterchangeWithPath.class);
        this.routeRepository = routeRepository;
        this.interchangeRepository = interchangeRepository;

        this.findRouteStationsWithPathToInterchange = findRouteStationsWithPathToInterchange;
        interchangesForRoute = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        populateRouteToInterchangeMap();

        withPathToInterchange = new RouteStationWithPathToInterchange();
        if (super.loadFromCache(withPathToInterchange)) {
            logger.info("Loaded from cache");
        }
        else {
            populateRouteStationToFirstInterchangeByRouteStation();
        }
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        interchangesForRoute.clear();
        super.saveCacheIfNeeded(withPathToInterchange);
        withPathToInterchange.clear();
        logger.info("Stopped");
    }

    private void populateRouteStationToFirstInterchangeByRouteStation() {
        logger.info("Populate cost to first interchange");
        Set<RouteStationId> havePaths = findRouteStationsWithPathToInterchange.havePathToInterchange();
        havePaths.forEach((routeStation) -> withPathToInterchange.put(routeStation));
    }

    private void populateRouteToInterchangeMap() {
        try (Timing ignored = new Timing(logger,"Populate interchanges for routes")) {
            routeRepository.getRoutes().forEach(route -> interchangesForRoute.put(route, new HashSet<>()));
            Set<InterchangeStation> allInterchanges = interchangeRepository.getAllInterchanges();
            allInterchanges.stream().
                    flatMap(inter -> inter.getDropoffRoutes().stream().map(route -> Pair.of(route, inter))).
                    forEach(pair -> interchangesForRoute.get(pair.getLeft()).add(pair.getRight()));
        }
    }

    public Set<InterchangeStation> getFor(Route route) {
        return interchangesForRoute.get(route);
    }

    public boolean hasPathToInterchange(RouteStation routeStation) {
        if (interchangeRepository.isInterchange(routeStation.getStation())) {
            return true;
        }
        return withPathToInterchange.hasRouteStation(routeStation);
    }

    protected static class RouteStationWithPathToInterchange implements FileDataCache.CachesData<RouteInterchangeWithPath> {
        private final Set<RouteStationId> hasPath;

        private RouteStationWithPathToInterchange() {
            hasPath = new HashSet<>();
        }

        @Override
        public void cacheTo(HasDataSaver<RouteInterchangeWithPath> hasDataSaver) {
            hasDataSaver.cacheStream(hasPath.stream().map(RouteInterchangeWithPath::from));
        }

        @Override
        public String getFilename() {
            return "RouteStationWithPathToInterchange.json";
        }

        @Override
        public void loadFrom(Stream<RouteInterchangeWithPath> stream) {
            stream.forEach(item ->
                    hasPath.add(item.getRouteStationId()));
        }

        public void clear() {
            hasPath.clear();
        }

        public boolean hasRouteStation(RouteStation routeStation) {
            return hasPath.contains(routeStation.getId());
        }

        public void put(RouteStationId routeStation) {
            hasPath.add(routeStation);
        }
    }

    protected static class RouteInterchangeWithPath implements CachableData {
        private final RouteStationId routeStationId;

        @JsonCreator
        public RouteInterchangeWithPath(@JsonProperty("routeStationId") RouteStationId routeStationId) {
            this.routeStationId = routeStationId;
        }

        public static RouteInterchangeWithPath from(RouteStationId routeStationId) {
            return new RouteInterchangeWithPath(routeStationId);
        }

        public RouteStationId getRouteStationId() {
            return routeStationId;
        }

    }
}
