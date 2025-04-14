package com.tramchester.graph.search;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.JourneysForBox;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.collections.RequestStopStream;
import com.tramchester.domain.collections.Running;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.StationsBoxSimpleGrid;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.NumberOfNodesAndRelationshipsRepository;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.search.diagnostics.CreateJourneyDiagnostics;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
import com.tramchester.graph.search.selectors.BreadthFirstBranchSelectorForGridSearch;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.repository.*;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
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
    private final InterchangeRepository interchangeRepository;

    @Inject
    public RouteCalculatorForBoxes(TramchesterConfig config,
                                   TransportData transportData,
                                   GraphDatabase graphDatabaseService,
                                   PathToStages pathToStages,
                                   ProvidesNow providesNow,
                                   MapPathToLocations mapPathToLocations,
                                   BetweenRoutesCostRepository routeToRouteCosts,
                                   ClosedStationsRepository closedStationsRepository, RunningRoutesAndServices runningRoutesAndService,
                                   @SuppressWarnings("unused") RouteCostCalculator routeCostCalculator,
                                   StationAvailabilityRepository stationAvailabilityRepository, CreateJourneyDiagnostics failedJourneyDiagnostics, NumberOfNodesAndRelationshipsRepository countsNodes, InterchangeRepository interchangeRepository) {
        super(pathToStages, graphDatabaseService,
                providesNow, mapPathToLocations,
                transportData, config, routeToRouteCosts, failedJourneyDiagnostics,
                stationAvailabilityRepository, false, countsNodes);
        this.config = config;
        this.graphDatabaseService = graphDatabaseService;
        this.closedStationsRepository = closedStationsRepository;
        this.runningRoutesAndService = runningRoutesAndService;
        this.interchangeRepository = interchangeRepository;
    }

    public RequestStopStream<JourneysForBox> calculateRoutes(final StationsBoxSimpleGrid destinationBox, final JourneyRequest journeyRequest,
                                                             final List<StationsBoxSimpleGrid> startingBoxes) {
        logger.info("Finding routes from " + startingBoxes.size() + " bounding boxes");

        // TODO Compute over a range of times??

        final LocationSet<Station> destinations = destinationBox.getStations();
        final Set<GraphNodeId> destinationNodeIds = getDestinationNodeIds(destinations);

        final TowardsDestination towardsDestination = new TowardsDestination(destinations);

        final long maxNumberOfJourneys = journeyRequest.getMaxNumberOfJourneys();

        final Duration maxInitialWait = RouteCalculatorSupport.getMaxInitialWaitFor(startingBoxes, config);
        final TimeRange timeRange = journeyRequest.getJourneyTimeRange(maxInitialWait);

        final JourneyConstraints journeyConstraints = createJourneyConstraints(destinations, journeyRequest, timeRange);

        // share selector across queries, to allow caching of station to station distances
        // TODO Optimise using box based distance calculation? -- avoid doing per station per box
        final BranchOrderingPolicy selector = getBranchOrderingPolicy(destinationBox, startingBoxes);

        final RequestStopStream<JourneysForBox> result = new RequestStopStream<>();

        final ChangesForDestinations changesForDestinations = new ChangesForDestinations(destinations, journeyRequest);

        final ServiceReasons serviceReasons = createServiceReasons(journeyRequest);

        scheduleLogging(result, serviceReasons);

        final Stream<JourneysForBox> stream = startingBoxes.parallelStream().
                filter(item -> result.isRunning()).
                map(startBox -> {

            if (logger.isDebugEnabled()) {
                logger.debug(format("Finding shortest path for %s --> %s for %s", startBox, destinations, journeyRequest));
            }
            final LocationSet<Station> startingStations = startBox.getStations();
            final LowestCostSeen lowestCostSeenForBox = new LowestCostSeen();

            final AtomicInteger journeyIndex = new AtomicInteger(0);

            try (final GraphTransaction txn = graphDatabaseService.beginTx()) {

                final Stream<Journey> journeys = startingStations.stream().
                        filter(start -> !destinations.contains(start)).
                        map(start -> createNodeAndStation(txn, start)).
                        flatMap(nodeAndStation -> changesForDestinations.getNumberOfChangesRange(startBox, timeRange).
                                map(numChanges -> createPathRequest(journeyRequest, nodeAndStation,  numChanges, journeyConstraints, selector))).

                        filter(item -> result.isRunning()).
                        flatMap(pathRequest -> findShortestPath(txn, serviceReasons, pathRequest, createPreviousVisits(journeyRequest),
                                lowestCostSeenForBox, destinations, towardsDestination, destinationNodeIds, result)).
                        filter(item -> result.isRunning()).
                        map(timedPath -> createJourney(journeyRequest, timedPath, towardsDestination, journeyIndex, txn));

                final Set<Journey> collect = journeys.
                        filter(journey -> !journey.getStages().isEmpty()).
                        limit(maxNumberOfJourneys).
                        collect(Collectors.toSet());

                // yielding
                return new JourneysForBox(startBox, collect);
            }
        });

        return result.setStream(stream);
    }

    @SuppressWarnings("unchecked")
    private static @NotNull BranchOrderingPolicy getBranchOrderingPolicy(final StationsBoxSimpleGrid destinationBox, final List<StationsBoxSimpleGrid> boxes) {
        return (startBranch, expander) -> new BreadthFirstBranchSelectorForGridSearch(startBranch, expander,
                destinationBox, boxes);
    }

    private static void scheduleLogging(final Running results, final ServiceReasons serviceReasons) {
        final Timer timer = new Timer("GridSearchLoggingTimer");

        final long logFrequency = Duration.ofMinutes(1).toMillis();
        final TimerTask loggingTask = new TimerTask() {
            @Override
            public void run() {
                if (results.isRunning()) {
                    serviceReasons.logCounters();
                } else {
                    timer.cancel();
                }
            }
        };
        timer.scheduleAtFixedRate(loggingTask, logFrequency, logFrequency);
    }

    private class ChangesForDestinations {
        private final LocationSet<Station> destinations;
        private final JourneyRequest journeyRequest;
        private final Cache<BoundingBoxWithStations, Integer> cache;

        private ChangesForDestinations(LocationSet<Station> destinations, JourneyRequest journeyRequest) {
            this.destinations = destinations;
            this.journeyRequest = journeyRequest;
            cache = Caffeine.newBuilder().build();
        }

        public Stream<Integer> getNumberOfChangesRange(final BoundingBoxWithStations box, final TimeRange range) {
            final int possibleMinChanges = cache.get(box, theBox -> computeNumberOfChanges(box, range));
            return numChangesRange(journeyRequest, possibleMinChanges);
        }

        private int computeNumberOfChanges(BoundingBoxWithStations box, TimeRange timeRange) {
            return routeToRouteCosts.getNumberOfChanges(box.getStations(), destinations, journeyRequest, timeRange);
        }

    }

    @NotNull
    private JourneyConstraints createJourneyConstraints(final LocationSet<Station> destinations, final JourneyRequest journeyRequest, TimeRange timeRange) {
        final TramDate date = journeyRequest.getDate();

        final TimeRange destinationsAvailable = getDestinationsAvailable(destinations, date);
        final LowestCostsForDestRoutes lowestCostForDestinations = routeToRouteCosts.getLowestCostCalculatorFor(destinations, journeyRequest,
                timeRange);
        final RunningRoutesAndServices.FilterForDate routeAndServicesFilter = runningRoutesAndService.getFor(date, journeyRequest.getRequestedModes());

        final Set<ClosedStation> closedStations = closedStationsRepository.getAnyWithClosure(date);

        EnumSet<TransportMode> destinationsModes = interchangeRepository.getInterchangeModes(destinations);

        return new JourneyConstraints(config, routeAndServicesFilter, closedStations,
                destinationsModes, lowestCostForDestinations, journeyRequest.getMaxJourneyDuration(), destinationsAvailable);
    }


}
