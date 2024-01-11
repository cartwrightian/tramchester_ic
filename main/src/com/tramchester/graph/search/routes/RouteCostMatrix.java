package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.ComponentThatCaches;
import com.tramchester.caching.DataCache;
import com.tramchester.caching.FileDataCache;
import com.tramchester.dataexport.HasDataSaver;
import com.tramchester.dataimport.data.CostsPerDegreeData;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NumberOfRoutes;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class RouteCostMatrix extends ComponentThatCaches<CostsPerDegreeData, RouteCostMatrix.CostsPerDegree> {
    private static final Logger logger = LoggerFactory.getLogger(RouteCostMatrix.class);

    public static final byte MAX_VALUE = Byte.MAX_VALUE;

    public static final int MAX_DEPTH = 5;

    private final InterchangeRepository interchangeRepository;
    private final GraphFilterActive graphFilter;
    private final RouteIndex routeIndex;

    private final int numRoutes;
    private final CostsPerDegree costsForDegree;
    private final RouteInterconnectRepository routeInterconnectRepository;

    @Inject
    public RouteCostMatrix(NumberOfRoutes numberOfRoutes, InterchangeRepository interchangeRepository, DataCache dataCache,
                    GraphFilterActive graphFilter, RouteIndexPairFactory pairFactory, RouteIndex routeIndex) {
        super(dataCache, CostsPerDegreeData.class);
        this.interchangeRepository = interchangeRepository;
        this.graphFilter = graphFilter;
        this.routeIndex = routeIndex;
        this.numRoutes = numberOfRoutes.numberOfRoutes();

        costsForDegree = new CostsPerDegree();
        routeInterconnectRepository = new RouteInterconnectRepository(pairFactory, numRoutes, routeIndex, interchangeRepository, this);
    }

    @PostConstruct
    public void start() {
        logger.info("start");

        final RouteDateAndDayOverlap routeDateAndDayOverlap = new RouteDateAndDayOverlap(routeIndex, numRoutes);
        routeDateAndDayOverlap.populateFor();

        routeInterconnectRepository.start();

        if (graphFilter.isActive()) {
            logger.warn("Filtering is enabled, skipping all caching");
            createCostMatrix(routeDateAndDayOverlap);
        } else {
            if (!super.loadFromCache(costsForDegree)) {
                createCostMatrix(routeDateAndDayOverlap);
            }
        }
        logger.info("CostsPerDegree bits set: " + costsForDegree.numberOfBitsSet());
        createBacktracking(routeDateAndDayOverlap);
        logger.info("started");
    }

    @PreDestroy
    public  void stop() {
        logger.info("stop");
        super.saveCacheIfNeeded(costsForDegree);
        costsForDegree.clear();
        routeInterconnectRepository.clear();
        logger.info("stopped");
    }

    private void createCostMatrix(final RouteDateAndDayOverlap routeDateAndDayOverlap) {
        final IndexedBitSet forDegreeOne = costsForDegree.getDegreeMutable(1);
        addInitialConnectionsFromInterchanges(routeDateAndDayOverlap, forDegreeOne);
        populateCosts(routeDateAndDayOverlap);
    }

    private void addInitialConnectionsFromInterchanges(final RouteDateAndDayOverlap routeDateAndDayOverlap, final IndexedBitSet forDegreeOne) {
        final Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        logger.info("Pre-populate route to route costs from " + interchanges.size() + " interchanges ");

        interchanges.forEach(interchange -> {
            // TODO This does not work for multi-mode station interchanges?
            // record interchanges, where we can go from being dropped off (routes) to being picked up (routes)
            final Set<Route> dropOffAtInterchange = interchange.getDropoffRoutes();
            final Set<Route> pickupAtInterchange = interchange.getPickupRoutes();

            addOverlapsForRoutes(forDegreeOne, routeDateAndDayOverlap, dropOffAtInterchange, pickupAtInterchange);
        });
        logger.info("Add " + numberOfBitsSet() + " bits/connections for interchanges");
    }


    private void createBacktracking(final RouteDateAndDayOverlap routeDateAndDayOverlap) {
        // degree 1 = depth 0 = interchanges directly
        for (int currentDegree = 1; currentDegree <= MAX_DEPTH; currentDegree++) {
            createBacktracking(routeDateAndDayOverlap, currentDegree);
        }
    }

    private void createBacktracking(final RouteDateAndDayOverlap routeDateAndDayOverlap, final int currentDegree) {
        final int totalSize = numRoutes * numRoutes;
        if (currentDegree<1) {
            throw new RuntimeException("Only call for >1 , got " + currentDegree);
        }

        //final int nextDegree = currentDegree + 1;
        final ImmutableIndexedBitSet matrixForDegree = costsForDegree.getDegree(currentDegree);

        logger.info("Create backtrack pair map for degree " + currentDegree + " matrixForDegree bits set " + matrixForDegree.numberOfBitsSet());

        if (matrixForDegree.numberOfBitsSet()>0) {
            // zero indexed
            final RouteInterconnectRepository.RouteInterconnects routeInterconnects = routeInterconnectRepository.forDegree(currentDegree);

            final Instant startTime = Instant.now();

            for (short currentRoute = 0; currentRoute < numRoutes; currentRoute++) {
                final short currentRouteIndex = currentRoute;

                final RouteOverlaps dateOverlapsForRoute = routeDateAndDayOverlap.overlapsFor(currentRouteIndex);

                final SimpleImmutableBitmap currentConnections = matrixForDegree.getBitSetForRow(currentRouteIndex);

                currentConnections.getBitIndexes().
                        filter(dateOverlapsForRoute::get). // true if route runs on date
                        forEach(connectedRoute -> {
                            final RouteOverlaps dateOverlapsForConnectedRoute = routeDateAndDayOverlap.overlapsFor(connectedRoute);
                            final SimpleImmutableBitmap intermediates = matrixForDegree.getBitSetForRow(connectedRoute);
                            routeInterconnects.addLinksBetween(currentRouteIndex, connectedRoute, intermediates,
                                    dateOverlapsForRoute, dateOverlapsForConnectedRoute);
                });
            }

            final long took = Duration.between(startTime, Instant.now()).toMillis();
            final int added = routeInterconnects.numberOfLinks();
            final double percentage = ((double)added)/((double)totalSize) * 100D;
            logger.info(String.format("Added backtrack pairs %s (%s %%) Degree %s in %s ms",
                    added, percentage, currentDegree, took));

        } else {
            logger.info("No bits set for degree " + currentDegree);
        }
    }

    public int getNumberBacktrackFor(final int depth) {
        return routeInterconnectRepository.forDegree(depth).numberOfLinks();
    }

    private void addOverlapsForRoutes(final IndexedBitSet forDegreeOne, final RouteDateAndDayOverlap routeDateAndDayOverlap,
                                      final Set<Route> dropOffAtInterchange, final Set<Route> pickupAtInterchange) {
        for (final Route dropOff : dropOffAtInterchange) {
            final short dropOffIndex = routeIndex.indexFor(dropOff.getId());
            // todo, could use bitset Or and And with DateOverlapMask here
            for (final Route pickup : pickupAtInterchange) {
                if ((!dropOff.equals(pickup)) && pickup.isDateOverlap(dropOff)) {
                    final int pickupIndex = routeIndex.indexFor(pickup.getId());
                    forDegreeOne.set(dropOffIndex, pickupIndex);
                }
            }
            // apply dates and days
            forDegreeOne.applyAndToRow(dropOffIndex, routeDateAndDayOverlap.overlapsFor(dropOffIndex).getBitSet());
        }
    }

    // create a bitmask for route->route changes that are possible on a given date and transport mode
    public IndexedBitSet createOverlapMatrixFor(TramDate date, Set<TransportMode> requestedModes) {

        final Set<Short> availableOnDate = new HashSet<>();
        for (short routeIndex = 0; routeIndex < numRoutes; routeIndex++) {
            final Route route = this.routeIndex.getRouteFor(routeIndex);
            if (route.isAvailableOn(date) && requestedModes.contains(route.getTransportMode())) {
                availableOnDate.add(routeIndex);
            }
        }

        IndexedBitSet result = IndexedBitSet.Square(numRoutes);
        for (short firstRouteIndex = 0; firstRouteIndex < numRoutes; firstRouteIndex++) {
            SimpleBitmap row = SimpleBitmap.create(numRoutes);
            if (availableOnDate.contains(firstRouteIndex)) {
                for (short secondRouteIndex = 0; secondRouteIndex < numRoutes; secondRouteIndex++) {
                    if (availableOnDate.contains(secondRouteIndex)) {
                        row.set(secondRouteIndex);
                    }
                }
            }
            result.insert(firstRouteIndex, row);
        }
        availableOnDate.clear();

        logger.info(format("created overlap matrix for %s and modes %s with %s entries", date, requestedModes, result.numberOfBitsSet()));
        return result;
    }

    public long numberOfBitsSet() {
        return costsForDegree.numberOfBitsSet();
    }

    byte getDegree(final RouteIndexPair routePair) {
        if (routePair.isSame()) {
            return 0;
        }
        for (int degree = 1; degree <= MAX_DEPTH; degree++) {
            if (costsForDegree.isSet(degree, routePair)) {
                return (byte) degree;
            }
        }
        return MAX_VALUE;
    }

    /***
     * Test support, get all degrees where pair is found
     * @param routeIndexPair pair to fetch all degrees for
     * @return list of matches
     */
    public List<Integer> getAllDegrees(final RouteIndexPair routeIndexPair) {
        if (routeIndexPair.isSame()) {
            throw new RuntimeException("Don't call with same routes");
        }
        final List<Integer> results = new ArrayList<>();
        for (int degree = 1; degree <= MAX_DEPTH; degree++) {
            if (costsForDegree.isSet(degree, routeIndexPair)) {
                results.add(degree);
            }
        }
        return results;
    }

    private boolean isOverlap(final ImmutableIndexedBitSet bitSet, final RouteIndexPair pair) {
        return bitSet.isSet(pair);
    }

    private void populateCosts(RouteDateAndDayOverlap routeDateAndDayOverlap) {
        final int size = numRoutes;
        final int fullyConnected = size * size;

        logger.info("Find costs between " + size + " routes (" + fullyConnected + ")");

        long previousTotal = 0;
        for (byte currentDegree = 1; currentDegree <= MAX_DEPTH; currentDegree++) {
            addConnectionsFor(routeDateAndDayOverlap, currentDegree);
            final long currentTotal = numberOfBitsSet();
            logger.info("Total number of connections " + currentTotal);
            if (currentTotal >= fullyConnected) {
                break;
            }
            if (previousTotal==currentTotal) {
                logger.warn(format("No improvement in connections at degree %s and number %s", currentDegree, currentTotal));
                break;
            }
            previousTotal = currentTotal;
        }

        final long finalSize = numberOfBitsSet();
        logger.info("Added cost for " + finalSize + " route combinations");
        if (finalSize < fullyConnected) {
            double percentage = ((double) finalSize / (double) fullyConnected);
            logger.warn(format("Not fully connected, only %s (%s) of %s ", finalSize, percentage, fullyConnected));
        } else {
            logger.info(format("Fully connected, with %s of %s ", finalSize, fullyConnected));
        }
    }

    // based on the previous degree and connections, add further connections at current degree which are
    // enabled by the previous degree. For example if degree 1 has: R1->R2 at IntA and R2->R3 at IntB then
    // at degree 2 we have: R1->R3
    // implementation uses a bitmap to do this computation quickly row by row
    private void addConnectionsFor(RouteDateAndDayOverlap routeDateAndDayOverlap, byte currentDegree) {
        final Instant startTime = Instant.now();
        final int nextDegree = currentDegree + 1;

        final ImmutableIndexedBitSet currentMatrix = costsForDegree.getDegree(currentDegree);
        final IndexedBitSet newMatrix = costsForDegree.getDegreeMutable(nextDegree);

        for (short route = 0; route < numRoutes; route++) {
            final SimpleBitmap resultForForRoute = SimpleBitmap.create(numRoutes);
            final SimpleImmutableBitmap currentConnectionsForRoute = currentMatrix.getBitSetForRow(route);

            currentConnectionsForRoute.getBitIndexes().forEach(connectedRoute -> {
                // if current route is connected to another route, then for next degree include that other route's connections
                final SimpleImmutableBitmap otherRoutesConnections = currentMatrix.getBitSetForRow(connectedRoute);
                //otherRoutesConnections.applyOrTo(resultForForRoute);
                resultForForRoute.or(otherRoutesConnections);
            });

            final RouteOverlaps dateOverlapMask = routeDateAndDayOverlap.overlapsFor(route);  // only those routes whose dates overlap
            resultForForRoute.and(dateOverlapMask.getBitSet());

            final SimpleImmutableBitmap allExistingConnectionsForRoute = getExistingBitSetsForRoute(route, currentDegree);

            resultForForRoute.andNot(allExistingConnectionsForRoute);
            //allExistingConnectionsForRoute.applyAndNotTo(resultForForRoute);  // don't include any current connections for this route

            newMatrix.insert(route, resultForForRoute);
        }

        final long took = Duration.between(startTime, Instant.now()).toMillis();
        logger.info("Added " + newMatrix.numberOfBitsSet() + " connections for  degree " + nextDegree + " in " + took + " ms");
    }

    public SimpleImmutableBitmap getExistingBitSetsForRoute(final int routeIndex, final int startingDegree) {
        final SimpleBitmap result = SimpleBitmap.create(numRoutes);

        for (int degree = startingDegree; degree > 0; degree--) {
            final ImmutableIndexedBitSet allConnectionsAtDegree = costsForDegree.getDegree(degree);
            final SimpleImmutableBitmap existingConnectionsAtDepth = allConnectionsAtDegree.getBitSetForRow(routeIndex);
            result.or(existingConnectionsAtDepth);
        }

        return result;
    }

    public int getConnectionDepthFor(Route routeA, Route routeB) {
        RouteIndexPair routePair = routeIndex.getPairFor(RoutePair.of(routeA, routeB));
        return getDegree(routePair);
    }

    public PathResults getInterchangesFor(final RouteIndexPair indexPair, final ImmutableIndexedBitSet dateOverlaps,
                                          final Function<InterchangeStation, Boolean> interchangeFilter) {
        final int degree = getDegree(indexPair);

        if (degree==Byte.MAX_VALUE) {
            logger.warn("No degree found for " + routeIndex.getPairFor(indexPair));
            return new PathResults.NoPathResults();
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Get interchanges for %s with initial degree %s", HasId.asIds(routeIndex.getPairFor(indexPair)), degree));
        }

        final ImmutableIndexedBitSet changesForDegree = costsForDegree.getDegree(degree).getCopyOfRowAndColumn(indexPair.first(), indexPair.second());
        // apply mask to filter out unavailable dates/modes quickly
        final IndexedBitSet withDateApplied = IndexedBitSet.and(changesForDegree, dateOverlaps);

        if (withDateApplied.isSet(indexPair)) {

            QueryPathsWithDepth.QueryPath pathFor = getPathFor(indexPair, degree, interchangeFilter);
            if (pathFor.hasAny()) {
                return new PathResults.HasPathResults(pathFor);
            } else {
                return new PathResults.NoPathResults();
            }

        } else {
            return new PathResults.NoPathResults();
        }
    }

    private QueryPathsWithDepth.QueryPath getPathFor(final RouteIndexPair indexPair, final int currentDegree,
                                                     final Function<InterchangeStation, Boolean> interchangeFilter) {

        if (currentDegree==1) {
            if (!interchangeRepository.hasInterchangeFor(indexPair)) {
                final RoutePair routePair = routeIndex.getPairFor(indexPair);
                final String msg = "Unable to find interchange for " + HasId.asIds(routePair);
                logger.error(msg);
                logger.warn("Full pair first:" + routePair.first() + " second: " + routePair.second());
                if (!routePair.isDateOverlap()) {
                    logger.error("Further: No date overlap between " + HasId.asIds(routePair));
                }
                throw new RuntimeException(msg);
            }

            // can be multiple interchanges points between a pair of routes
            final Set<InterchangeStation> changes = interchangeRepository.getInterchangesFor(indexPair).
                    filter(interchangeFilter::apply).
                    collect(Collectors.toSet());

            if (changes.isEmpty()) {
                return QueryPathsWithDepth.ZeroPaths.get();
            } else {
                return QueryPathsWithDepth.AnyOfInterchanges.Of(changes);
            }
        } else {
            final int lowerDegree = currentDegree-1;
            //final int depth = degree - 1;

            final Stream<Pair<RouteIndexPair, RouteIndexPair>> underlying = routeInterconnectRepository.forDegree(lowerDegree, indexPair);
            // TODO parallel? not required?
            final Set<QueryPathsWithDepth.BothOf> combined = underlying. //.parallel().
                    map(pair -> expandPathFor(pair, lowerDegree, interchangeFilter)).
                    filter(QueryPathsWithDepth.BothOf::hasAny).
                    collect(Collectors.toSet());

            return new QueryPathsWithDepth.AnyOf(combined);
        }
    }

    @NotNull
    private QueryPathsWithDepth.BothOf expandPathFor(final Pair<RouteIndexPair, RouteIndexPair> pair, final int degree,
                                                     final Function<InterchangeStation, Boolean> interchangeFilter) {
        final QueryPathsWithDepth.QueryPath pathA = getPathFor(pair.getLeft(), degree, interchangeFilter);
        final QueryPathsWithDepth.QueryPath pathB = getPathFor(pair.getRight(), degree, interchangeFilter);
        return new QueryPathsWithDepth.BothOf(pathA, pathB);
    }

    // test support
    public Set<Pair<RoutePair, RoutePair>> getBackTracksFor(int degree, RouteIndexPair indexPair) {
        return routeInterconnectRepository.forDegree(degree, indexPair).
                map(pair -> Pair.of(routeIndex.getPairFor(pair.getLeft()), routeIndex.getPairFor(pair.getRight()))).
                collect(Collectors.toSet());
    }

    public ImmutableIndexedBitSet getCostsPerDegree(int degree) {
        return costsForDegree.getDegree(degree);
    }


    public static class RouteDateAndDayOverlap {
        // Create a bitmask corresponding to the dates and days routes overlap
        // NOTE: this is for route overlaps only

        private final SimpleBitmap[] overlapMasks;
        private final int numberOfRoutes;
        private final RouteIndex index;
        private int numberSet = -1;

        public RouteDateAndDayOverlap(RouteIndex index, int numberOfRoutes) {
            this.index = index;
            this.numberOfRoutes = numberOfRoutes;
            overlapMasks = new SimpleBitmap[numberOfRoutes];
        }

        public void populateFor() {
            logger.info("Creating matrix for route date/day overlap");
            numberSet = 0;

            for (short i = 0; i < numberOfRoutes; i++) {
                final Route from = index.getRouteFor(i);
                final SimpleBitmap resultsForRoute = SimpleBitmap.create(numberOfRoutes);
                final int fromIndex = i;
                // thread safety: split into list and then application of list to bitset
                final List<Integer> toSet = IntStream.range(0, numberOfRoutes).
                        //parallel().
                        filter(toIndex -> (fromIndex == toIndex) || from.isDateOverlap(index.getRouteFor((short)toIndex))).
                        boxed().toList();
                numberSet = numberSet + toSet.size();
                toSet.forEach(resultsForRoute::set);
                overlapMasks[i] = resultsForRoute;
            }
            logger.info("Finished matrix for route date/day overlap, added " + numberSet + " overlaps");
        }

        public RouteOverlaps overlapsFor(final short routeIndex) {
            if (numberSet<0) {
                throw new RuntimeException("populate first");
            }
            return new RouteOverlaps(routeIndex, overlapMasks[routeIndex]);
        }

        public int numberBitsSet() {
            return numberSet;
        }
    }

    public static class RouteOverlaps {
        final short routeIndex;
        final SimpleImmutableBitmap overlaps;

        private RouteOverlaps(short routeIndex, SimpleImmutableBitmap overlaps) {
            this.routeIndex = routeIndex;
            this.overlaps = overlaps;
        }

        public boolean get(final short otherRouteIndex) {
            return overlaps.get(otherRouteIndex);
        }

        public SimpleImmutableBitmap getBitSet() {
            return overlaps;
        }
    }


    /***
     * encapsulate cost per degree to facilitate caching
     */
    public class CostsPerDegree implements FileDataCache.CachesData<CostsPerDegreeData> {

        private final IndexedBitSet[] bitSets;

        private CostsPerDegree() {
            bitSets = new IndexedBitSet[MAX_DEPTH+1];

            for (int depth = 0; depth <= MAX_DEPTH; depth++) {
                bitSets[depth] = IndexedBitSet.Square(numRoutes);
            }
        }

        // degreeIndex runs 1, 2, 3,....
        public ImmutableIndexedBitSet getDegree(int degree) {
            return bitSets[degree-1];
        }

        public IndexedBitSet getDegreeMutable(int degree) {
            return bitSets[degree-1];
        }

        public void clear() {
            for (int depth = 0; depth < MAX_DEPTH; depth++) {
                bitSets[depth].clear();
            }
        }

        public boolean isSet(final int degree, final RouteIndexPair routePair) {
            return isOverlap(getDegree(degree), routePair);
        }

        public long numberOfBitsSet() {
            long result = 0;
            for (int depth = 0; depth < MAX_DEPTH; depth++) {
                result = result + bitSets[depth].numberOfBitsSet();
            }
            return result;
        }

        @Override
        public void cacheTo(HasDataSaver<CostsPerDegreeData> hasDataSaver) {
            try (HasDataSaver.ClosableDataSaver<CostsPerDegreeData> saver = hasDataSaver.get()) {
                for (int depth = 0; depth < MAX_DEPTH; depth++) {
                    IndexedBitSet bitSet = bitSets[depth];
                    for (int routeIndex = 0; routeIndex < numRoutes; routeIndex++) {
                        SimpleImmutableBitmap bitmapForRow = bitSet.getBitSetForRow(routeIndex);
                        if (bitmapForRow.cardinality() > 0) {
                            List<Short> bitsSetForRow = bitmapForRow.getBitIndexes().collect(Collectors.toList());
                            CostsPerDegreeData item = new CostsPerDegreeData(depth, routeIndex, bitsSetForRow);
                            saver.write(item);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Exception while writing cache",e);
            }

        }

        @Override
        public String getFilename() {
            return "costs_per_degree.csv";
        }

        @Override
        public void loadFrom(Stream<CostsPerDegreeData> stream) {
            AtomicInteger counter = new AtomicInteger(0);
            stream.forEach(item -> {
                counter.getAndIncrement();
                final int index = item.getIndex();
                final int routeIndex = item.getRouteIndex();
                final List<Short> setBits = item.getSetBits();
                final IndexedBitSet bitset = bitSets[index];
                setBits.forEach(bit -> bitset.set(routeIndex, bit));
            });
            logger.info("Loaded " + counter.get() + " items from cache");
        }


    }


}
