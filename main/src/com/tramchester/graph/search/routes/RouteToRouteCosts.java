package com.tramchester.graph.search.routes;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.RouteIndexPairFactory;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.graph.search.BetweenRoutesCostRepository;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.ReportsCacheStats;
import com.tramchester.repository.StationAvailabilityRepository;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteToRouteCosts implements BetweenRoutesCostRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteToRouteCosts.class);

    public final static String INDEX_FILE = "route_index.json";

    private final NeighboursRepository neighboursRepository;
    private final StationAvailabilityRepository availabilityRepository;
    private final ClosedStationsRepository closedStationsRepository;
    private final RouteIndex index;
    private final RouteCostMatrix costs;
    private final RouteInterconnectRepository routeInterconnectRepository;
    private final RouteIndexPairFactory pairFactory;

    @Inject
    public RouteToRouteCosts(NeighboursRepository neighboursRepository, StationAvailabilityRepository availabilityRepository,
                             ClosedStationsRepository closedStationsRepository, RouteIndex index, RouteCostMatrix costs,
                             RouteInterconnectRepository routeInterconnectRepository,
                             RouteIndexPairFactory pairFactory) {
        this.neighboursRepository = neighboursRepository;
        this.availabilityRepository = availabilityRepository;
        this.closedStationsRepository = closedStationsRepository;
        this.index = index;
        this.costs = costs;
        this.routeInterconnectRepository = routeInterconnectRepository;

        this.pairFactory = pairFactory;
    }

    @PostConstruct
    public void start() {
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stopped");
    }

    private int getNumberChangesFor(final RoutePair routePair, final TramDate date,
                                    final StationAvailabilityFacade changeStationOperating,
                                    final IndexedBitSet dateAndModeOverlaps) {
        if (routePair.areSame()) {
            return 0;
        }
        if (!routePair.bothAvailableOn(date)) {
            logger.debug(format("Routes %s not available on date %s", date, routePair));
            return Integer.MAX_VALUE;
        }

        final RouteIndexPair routeIndexPair = index.getPairFor(routePair);
        final int result = getDepth(routeIndexPair, changeStationOperating, dateAndModeOverlaps);

        if (result == RouteCostMatrix.MAX_VALUE) {
            if (routePair.sameMode()) {
                // TODO Why so many hits here?
                // for mixed transport mode having no value is quite normal
                logger.debug("Missing " + routePair);
            }
            return Integer.MAX_VALUE;
        }
        return result;
    }

    private int getDepth(final RouteIndexPair routePair, final StationAvailabilityFacade changeStationOperating,
                         final IndexedBitSet dateAndModeOverlaps) {

        // need to account for route availability and modes when getting the depth

        final PathResults results = routeInterconnectRepository.getInterchangesFor(routePair, dateAndModeOverlaps,
                changeStationOperating::isOperating);

        if (results.hasAny()) {
            return results.getDepth();
        }

        if (logger.isDebugEnabled()) {
            RoutePair pair = index.getPairFor(routePair);
            logger.debug("Found no operating station for " + HasId.asIds(pair));
        }
        return Integer.MAX_VALUE;
    }

    public long size() {
        return costs.numberOfBitsSet();
    }

    public int getPossibleMinChanges(StationGroup start, StationGroup end, TramDate date, TimeRange time,
                                     EnumSet<TransportMode> modes) {
        return getPossibleMinChanges(start.getAllContained(), end.getAllContained(), date, time, modes);
    }

