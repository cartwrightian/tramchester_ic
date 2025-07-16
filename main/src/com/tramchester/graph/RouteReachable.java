package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.facade.neo4j.ImmutableGraphRelationship;
import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.ON_ROUTE;

@LazySingleton
public class RouteReachable {
    private static final Logger logger = LoggerFactory.getLogger(RouteReachable.class);

    private final GraphDatabase graphDatabaseService;
    private final StationRepository stationRepository;
    private final StationAvailabilityRepository availabilityRepository;

    @Inject
    public RouteReachable(GraphDatabase graphDatabaseService, StationRepository stationRepository,
                          @SuppressWarnings("unused") StagedTransportGraphBuilder.Ready ready,
                          StationAvailabilityRepository availabilityRepository) {
        this.graphDatabaseService = graphDatabaseService;
        this.stationRepository = stationRepository;
        this.availabilityRepository = availabilityRepository;
    }

    // supports position inference on live data
    public List<Route> getRoutesFromStartToNeighbour(StationPair pair, TramDate date, TimeRange timeRange, EnumSet<TransportMode> modes) {
        List<Route> results = new ArrayList<>();
        final Station startStation = pair.getBegin();
        final Set<Route> firstRoutes = availabilityRepository.getPickupRoutesFor(startStation, date, timeRange, modes);
        final IdFor<Station> endStationId = pair.getEnd().getId();

        try (ImmutableGraphTransactionNeo4J txn = graphDatabaseService.beginTx()) {
            firstRoutes.forEach(route -> {
                final RouteStation routeStation = stationRepository.getRouteStation(startStation, route);
                final GraphNode routeStationNode = txn.findNode(routeStation);
                if (routeStationNode==null) {
                    logger.warn("Missing route station, graph DB rebuild needed?");
                } else {
                    Stream<ImmutableGraphRelationship> edges = routeStationNode.getRelationships(txn, GraphDirection.Outgoing, ON_ROUTE);

                    edges.forEach(edge -> {
                        final IdFor<Station> endNodeStationId = edge.getEndStationId();

                        if (endStationId.equals(endNodeStationId)) {
                            results.add(route);
                        }
                    });

                }
            });
        }
        return results;
    }

}
