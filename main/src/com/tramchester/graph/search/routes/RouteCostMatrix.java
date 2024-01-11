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
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.NumberOfRoutes;
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
import java.util.stream.Collectors;
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
    private final RouteDateAndDayOverlap routeDateAndDayOverlap;
    private final CostsPerDegree costsPerDegree;

    @Inject
    public RouteCostMatrix(NumberOfRoutes numberOfRoutes, InterchangeRepository interchangeRepository, DataCache dataCache,
                           GraphFilterActive graphFilter, RouteIndex routeIndex,
                           RouteDateAndDayOverlap routeDateAndDayOverlap) {
        super(dataCache, CostsPerDegreeData.class); //caching setup
        this.interchangeRepository = interchangeRepository;
        this.graphFilter = graphFilter;
        this.routeIndex = routeIndex;
        this.numRoutes = numberOfRoutes.numberOfRoutes();
        this.routeDateAndDayOverlap = routeDateAndDayOverlap;

        costsPerDegree = new CostsPerDegree();

    }

    @PostConstruct
    public void start() {
        logger.info("start");

        if (graphFilter.isActive()) {
            logger.warn("Filtering is enabled, skipping all caching");
            createCostMatrix(routeDateAndDayOverlap);
        } else {
            if (!super.loadFromCache(costsPerDegree)) {
                createCostMatrix(routeDateAndDayOverlap);
            }
        }
        logger.info("CostsPerDegree bits set: " + costsPerDegree.numberOfBitsSet());

        logger.info("started");
    }

    @PreDestroy
    public  void stop() {
        logger.info("stop");
        super.saveCacheIfNeeded(costsPerDegree);
        costsPerDegree.clear();
        logger.info("stopped");
    }

    private void createCostMatrix(final RouteDateAndDayOverlap routeDateAndDayOverlap) {
        final IndexedBitSet forDegreeOne = costsPerDegree.getDegreeMutable(1);
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
        return costsPerDegree.numberOfBitsSet();
    }

    byte getDegree(final RouteIndexPair routePair) {
        return costsPerDegree.getDegree(routePair);
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
            if (costsPerDegree.isSet(degree, routeIndexPair)) {
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

        final ImmutableIndexedBitSet currentMatrix = costsPerDegree.getDegree(currentDegree);
        final IndexedBitSet newMatrix = costsPerDegree.getDegreeMutable(nextDegree);

        for (short route = 0; route < numRoutes; route++) {
            final SimpleBitmap resultForForRoute = SimpleBitmap.create(numRoutes);
            final SimpleImmutableBitmap currentConnectionsForRoute = currentMatrix.getBitSetForRow(route);

            currentConnectionsForRoute.getBitIndexes().forEach(connectedRoute -> {
                // if current route is connected to another route, then for next degree include that other route's connections
                final SimpleImmutableBitmap otherRoutesConnections = currentMatrix.getBitSetForRow(connectedRoute);
                //otherRoutesConnections.applyOrTo(resultForForRoute);
                resultForForRoute.or(otherRoutesConnections);
            });

            final RouteDateAndDayOverlap.RouteOverlaps dateOverlapMask = routeDateAndDayOverlap.overlapsFor(route);  // only those routes whose dates overlap
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
            final ImmutableIndexedBitSet allConnectionsAtDegree = costsPerDegree.getDegree(degree);
            final SimpleImmutableBitmap existingConnectionsAtDepth = allConnectionsAtDegree.getBitSetForRow(routeIndex);
            result.or(existingConnectionsAtDepth);
        }

        return result;
    }

    public int getConnectionDepthFor(Route routeA, Route routeB) {
        RouteIndexPair routePair = routeIndex.getPairFor(RoutePair.of(routeA, routeB));
        return getDegree(routePair);
    }

    public ImmutableIndexedBitSet getCostsPerDegree(int degree) {
        return costsPerDegree.getDegree(degree);
    }

    public ImmutableIndexedBitSet getDegree(int currentDegree) {
        return costsPerDegree.getDegree(currentDegree);
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


        public byte getDegree(RouteIndexPair routePair) {
            if (routePair.isSame()) {
                return 0;
            }
            for (int degree = 1; degree <= MAX_DEPTH; degree++) {
                if (costsPerDegree.isSet(degree, routePair)) {
                    return (byte) degree;
                }
            }
            return MAX_VALUE;
        }
    }


}
