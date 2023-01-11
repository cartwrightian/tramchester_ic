package com.tramchester.graph.search.routes;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

@ImplementedBy(RouteCostMatrix.class)
public interface RouteCostCombinations {

    // create a bitmask for route->route changes that are possible on a given date and transport mode
    IndexedBitSet createOverlapMatrixFor(TramDate date, Set<TransportMode> requestedModes);

    int size();

    // get list of changes for given route pair and overlaps
    Stream<List<RoutePair>> getChangesFor(RouteIndexPair routePair, IndexedBitSet dateOverlaps);

    int getMaxDepth();

    int getDepth(RouteIndexPair routePair);

    boolean hasMatchAtDepth(int depth, RouteIndexPair routePair);

    boolean checkForRoutePair(int depth, RouteIndexPair pair, Function<RoutePair, Boolean> checker);
}