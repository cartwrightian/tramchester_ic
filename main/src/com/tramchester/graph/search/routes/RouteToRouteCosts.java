package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.SimpleList;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.graph.search.BetweenRoutesCostRepository;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteToRouteCosts implements BetweenRoutesCostRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteToRouteCosts.class);

    public final static String INDEX_FILE = "route_index.csv";

    private final NeighboursRepository neighboursRepository;
    private final StationAvailabilityRepository availabilityRepository;
    private final RouteIndexToInterchangeRepository routePairToInterchange;
    private final RouteIndex index;
    private final RouteCostMatrix costs;

    private final int numberOfRoutes;

    @Inject
    public RouteToRouteCosts(RouteRepository routeRepository, NeighboursRepository neighboursRepository,
                             StationAvailabilityRepository availabilityRepository,
                             RouteIndexToInterchangeRepository routePairToInterchange, RouteIndex index, RouteCostMatrix costs) {
        this.neighboursRepository = neighboursRepository;
        this.availabilityRepository = availabilityRepository;
        this.routePairToInterchange = routePairToInterchange;
        this.index = index;
        this.costs = costs;

        numberOfRoutes = routeRepository.numberOfRoutes();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        logger.info("stopped");
    }

    /***
     * Use methods from BetweenRoutesCostRepository instead of this
     * @param routeA first route
     * @param routeB second route
     * @param timeRange the range within with the route needs ot be available
     * @return number of changes
     */
    public int getNumberChangesFor(Route routeA, Route routeB, LocalDate date, TimeRange timeRange) {
        IndexedBitSet dateOverlaps = costs.createOverlapMatrixFor(date);

        InterchangeOperating interchangeOperating = new InterchangeOperating(date, timeRange);

        RoutePair pair = RoutePair.of(routeA, routeB);
        return getNumberChangesFor(pair, date, interchangeOperating, dateOverlaps);
    }

    public int getNumberChangesFor(RoutePair routePair, LocalDate date, InterchangeOperating interchangeOperating,
                                   IndexedBitSet overlapsForDate) {
        if (routePair.areSame()) {
            return 0;
        }
        if (!routePair.isAvailableOn(date)) {
            logger.debug(format("Routes %s not available on date %s", date, routePair));
            return Integer.MAX_VALUE;
        }

        RouteIndexPair routeIndexPair = index.getPairFor(routePair);
        final int result = getDepth(routeIndexPair, interchangeOperating, overlapsForDate);

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

    public List<List<RouteAndInterchanges>> getChangesFor(Route routeA, Route routeB) {
        RoutePair routePair = new RoutePair(routeA, routeB);

        logger.info("Get change stations betweem " + routePair);

        RouteIndexPair indexPair = index.getPairFor(routePair);

        IndexedBitSet dateOverlaps = IndexedBitSet.getIdentity(numberOfRoutes); // no specific date or time

        Stream<SimpleList<RouteIndexPair>> routeChanges = costs.getChangesFor(indexPair, dateOverlaps);

        List<List<RouteAndInterchanges>> interchanges = routeChanges.
                map(list -> list.stream().map(this::getInterchangeFor).filter(Objects::nonNull)).
                map(onePossibleSetOfChange -> onePossibleSetOfChange.collect(Collectors.toList()))
                .collect(Collectors.toList());

        if (interchanges.isEmpty()) {
            logger.warn(format("Unable to find interchanges between %s", routePair));
        }
        return interchanges;
    }

    private int getDepth(RouteIndexPair routePair, InterchangeOperating interchangeOperating, IndexedBitSet dateOverlaps) {

        final Stream<SimpleList<RouteIndexPair>> possibleChanges = costs.getChangesFor(routePair, dateOverlaps);

        final List<List<RouteAndInterchanges>> smallestFilteredByAvailability = new ArrayList<>();

        final AtomicInteger smallestSeen = new AtomicInteger(Integer.MAX_VALUE);

//        possibleChanges.forEach(listOfChanges -> {
//            final int numberOfChanges = listOfChanges.size();
//            if (numberOfChanges < smallestSeen.get()) {
//                List<RouteAndInterchanges> listOfInterchanges = getRouteAndInterchange(listOfChanges);
//                final boolean available = interchangeOperating.isOperating(availabilityRepository, listOfInterchanges);
//                if (available) {
//                    smallestFilteredByAvailability.add(listOfInterchanges);
//                    smallestSeen.set(numberOfChanges);
//                }
//            }
//        });

        possibleChanges.filter(listOfChanges -> listOfChanges.size() < smallestSeen.get()).
                map(this::getRouteAndInterchange).
                filter(listOfInterchanges -> interchangeOperating.isOperating(availabilityRepository, listOfInterchanges)).
                forEach(listOfInterchanges -> {
                    smallestFilteredByAvailability.add(listOfInterchanges);
                    smallestSeen.set(listOfInterchanges.size());
                });


        Optional<Integer> result = smallestFilteredByAvailability.stream().
                map(List::size).
                min(Integer::compare);

        return result.orElse(Integer.MAX_VALUE);

    }

    private List<RouteAndInterchanges> getRouteAndInterchange(SimpleList<RouteIndexPair> listOfChanges) {
        List<RouteAndInterchanges> result = listOfChanges.stream().
                map(this::getInterchangeFor).
                filter(Objects::nonNull).
                collect(Collectors.toList());
        if (result.isEmpty()) {
            logger.warn("No interchanges found for any of " + listOfChanges);
        }
        return result;
    }

    RouteAndInterchanges getInterchangeFor(RouteIndexPair indexPair) {

        final RoutePair routePair = index.getPairFor(indexPair);

        if (routePairToInterchange.hasInterchangesFor(indexPair)) {

            final Set<Station> changes = routePairToInterchange.getInterchanges(indexPair);
            final RouteAndInterchanges routeAndInterchanges = new RouteAndInterchanges(routePair, changes);
            if (logger.isDebugEnabled()) {
                logger.debug(format("Found changes %s for %s", HasId.asIds(changes), indexPair));
            }
            return routeAndInterchanges;
        }

        // TODO TODO TODO
        logger.error("Did not find any interchanges for " + routePair);
        return null;
    }

    public int size() {
        return costs.size();
    }

    @Override
    public NumberOfChanges getNumberOfChanges(StationGroup start, StationGroup end, LocalDate date, TimeRange time) {
        return getNumberOfChanges(LocationSet.of(start.getContained()), LocationSet.of(end.getContained()), date, time);
    }

    @Override
    public NumberOfChanges getNumberOfChanges(LocationSet starts, LocationSet destinations, LocalDate date, TimeRange timeRange) {

        Set<Route> startRoutes = pickupRoutesFor(starts, date, timeRange);
        Set<Route> endRoutes = dropoffRoutesFor(destinations, date, timeRange);

        if (startRoutes.isEmpty()) {
            logger.warn(format("start stations %s not available at %s and %s ", HasId.asIds(starts), date, timeRange));
            return NumberOfChanges.None();
        }
        if (endRoutes.isEmpty()) {
            logger.warn(format("destination stations %s not available at %s and %s ", HasId.asIds(starts), date, timeRange));
            return NumberOfChanges.None();
        }

        InterchangeOperating interchangesOperating = new InterchangeOperating(date, timeRange);

        if (neighboursRepository.areNeighbours(starts, destinations)) {
            return new NumberOfChanges(1, 1);
        }
        return getNumberOfHops(startRoutes, endRoutes, date, interchangesOperating);
    }

    @Override
    public NumberOfChanges getNumberOfChanges(Location<?> startStation, Location<?> destination,
                                              Set<TransportMode> preferredModes, LocalDate date, TimeRange timeRange) {

        if (neighboursRepository.areNeighbours(startStation, destination)) {
            logger.info(format("Number of changes set to 1 since %s and %s are neighbours", startStation, destination));
            return new NumberOfChanges(1, 1);
        }

        // Need to respect timing here, otherwise can find a route that is valid at an interchange but isn't
        // actually running from the start or destination
        final Set<Route> pickupRoutes = availabilityRepository.getPickupRoutesFor(startStation,date, timeRange);
        final Set<Route> dropoffRoutes = availabilityRepository.getDropoffRoutesFor(destination, date, timeRange);

        logger.info(format("Compute number of changes between %s (%s) and %s (%s) using modes '%s' on %s within %s",
                startStation.getId(), HasId.asIds(pickupRoutes), destination.getId(), HasId.asIds(dropoffRoutes),
                preferredModes, date, timeRange));

        InterchangeOperating interchangesOperating = new InterchangeOperating(date, timeRange);

        if (pickupRoutes.isEmpty()) {
            logger.warn(format("start station %s has no matching pick-up routes", startStation.getId()));
            return NumberOfChanges.None();
        }
        if (dropoffRoutes.isEmpty()) {
            logger.warn(format("destination station %s has no matching drop-off routes", destination.getId()));
            return NumberOfChanges.None();
        }

        if (preferredModes.isEmpty()) {
            return getNumberOfHops(pickupRoutes, dropoffRoutes, date, interchangesOperating);
        } else {
            final Set<Route> filteredPickupRoutes = filterForModes(preferredModes, pickupRoutes);
            final Set<Route> filteredDropoffRoutes = filterForModes(preferredModes, dropoffRoutes);

            if (filteredPickupRoutes.isEmpty() || filteredDropoffRoutes.isEmpty()) {
                logger.warn(format("No paths between routes %s and %s due to preferredModes modes %s, filtering gave %s and %s",
                        HasId.asIds(pickupRoutes), HasId.asIds(dropoffRoutes), preferredModes, HasId.asIds(filteredPickupRoutes),
                        HasId.asIds(filteredDropoffRoutes)));
                return NumberOfChanges.None();
            }

            return getNumberOfHops(filteredPickupRoutes, filteredDropoffRoutes, date, interchangesOperating);
        }

    }

    @NotNull
    private Set<Route> filterForModes(Set<TransportMode> modes, Set<Route> routes) {
        return routes.stream().filter(route -> modes.contains(route.getTransportMode())).collect(Collectors.toSet());
    }

    @Override
    public LowestCostsForDestRoutes getLowestCostCalcutatorFor(LocationSet destinations, LocalDate date, TimeRange timeRange) {
        Set<Route> destinationRoutes = destinations.stream().
                map(dest -> availabilityRepository.getDropoffRoutesFor(dest, date, timeRange)).
                flatMap(Collection::stream).
                collect(Collectors.toUnmodifiableSet());
        return new LowestCostForDestinations(this, destinationRoutes, date, timeRange);
    }

    @NotNull
    private NumberOfChanges getNumberOfHops(Set<Route> startRoutes, Set<Route> destinationRoutes, LocalDate date,
                                            InterchangeOperating interchangesOperating) {
        logger.info(format("Compute number of changes between %s and %s on %s",
                HasId.asIds(startRoutes), HasId.asIds(destinationRoutes), date));

        IndexedBitSet dateOverlaps = costs.createOverlapMatrixFor(date);

        Set<RoutePair> routePairs = getRoutePairs(startRoutes, destinationRoutes);

        Set<Integer> numberOfChangesForRoutes = routePairs.stream().
                map(pair -> getNumberChangesFor(pair, date, interchangesOperating, dateOverlaps)).
                collect(Collectors.toSet());

        int maxDepth = RouteCostMatrix.MAX_DEPTH;

        int minHops = minHops(numberOfChangesForRoutes);
        if (minHops > maxDepth) {
            logger.error(format("Unexpected result for min hops %s greater than max depth %s, for %s to %s",
                    minHops, maxDepth, HasId.asIds(startRoutes), HasId.asIds(destinationRoutes)));
        }

        int maxHops = maxHops(numberOfChangesForRoutes);
        if (maxHops > maxDepth) {
            logger.error(format("Unexpected result for max hops %s greater than max depth %s, for %s to %s",
                    maxHops, maxDepth, HasId.asIds(startRoutes), HasId.asIds(destinationRoutes)));
        }

        NumberOfChanges numberOfChanges = new NumberOfChanges(minHops, maxHops);
        logger.info(format("Computed number of changes from %s to %s on %s as %s",
                HasId.asIds(startRoutes), HasId.asIds(destinationRoutes), date, numberOfChanges));
        return numberOfChanges;
    }

    private int maxHops(Set<Integer> numberOfChangesForRoutes) {
        final Optional<Integer> query = numberOfChangesForRoutes.stream().
                filter(result -> result != Integer.MAX_VALUE).
                max(Integer::compare);

        if (query.isEmpty()) {
            logger.warn("No maxHops found for " + numberOfChangesForRoutes);
        }
        return query.orElse(Integer.MAX_VALUE);
    }

    private int minHops(Set<Integer> numberOfChangesForRoutes) {
        final Optional<Integer> query = numberOfChangesForRoutes.stream().
                min(Integer::compare);

        if (query.isEmpty()) {
            logger.warn("No minHops found for " + numberOfChangesForRoutes);
        }
        return query.orElse(Integer.MAX_VALUE);
    }

    @NotNull
    private Set<RoutePair> getRoutePairs(Set<Route> startRoutes, Set<Route> endRoutes) {
        // note: allow routeA -> routeA here, needed to correctly select minimum later on
        return startRoutes.stream().
                flatMap(startRoute -> endRoutes.stream().map(endRoute -> RoutePair.of(startRoute, endRoute))).
                collect(Collectors.toSet());
    }

    private Set<Route> dropoffRoutesFor(LocationSet locations, LocalDate date, TimeRange timeRange) {
        return availabilityRepository.getDropoffRoutesFor(locations, date, timeRange);
    }

    private Set<Route> pickupRoutesFor(LocationSet locations, LocalDate date, TimeRange timeRange) {
        return availabilityRepository.getPickupRoutesFor(locations, date, timeRange);
    }


    /***
     * Encapsulates lowest cost and hops for one specific set of destinations, required for performance reasons
     * as looking up destinations during the graph traversal was too costly
     */
    private static class LowestCostForDestinations implements LowestCostsForDestRoutes {
        private final RouteToRouteCosts routeToRouteCosts;
        private final Set<Integer> destinationIndexs;
        private final LocalDate date;
        private final TimeRange time;
        private final InterchangeOperating interchangeOperating;
        private final IndexedBitSet dateOverlaps;

        public LowestCostForDestinations(BetweenRoutesCostRepository routeToRouteCosts, Set<Route> destinations, LocalDate date, TimeRange time) {
            this.routeToRouteCosts = (RouteToRouteCosts) routeToRouteCosts;
            destinationIndexs = destinations.stream().
                    map(destination -> this.routeToRouteCosts.index.indexFor(destination.getId())).
                    collect(Collectors.toUnmodifiableSet());
            this.date = date;
            this.time = time;
            interchangeOperating = new InterchangeOperating(date, time);
            dateOverlaps = ((RouteToRouteCosts) routeToRouteCosts).costs.createOverlapMatrixFor(date);

        }

        /***
         * least number of "hops" between routes to reach a destination route
         * @param startingRoute current position
         * @return min number of hops needed to reach one of the destination routes
         */
        @Override
        public int getFewestChanges(Route startingRoute) {
            int indexOfStart = routeToRouteCosts.index.indexFor(startingRoute.getId());
            if (destinationIndexs.contains(indexOfStart)) {
                return 0;
            }

            InterchangeOperating interchangeOperating = new InterchangeOperating(date, time);

            // note: IntStream uses int in implementation so avoids any boxing overhead
            return destinationIndexs.stream().mapToInt(item -> item).
                    map(indexOfDest -> routeToRouteCosts.getDepth(RouteIndexPair.of(indexOfStart, indexOfDest),
                            interchangeOperating, dateOverlaps)).
                    filter(result -> result != RouteCostMatrix.MAX_VALUE).
                    min().
                    orElse(Integer.MAX_VALUE);
        }

        @Override
        public <T extends HasId<Route>> Stream<T> sortByDestinations(Stream<T> startingRoutes) {
            return startingRoutes.
                    map(this::getLowestCost).
                    sorted(Comparator.comparingInt(Pair::getLeft)).
                    map(Pair::getRight);
        }

        @NotNull
        private <T extends HasId<Route>> Pair<Integer, T> getLowestCost(T start) {
            int indexOfStart = routeToRouteCosts.index.indexFor(start.getId());
            if (destinationIndexs.contains(indexOfStart)) {
                return Pair.of(0, start); // start on route that is present at destination
            }

            // note: IntStream uses int in implementation so avoids any boxing overhead
            int result = destinationIndexs.stream().mapToInt(item -> item).
                    map(dest -> routeToRouteCosts.getDepth(RouteIndexPair.of(indexOfStart, dest), interchangeOperating, dateOverlaps)).
                    min().
                    orElse(Integer.MAX_VALUE);
            return Pair.of(result, start);
        }

    }

    /***
     * Caches whether an interchange is available at a specific date and time range
     */
    static class InterchangeOperating {
        private final LocalDate date;
        private final TimeRange time;

        final private Set<RouteAndInterchanges> active;

        public InterchangeOperating(LocalDate date, TimeRange time) {

            this.date = date;
            this.time = time;
            active = new HashSet<>();
        }

        public boolean isOperating(StationAvailabilityRepository availabilityRepository, List<RouteAndInterchanges> changeSet) {
            return changeSet.stream().anyMatch(item -> isOperating(availabilityRepository, item));
        }

        private boolean isOperating(StationAvailabilityRepository availabilityRepository, RouteAndInterchanges routeAndInterchanges) {
            if (active.contains(routeAndInterchanges)) {
                return true;
            }
            boolean available = availabilityRepository.isAvailable(routeAndInterchanges, date, time);
            if (available) {
                active.add(routeAndInterchanges);
            }
            return available;
        }
    }

}