//    @Override
//    public int getNumberOfChanges(StationGroup start, StationGroup end, JourneyRequest journeyRequest) {
//        return getPossibleMinChanges(start, end, journeyRequest.getDate(), journeyRequest.getTimeRange(),
//                journeyRequest.getRequestedModes());
//    }

    @Override
    public int getNumberOfChanges(LocationSet<Station> starts, LocationSet<Station> destinations,
                                  JourneyRequest journeyRequest, TimeRange timeRange) {
        return getPossibleMinChanges(starts, destinations, journeyRequest.getDate(), timeRange,
                journeyRequest.getRequestedModes());
    }

    public int getPossibleMinChanges(final LocationSet<Station> starts, final LocationSet<Station> destinations,
                                     final TramDate date, final TimeRange timeRange,
                                     final EnumSet<TransportMode> requestedModes) {

        final Set<Route> startRoutes = pickupRoutesFor(starts, date, timeRange, requestedModes);
        if (startRoutes.isEmpty()) {
            logger.warn(format("start stations %s not available at %s and %s ", HasId.asIds(starts), date, timeRange));
            return Integer.MAX_VALUE;
        }

        final Set<Route> endRoutes = dropoffRoutesFor(destinations, date, timeRange, requestedModes);
        if (endRoutes.isEmpty()) {
            logger.warn(format("destination stations %s not available at %s and %s ", HasId.asIds(starts), date, timeRange));
            return Integer.MAX_VALUE;
        }

        final StationAvailabilityFacade availabilityFacade = getAvailabilityFacade(availabilityRepository, date, timeRange, requestedModes);

        if (neighboursRepository.areNeighbours(starts, destinations)) {
            return 0;
        }
        // todo account for closures, or covered by fact a set of locations is available here?
        return getNumberOfHops(startRoutes, endRoutes, date, availabilityFacade, 0, requestedModes);
    }

    @NotNull
    private static StationAvailabilityFacade getAvailabilityFacade(final StationAvailabilityRepository availabilityRepository,
                                                                   final TramDate date, final TimeRange timeRange,
                                                                   final EnumSet<TransportMode> requestedModes) {
        return new StationAvailabilityFacade(availabilityRepository, date, timeRange, requestedModes);
    }

    @Override
    public int getNumberOfChanges(final Location<?> start, final Location<?> destination, final JourneyRequest journeyRequest, TimeRange timeRange) {
        return getPossibleMinChanges(start, destination, journeyRequest.getRequestedModes(), journeyRequest.getDate(),
                timeRange);
    }

    @Override
    public int getNumberOfChanges(final Location<?> start, final LocationSet<Station> destinations,
                                  final JourneyRequest journeyRequest, TimeRange timeRange) {
        final TramDate date = journeyRequest.getDate();
        //final TimeRange timeRange = journeyRequest.getTimeRange();
        final EnumSet<TransportMode> preferredModes = journeyRequest.getRequestedModes();

        final Set<Route> pickupRoutes = availabilityRepository.getPickupRoutesFor(start, date, timeRange, preferredModes);
        final Set<Route> dropoffRoutes = availabilityRepository.getDropoffRoutesFor(destinations, date, timeRange, preferredModes);

        final int closureOffset = getClosureOffset(start, destinations, date);

        final StationAvailabilityFacade availabilityFacade = new StationAvailabilityFacade(availabilityRepository, date,
                timeRange, preferredModes);
        return getNumberOfHops(pickupRoutes, dropoffRoutes, date, availabilityFacade, closureOffset, preferredModes);
    }

    @Override
    public int getNumberOfChanges(final LocationSet<Station> starts, final Location<?> destination,
                                  final JourneyRequest journeyRequest, TimeRange timeRange) {
        final TramDate date = journeyRequest.getDate();
        //final TimeRange timeRange = journeyRequest.getTimeRange();
        final EnumSet<TransportMode> preferredModes = journeyRequest.getRequestedModes();

        final Set<Route> pickupRoutes = availabilityRepository.getPickupRoutesFor(starts, date, timeRange, preferredModes);
        final Set<Route> dropoffRoutes = availabilityRepository.getDropoffRoutesFor(destination, date, timeRange, preferredModes);

        final int closureOffset = getClosureOffset(destination, starts, date);

        final StationAvailabilityFacade availabilityFacade = new StationAvailabilityFacade(availabilityRepository, date, timeRange, preferredModes);
        return getNumberOfHops(pickupRoutes, dropoffRoutes, date, availabilityFacade, closureOffset, preferredModes);
    }

    public int getPossibleMinChanges(final Location<?> startLocation, final Location<?> destLocation,
                                     final EnumSet<TransportMode> preferredModes, final TramDate date, final TimeRange timeRange) {

        if (preferredModes.isEmpty()) {
            throw new RuntimeException("Must provide preferredModes");
        }

        // should be captured correctly in the route matrix, but if filtering routes by transport mode/date/time-range
        // might miss a direct walk incorrectly at the start
        if (neighboursRepository.areNeighbours(startLocation, destLocation)) {
            logger.info(format("Number of changes set to 1 since %s and %s are neighbours", startLocation.getId(), destLocation.getId()));
            return 0;
        }

        final int closureOffset = getClosureOffset(startLocation, destLocation, date);

        // Need to respect timing here, otherwise can find a route that is valid at an interchange but isn't
        // actually running from the start or destination
        final Set<Route> pickupRoutes = availabilityRepository.getPickupRoutesFor(startLocation, date, timeRange, preferredModes);
        final Set<Route> dropoffRoutes = availabilityRepository.getDropoffRoutesFor(destLocation, date, timeRange, preferredModes);

        // TODO If the station is a partial closure or full closure AND walking diversions exist, then need
        // to calculate routes from those neighbours?
        // OR create fake routes?

        logger.info(format("Compute number of changes between %s (%s) and %s (%s) using modes '%s' on %s within %s",
                startLocation.getId(), HasId.asIds(pickupRoutes), destLocation.getId(), HasId.asIds(dropoffRoutes),
                preferredModes, date, timeRange));

        final StationAvailabilityFacade changeStationOperating = getAvailabilityFacade(availabilityRepository, date, timeRange, preferredModes);

        if (pickupRoutes.isEmpty()) {
            logger.warn(format("start location %s has no matching pick-up routes for %s %s %s",
                    startLocation.getId(), date, timeRange, preferredModes));
            return Integer.MAX_VALUE;
        }
        if (dropoffRoutes.isEmpty()) {
            logger.warn(format("destination location %s has no matching drop-off routes for %s %s %s",
                    destLocation.getId(), date, timeRange, preferredModes));
            return Integer.MAX_VALUE;
        }

        return getNumberOfHops(pickupRoutes, dropoffRoutes, date, changeStationOperating, closureOffset, preferredModes);

    }

    private int getClosureOffset(final Location<?> start, final LocationSet<Station> destinations, final TramDate date) {
        final boolean startClosed = closedStationsRepository.isClosed(start, date);
        final boolean destClosed = closedStationsRepository.allClosed(destinations, date);

        return (startClosed?1:0) + (destClosed?1:0);
    }

    private int getClosureOffset(final Location<?> start, final Location<?> dest, final TramDate date) {
        final boolean startClosed = closedStationsRepository.isClosed(start, date);
        final boolean destClosed = closedStationsRepository.isClosed(dest, date);

        return (startClosed?1:0) + (destClosed?1:0);
    }

    public int getPossibleMinChanges(final Route routeA, final Route routeB, final TramDate date,
                                     final TimeRange timeRange, final EnumSet<TransportMode> requestedModes) {
        final StationAvailabilityFacade interchangesOperating = getAvailabilityFacade(availabilityRepository, date,
                timeRange, requestedModes);
        return getNumberOfHops(Collections.singleton(routeA), Collections.singleton(routeB), date,
                interchangesOperating, 0, requestedModes);
    }

    @Override
    public LowestCostsForDestRoutes getLowestCostCalculatorFor(final LocationCollection desintationRoutes, final JourneyRequest journeyRequest, TimeRange timeRange) {
        return getLowestCostCalculatorFor(desintationRoutes, journeyRequest.getDate(), timeRange, journeyRequest.getRequestedModes());
    }

    public LowestCostsForDestRoutes getLowestCostCalculatorFor(final LocationCollection destinations, final TramDate date,
                                                               final TimeRange timeRange,
                                                               final EnumSet<TransportMode> requestedModes) {
        final Set<Route> destinationRoutes = destinations.locationStream().
                map(dest -> availabilityRepository.getDropoffRoutesFor(dest, date, timeRange, requestedModes)).
                flatMap(Collection::stream).
                collect(Collectors.toUnmodifiableSet());
        return new LowestCostForDestinations(this, pairFactory, destinationRoutes, date, timeRange,
                requestedModes, availabilityRepository);
    }

    private int getNumberOfHops(final Set<Route> startRoutes, final Set<Route> destinationRoutes, final TramDate date,
                                            final StationAvailabilityFacade interchangesOperating,
                                            final int closureOffset, final EnumSet<TransportMode> requestedModes) {
        if (logger.isDebugEnabled()) {
            logger.debug(format("Compute number of changes between %s and %s on %s",
                    HasId.asIds(startRoutes), HasId.asIds(destinationRoutes), date));
        }

        final IndexedBitSet dateAndModeOverlaps = costs.createOverlapMatrixFor(date, requestedModes);

        final Set<RoutePair> routePairs = getRoutePairs(startRoutes, destinationRoutes);

        // TODO only need min now, refactor?

        final Set<Integer> numberOfChangesForRoutes = routePairs.stream().
                map(pair -> getNumberChangesFor(pair, date, interchangesOperating, dateAndModeOverlaps)).
                collect(Collectors.toSet());

        final int maxDepth = RouteCostMatrix.MAX_DEPTH;

        int minHops = minHops(numberOfChangesForRoutes);
        if (minHops > maxDepth) {
            logger.error(format("Unexpected result for min hops %s greater than max depth %s, for %s to %s, change cache %s",
                    minHops, maxDepth, HasId.asIds(startRoutes), HasId.asIds(destinationRoutes), interchangesOperating));
        } else {
            minHops = minHops + closureOffset;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(format("Computed min number of changes from %s to %s on %s as %s",
                    HasId.asIds(startRoutes), HasId.asIds(destinationRoutes), date, minHops));
            interchangesOperating.reportStats();
        }

        return minHops;
    }

    private int minHops(final Set<Integer> numberOfChangesForRoutes) {
        final Optional<Integer> query = numberOfChangesForRoutes.stream().
                min(Integer::compare);

        if (query.isEmpty()) {
            logger.warn("No minHops found for " + numberOfChangesForRoutes);
        }
        return query.orElse(Integer.MAX_VALUE);
    }

    @NotNull
    private Set<RoutePair> getRoutePairs(final Set<Route> startRoutes, final Set<Route> endRoutes) {
        // note: allow routeA -> routeA here, needed to correctly select minimum later on
        return startRoutes.stream().
                flatMap(startRoute -> endRoutes.stream().map(endRoute -> RoutePair.of(startRoute, endRoute))).
                collect(Collectors.toSet());
    }

    private Set<Route> dropoffRoutesFor(final LocationSet<Station> locations, final TramDate date, final TimeRange timeRange,
                                        final EnumSet<TransportMode> modes) {
        return availabilityRepository.getDropoffRoutesFor(locations, date, timeRange, modes);
    }

    private Set<Route> pickupRoutesFor(final LocationSet<Station> locations, final TramDate date, final TimeRange timeRange,
                                       final EnumSet<TransportMode> modes) {
        return availabilityRepository.getPickupRoutesFor(locations, date, timeRange, modes);
    }


    /***
     * Encapsulates lowest cost and hops for one specific set of destinations, required for performance reasons
     * as looking up destinations during the graph traversal was too costly
     */
    private static class LowestCostForDestinations implements LowestCostsForDestRoutes {
        private final RouteToRouteCosts routeToRouteCosts;
        private final RouteIndexPairFactory pairFactory;
        private final Set<Short> destinationIndexs;
        private final Map<Short, DateRange> destinationRouteDateRange;
        private final StationAvailabilityFacade changeStationOperating;
        private final IndexedBitSet dateOverlaps;

        public LowestCostForDestinations(BetweenRoutesCostRepository routeToRouteCosts, RouteIndexPairFactory pairFactory,
                                         Set<Route> destinationRoutes,
                                         TramDate date, TimeRange time, EnumSet<TransportMode> requestedModes,
                                         StationAvailabilityRepository availabilityRepository) {
            this.routeToRouteCosts = (RouteToRouteCosts) routeToRouteCosts;
            this.pairFactory = pairFactory;
            destinationIndexs = destinationRoutes.stream().
                    map(destination -> this.routeToRouteCosts.index.indexFor(destination.getId())).
                    collect(Collectors.toUnmodifiableSet());
            destinationRouteDateRange = new HashMap<>();

            destinationIndexs.forEach(routeIndex -> {
                final DateRange dateRange = this.routeToRouteCosts.index.getRouteFor(routeIndex).getDateRange();
                destinationRouteDateRange.put(routeIndex, dateRange);
            });

            changeStationOperating = getAvailabilityFacade(availabilityRepository, date, time, requestedModes);
            dateOverlaps = ((RouteToRouteCosts) routeToRouteCosts).costs.createOverlapMatrixFor(date, requestedModes);

        }

        /***
         * find the least number of "hops" between routes to reach a destination route
         * @param startingRoute current position
         * @return min number of hops needed to reach one of the destination routes
         */
        @Override
        public int getFewestChanges(final Route startingRoute) {
            final short indexOfStart = routeToRouteCosts.index.indexFor(startingRoute.getId());
            if (destinationIndexs.contains(indexOfStart)) {
                return 0;
            }

            // note: IntStream uses int in implementation so avoids any boxing overhead, destinationIndexs are shorts
            // so should be safe
            final DateRange startingRouteDateRange = startingRoute.getDateRange();
            return destinationIndexs.stream().
                    filter(index -> destinationRouteDateRange.get(index).overlapsWith(startingRouteDateRange)).
                    mapToInt(item -> item).
                    map(indexOfDest -> routeToRouteCosts.getDepth(pairFactory.get(indexOfStart, (short)indexOfDest),
                            changeStationOperating, dateOverlaps)).
                    filter(result -> result != RouteCostMatrix.MAX_VALUE).
                    min().
                    orElse(Integer.MAX_VALUE);
        }

        @Override
        public <T extends HasId<Route>> Stream<T> sortByDestinations(final Stream<T> startingRoutes) {
            return startingRoutes.
                    map(this::getLowestCost).
                    sorted(Comparator.comparingInt(Pair::getLeft)).
                    map(Pair::getRight);
        }

        @NotNull
        private <T extends HasId<Route>> Pair<Integer, T> getLowestCost(final T start) {
            final short indexOfStart = routeToRouteCosts.index.indexFor(start.getId());
            if (destinationIndexs.contains(indexOfStart)) {
                return Pair.of(0, start); // start on route that is present at destination
            }

            // note: IntStream uses int in implementation so avoids any boxing overhead
            final int result = destinationIndexs.stream().mapToInt(item -> item).
                    map(dest -> routeToRouteCosts.getDepth(pairFactory.get(indexOfStart, (short)dest), changeStationOperating, dateOverlaps)).
                    min().
                    orElse(Integer.MAX_VALUE);
            return Pair.of(result, start);
        }

    }

    /***
     * Needed for rail performance, significant
     */
    static class StationAvailabilityFacade implements ReportsCacheStats {
        private final TramDate date;
        private final TimeRange time;
        private final EnumSet<TransportMode> modes;
        private final StationAvailabilityRepository availabilityRepository;

        private final Cache<IdFor<Station>, Boolean> cache;

        public StationAvailabilityFacade(StationAvailabilityRepository availabilityRepository, TramDate date, TimeRange time,
                                         EnumSet<TransportMode> modes) {
            this.availabilityRepository = availabilityRepository;
            this.date = date;
            this.time = time;
            this.modes = modes;

            final long size = availabilityRepository.size();
            if (logger.isDebugEnabled()) {
                logger.debug("Created cache of size " + size + " for " + date + " " + time + " " + modes);
            }
            cache = Caffeine.newBuilder().
                    //maximumSize(size).
                    //expireAfterAccess(1, TimeUnit.MINUTES).
                    recordStats().build();
        }

        @Override
        public String toString() {
            return "StationAvailabilityFacade{" +
                    "date=" + date +
                    ", time=" + time +
                    ", modes=" + modes +
                    '}';
        }

        public boolean isOperating(final InterchangeStation interchangeStation) {
            final Station station = interchangeStation.getStation();
            return cache.get(station.getId(), unused -> uncached(station));
        }

        private boolean uncached(final Station station) {
            return availabilityRepository.isAvailable(station, date, time, modes);
        }

        @Override
        public List<Pair<String, CacheStats>> stats() {
            final Pair<String, CacheStats> stats = Pair.of("StationAvailabilityFacade", cache.stats());
            return Collections.singletonList(stats);
        }

        public void reportStats() {
            stats().forEach(stat -> logger.info(String.format("%s %s", stat.getLeft(), stat.getRight())));
        }
    }

}
