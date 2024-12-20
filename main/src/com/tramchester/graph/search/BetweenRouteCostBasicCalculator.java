package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.*;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import org.apache.commons.collections4.SetUtils;

import java.util.OptionalInt;
import java.util.Set;

@LazySingleton
public class BetweenRouteCostBasicCalculator implements BetweenRoutesCostRepository {

    @Override
    public int getNumberOfChanges(Location<?> start, Location<?> destination, JourneyRequest journeyRequest) {
        return getPossibleMinNumChanges(start, destination);
    }

    @Override
    public int getNumberOfChanges(LocationSet<Station> starts, LocationSet<Station> destinations, JourneyRequest journeyRequest) {
        final OptionalInt findMin = starts.stream().
                flatMap(start -> destinations.stream().map(destination -> StationPair.of(start, destination))).
                filter(pair -> !pair.areSame()).
                mapToInt(pair -> getPossibleMinNumChanges(pair.getBegin(), pair.getEnd())).
                min();
        final int maxNumberOfChanges = journeyRequest.getMaxChanges().get();
        return findMin.orElse(maxNumberOfChanges);
    }

    @Override
    public int getNumberOfChanges(StationGroup start, StationGroup end, JourneyRequest journeyRequest) {
        return getNumberOfChanges(start.getAllContained(), end.getAllContained(), journeyRequest);
    }

    @Override
    public int getNumberOfChanges(Location<?> start, LocationSet<Station> destinations, JourneyRequest journeyRequest) {
        final OptionalInt findMin = destinations.stream().
                mapToInt(destination -> getPossibleMinNumChanges(start, destination)).
                min();
        final int maxNumberOfChanges = journeyRequest.getMaxChanges().get();
        return findMin.orElse(maxNumberOfChanges);
    }

    @Override
    public int getNumberOfChanges(LocationSet<Station> starts, Location<?> destination, JourneyRequest journeyRequest) {
        final OptionalInt findMin = starts.stream().
                mapToInt(start -> getPossibleMinNumChanges(start, destination)).
                min();
        final int maxNumberOfChanges = journeyRequest.getMaxChanges().get();
        return findMin.orElse(maxNumberOfChanges);
    }

    @Override
    public LowestCostsForDestRoutes getLowestCostCalculatorFor(LocationCollection destinationRoutes, JourneyRequest journeyRequest) {
        return null;
    }


    private int getPossibleMinNumChanges(Location<?> start, Location<?> destination) {
        final int min;
        if (overlaps(start.getPickupRoutes(), destination.getDropoffRoutes())) {
            min = 0; // might be a zero change
        } else {
            min = 1; // at least one changce
        }
        return min;
    }

    private boolean overlaps(Set<Route> pickUps, Set<Route> dropOffs) {
        return !SetUtils.intersection(pickUps, dropOffs).isEmpty();
    }
}
