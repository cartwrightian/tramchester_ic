package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.RequestStopStream;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.search.diagnostics.ReasonsToGraphViz;
import com.tramchester.graph.search.selectors.BranchSelectorFactory;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.TransportData;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCalculatorForBoxes extends RouteCalculatorSupport {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculatorForBoxes.class);

    private final TramchesterConfig config;
    private final GraphDatabase graphDatabaseService;
    private final ClosedStationsRepository closedStationsRepository;
    private final RunningRoutesAndServices runningRoutesAndService;
    private final BranchSelectorFactory branchSelectorFactory;

    @Inject
    public RouteCalculatorForBoxes(TramchesterConfig config,
                                   TransportData transportData,
                                   GraphDatabase graphDatabaseService, TraversalStateFactory traversalStateFactory,
                                   PathToStages pathToStages,
                                   NodeContentsRepository nodeContentsRepository,
                                   ProvidesNow providesNow,
                                   MapPathToLocations mapPathToLocations,
                                   BetweenRoutesCostRepository routeToRouteCosts, ReasonsToGraphViz reasonToGraphViz,
                                   ClosedStationsRepository closedStationsRepository, RunningRoutesAndServices runningRoutesAndService,
                                   @SuppressWarnings("unused") RouteCostCalculator routeCostCalculator,
                                   BranchSelectorFactory branchSelectorFactory, StationAvailabilityRepository stationAvailabilityRepository) {
        super(pathToStages, nodeContentsRepository, graphDatabaseService,
                traversalStateFactory, providesNow, mapPathToLocations,
                transportData, config, transportData, routeToRouteCosts, reasonToGraphViz, stationAvailabilityRepository, false);
        this.config = config;
        this.graphDatabaseService = graphDatabaseService;
        this.closedStationsRepository = closedStationsRepository;
        this.runningRoutesAndService = runningRoutesAndService;
        this.branchSelectorFactory = branchSelectorFactory;
    }

    public RequestStopStream<JourneysForBox> calculateRoutes(final LocationSet destinations, final JourneyRequest journeyRequest,
                                                             final List<BoundingBoxWithStations> boxes) {
        logger.info("Finding routes for " + boxes.size() + " bounding boxes");

        // TODO Compute over a range of times??

        final long maxNumberOfJourneys = journeyRequest.getMaxNumberOfJourneys();

        final JourneyConstraints journeyConstraints = createJourneyConstraints(destinations, journeyRequest);

        final Set<GraphNodeId> destinationNodeIds = getDestinationNodeIds(destinations);

        // share selector across queries, to allow caching of station to station distances
        final BranchOrderingPolicy selector = branchSelectorFactory.getFor(destinations);

        final RequestStopStream<JourneysForBox> result = new RequestStopStream<>();

        final Stream<JourneysForBox> stream = boxes.parallelStream().
                filter(item -> result.isRunning()).
                map(box -> {

            if (logger.isDebugEnabled()) {
                logger.debug(format("Finding shortest path for %s --> %s for %s", box, destinations, journeyRequest));
            }
            final LocationSet startingStations = box.getStations();
            final LowestCostSeen lowestCostSeenForBox = new LowestCostSeen();

            final AtomicInteger journeyIndex = new AtomicInteger(0);

            try (final GraphTransaction txn = graphDatabaseService.beginTx()) {

                final Stream<Journey> journeys = startingStations.stream().
                        filter(start -> !destinations.contains(start)).
                        map(start -> createNodeAndStation(txn, start)).
                        flatMap(nodeAndStation -> numChangesRange(journeyRequest, startingStations, destinations).
                                map(numChanges -> createPathRequest(journeyRequest, nodeAndStation,  numChanges, journeyConstraints, selector))).
                        flatMap(pathRequest -> findShortestPath(txn, destinationNodeIds, destinations,
                                createServiceReasons(journeyRequest), pathRequest, createPreviousVisits(), lowestCostSeenForBox)).
                        map(timedPath -> createJourney(journeyRequest, timedPath, destinations, journeyIndex, txn));

                final Set<Journey> collect = journeys.
                        filter(journey -> !journey.getStages().isEmpty()).
                        limit(maxNumberOfJourneys).
                        collect(Collectors.toSet());

                // yielding
                return new JourneysForBox(box, collect);
            }
        });

        return result.setStream(stream);
    }

    @NotNull
    private JourneyConstraints createJourneyConstraints(final LocationSet destinations, final JourneyRequest journeyRequest) {
        final TramDate date = journeyRequest.getDate();

        final TimeRange destinationsAvailable = getDestinationsAvailable(destinations, date);
        final LowestCostsForDestRoutes lowestCostForDestinations = routeToRouteCosts.getLowestCostCalcutatorFor(destinations, journeyRequest);
        final RunningRoutesAndServices.FilterForDate routeAndServicesFilter = runningRoutesAndService.getFor(date);
        final IdSet<Station> closedStations = closedStationsRepository.getFullyClosedStationsFor(date).stream().
                map(ClosedStation::getStationId).collect(IdSet.idCollector());

        return new JourneyConstraints(config, routeAndServicesFilter, closedStations,
                destinations, lowestCostForDestinations, journeyRequest.getMaxJourneyDuration(), destinationsAvailable);
    }

    private NumberOfChanges computeNumberOfChanges(LocationSet starts, LocationSet destinations, TramDate date, TimeRange timeRange, EnumSet<TransportMode> modes) {
        return routeToRouteCosts.getNumberOfChanges(starts, destinations, date, timeRange, modes);
    }

    protected Stream<Integer> numChangesRange(final JourneyRequest journeyRequest, final LocationSet startingStations, final LocationSet destinations) {
        NumberOfChanges numberChanges = computeNumberOfChanges(startingStations, destinations, journeyRequest.getDate(), journeyRequest.getTimeRange(),
                journeyRequest.getRequestedModes());
        return numChangesRange(journeyRequest, numberChanges);
    }

}
