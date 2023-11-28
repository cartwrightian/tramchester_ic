package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RouteStationId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TimedTransaction;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class FindRouteStationToInterchangeCosts {
    private static final Logger logger = LoggerFactory.getLogger(FindRouteStationToInterchangeCosts.class);

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final GraphDatabase graphDatabase;

    @Inject
    public FindRouteStationToInterchangeCosts(StationRepository stationRepository, RouteRepository routeRepository,
                                              @SuppressWarnings("unused") StagedTransportGraphBuilder.Ready ready,
                                              GraphDatabase graphDatabase) {
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
        this.graphDatabase = graphDatabase;
    }

    /**
     * @return map from routeStationId to the least cost to an interchange
     */
    public Map<RouteStationId, Duration> getDurations() {
        final Set<RouteStation> routeStations = stationRepository.getRouteStations();
        logger.info("Populate cost for first interchange " + routeStations.size() + " route stations");

        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "populateForRoutes")) {
            return routeRepository.getRoutes().stream().
                    flatMap(route -> getCostsForRoute(timedTransaction.transaction(), route).entrySet().stream()).
                    collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    public Map<RouteStationId, Duration> getCostsForRoute(final MutableGraphTransaction txn, final Route route) {

        final Instant startTime = Instant.now();

        final IdFor<Route> routeId = route.getId();

        final long maxNodes = route.getTrips().stream().
                flatMap(trip -> trip.getStopCalls().getStationSequence(false).stream()).distinct().count();

        logger.debug("Find stations to interchange least costs for " + routeId + " max nodes " + maxNodes);

        final String template = "MATCH (rs:ROUTE_STATION {route_id:$id})-[r:ON_ROUTE*1..%s {route_id:$id}]->(:INTERCHANGE {route_id:$id})" +
                " WHERE NOT rs:INTERCHANGE " +
                " WITH reduce(s = 0, x IN r | s + x.cost) as cost, rs.station_id as start " +
                " RETURN start, cost";

        final String query = format(template, maxNodes);

        final Map<String, Object> params = new HashMap<>();
        params.put("id", routeId.getGraphId());

        final Result results = txn.execute(query, params);

        Stream<Pair<String, Long>> stationToInterList = results.stream().
                map(row -> Pair.of(row.get("start").toString(), (Long) row.get("cost"))).
                map(pair -> Pair.of(pair.getLeft(), pair.getRight()));

        BinaryOperator<Long> merge = (costA, costB) -> (costA<costB) ? costA : costB;
        Map<String, Long> stationToInterPair = stationToInterList.collect(Collectors.toMap(Pair::getLeft, Pair::getRight, merge));

        logger.debug("Found " + stationToInterPair.size() + " results for " + routeId);

        Map<RouteStationId, Duration> routeStationToInterchangeCost = new HashMap<>();

        stationToInterPair.forEach((text, cost) -> {
            RouteStationId routeStation = RouteStation.createId(Station.createId(text), routeId);
            routeStationToInterchangeCost.put(routeStation, Duration.ofMinutes(cost));
        });

        stationToInterPair.clear();

        Instant finish = Instant.now();
        long durationMs = Duration.between(startTime, finish).toMillis();
        if (durationMs>1000) {
            logger.warn(format("Route %s (%s) max nodes %s took %s milli", route.getId(), route.getName(), maxNodes, durationMs));
        }

        return routeStationToInterchangeCost;
    }
}
