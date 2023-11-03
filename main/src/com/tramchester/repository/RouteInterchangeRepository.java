package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TimedTransaction;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.metrics.Timing;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteInterchangeRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteInterchangeRepository.class);

    private final RouteRepository routeRepository;
    private final StationRepository stationRepository;
    private final InterchangeRepository interchangeRepository;
    private final GraphDatabase graphDatabase;

    private final Map<Route, Set<InterchangeStation>> interchangesForRoute;
    private Map<RouteStation, Duration> routeStationToInterchangeCost;

    @Inject
    public RouteInterchangeRepository(RouteRepository routeRepository, StationRepository stationRepository, InterchangeRepository interchangeRepository,
                                      GraphDatabase graphDatabase,
                                      @SuppressWarnings("unused") StagedTransportGraphBuilder.Ready ready) {
        this.routeRepository = routeRepository;
        this.stationRepository = stationRepository;
        this.interchangeRepository = interchangeRepository;

        this.graphDatabase = graphDatabase;
        interchangesForRoute = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        populateRouteToInterchangeMap();
        populateRouteStationToFirstInterchangeByRouteStation();
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        interchangesForRoute.clear();
        routeStationToInterchangeCost.clear();
        logger.info("Stopped");
    }

    private void populateRouteStationToFirstInterchangeByRouteStation() {
        final Set<RouteStation> routeStations = stationRepository.getRouteStations();
        logger.info("Populate for first interchange " + routeStations.size() + " route stations");

        routeStationToInterchangeCost = new HashMap<>();
        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "populateForRoutes")) {
            routeRepository.getRoutes().forEach(route -> populateForRoute(timedTransaction.transaction(), route));
        }
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
        if (routeStationToInterchangeCost.containsKey(routeStation)) {
            return routeStationToInterchangeCost.get(routeStation);
        }
        return Duration.ofSeconds(-999);
    }

    private void populateForRoute(final GraphTransaction txn, final Route route) {

        Instant startTime = Instant.now();

        final IdFor<Route> routeId = route.getId();

        final long maxNodes = route.getTrips().stream().
                flatMap(trip -> trip.getStopCalls().getStationSequence(false).stream()).distinct().count();

        logger.debug("Find stations to interchange least costs for " + routeId + " max nodes " + maxNodes);

        String template = "MATCH (rs:ROUTE_STATION {route_id:$id})-[r:ON_ROUTE*1..%s {route_id:$id}]->(:INTERCHANGE {route_id:$id})" +
                " WHERE NOT rs:INTERCHANGE " +
                " WITH reduce(s = 0, x IN r | s + x.cost) as cost, rs.station_id as start " +
                " RETURN start, cost";

        final String query = format(template, maxNodes);

        Map<String, Object> params = new HashMap<>();
        params.put("id", routeId.getGraphId());

        final Result results = txn.execute(query, params);

        Stream<Pair<IdFor<Station>, Long>> stationToInterList = results.stream().
                map(row -> Pair.of(row.get("start").toString(), (Long) row.get("cost"))).
                map(pair -> Pair.of(Station.createId(pair.getLeft()), pair.getRight()));

        Map<IdFor<Station>, Long> stationToInterPair = new HashMap<>();

        stationToInterList.forEach(pair -> {
            final IdFor<Station> key = pair.getKey();
            final long cost = pair.getValue();
            if (stationToInterPair.containsKey(key)) {
                if (cost < stationToInterPair.get(key)) {
                    stationToInterPair.put(key, cost);
                }
            } else {
                stationToInterPair.put(key, cost);
            }
        });

        logger.debug("Found " + stationToInterPair.size() + " results for " + routeId);

        stationToInterPair.forEach((stationIdPair, cost) -> {
            RouteStation routeStation = stationRepository.getRouteStationById(RouteStation.createId(stationIdPair, routeId));
            routeStationToInterchangeCost.put(routeStation, Duration.ofMinutes(cost));
        });

        stationToInterPair.clear();

        Instant finish = Instant.now();
        long durationMs = Duration.between(startTime, finish).toMillis();
        if (durationMs>1000) {
            logger.warn(format("Route %s max nodes %s took %s milli", route.getId(), maxNodes, durationMs));
        }
    }

    private Duration sum(ArrayList<Integer> nums) {
        return Duration.ofMinutes(nums.stream().mapToInt(num -> num).sum());
    }

}
