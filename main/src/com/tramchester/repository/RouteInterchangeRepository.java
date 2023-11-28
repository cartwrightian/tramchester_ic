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
import com.tramchester.graph.search.routes.FindRouteStationToInterchangeCosts;
import com.tramchester.metrics.Timing;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@LazySingleton
public class RouteInterchangeRepository extends ComponentThatCaches<RouteInterchangeRepository.RouteToInterchangeCost,
        RouteInterchangeRepository.RouteStationToInterchangeCosts> {
    private static final Logger logger = LoggerFactory.getLogger(RouteInterchangeRepository.class);

    private final RouteRepository routeRepository;
    private final InterchangeRepository interchangeRepository;
    private final FindRouteStationToInterchangeCosts findRouteStationToInterchangeCosts;

    private final Map<Route, Set<InterchangeStation>> interchangesForRoute;
    private RouteStationToInterchangeCosts routeStationToInterchangeCost;

    @Inject
    public RouteInterchangeRepository(RouteRepository routeRepository, InterchangeRepository interchangeRepository,
                                      @SuppressWarnings("unused") StagedTransportGraphBuilder.Ready ready,
                                      FileDataCache fileDataCache, FindRouteStationToInterchangeCosts findRouteStationToInterchangeCosts) {
        super(fileDataCache, RouteToInterchangeCost.class);
        this.routeRepository = routeRepository;
        this.interchangeRepository = interchangeRepository;

        this.findRouteStationToInterchangeCosts = findRouteStationToInterchangeCosts;
        interchangesForRoute = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        populateRouteToInterchangeMap();

        routeStationToInterchangeCost = new RouteStationToInterchangeCosts();
        if (super.loadFromCache(routeStationToInterchangeCost)) {
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
        super.saveCacheIfNeeded(routeStationToInterchangeCost);
        routeStationToInterchangeCost.clear();
        logger.info("Stopped");
    }

    private void populateRouteStationToFirstInterchangeByRouteStation() {
        logger.info("Populate cost to first interchange");
        Map<RouteStationId, Duration> durations = findRouteStationToInterchangeCosts.getDurations();
        durations.forEach((routeStation, duration) -> routeStationToInterchangeCost.putCost(routeStation,duration));
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

    public Duration costToInterchange(RouteStation routeStation) {
        if (interchangeRepository.isInterchange(routeStation.getStation())) {
            return Duration.ZERO;
        }
        if (routeStationToInterchangeCost.hasRouteStation(routeStation)) {
            return routeStationToInterchangeCost.costFrom(routeStation);
        }
        return Duration.ofSeconds(-999);
    }

    protected static class RouteStationToInterchangeCosts implements FileDataCache.CachesData<RouteToInterchangeCost> {
        private final Map<RouteStationId, Duration> costs;

        private RouteStationToInterchangeCosts() {
            costs = new HashMap<>();
        }

        @Override
        public void cacheTo(HasDataSaver<RouteToInterchangeCost> hasDataSaver) {
            hasDataSaver.cacheStream(costs.entrySet().stream().map(RouteToInterchangeCost::from));
        }

        @Override
        public String getFilename() {
            return "RouteStationToInterchangeCosts.json";
        }

        @Override
        public void loadFrom(Stream<RouteToInterchangeCost> stream) {
            stream.forEach(item ->
                    costs.put(item.getRouteStationId(), Duration.ofSeconds(item.getSeconds())));
        }

        public void clear() {
            costs.clear();
        }

        public boolean hasRouteStation(RouteStation routeStation) {
            return costs.containsKey(routeStation.getId());
        }

        public Duration costFrom(RouteStation routeStation) {
            return costs.get(routeStation.getId());
        }

        public void putCost(RouteStationId routeStation, Duration duration) {
            costs.put(routeStation, duration);
        }
    }

    protected static class RouteToInterchangeCost implements CachableData {
        private final RouteStationId routeStationId;
        private final long seconds;

        @JsonCreator
        public RouteToInterchangeCost(@JsonProperty("routeStationId") RouteStationId routeStationId,
                                      @JsonProperty("seconds") long seconds) {
            this.routeStationId = routeStationId;
            this.seconds = seconds;
        }

        public static RouteToInterchangeCost from(Map.Entry<RouteStationId, Duration> enrty) {
            return new RouteToInterchangeCost(enrty.getKey(), enrty.getValue().getSeconds());
        }

        public RouteStationId getRouteStationId() {
            return routeStationId;
        }

        public long getSeconds() {
            return seconds;
        }
    }
}
