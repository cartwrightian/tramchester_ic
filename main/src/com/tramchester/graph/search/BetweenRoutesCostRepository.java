package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.graph.search.routes.RouteToRouteCosts;

@ImplementedBy(RouteToRouteCosts.class)
public interface BetweenRoutesCostRepository {

    NumberOfChanges getNumberOfChanges(LocationSet starts, LocationSet destinations, JourneyRequest journeyRequest);
    NumberOfChanges getNumberOfChanges(Location<?> start, Location<?> destination, JourneyRequest journeyRequest);
    NumberOfChanges getNumberOfChanges(StationGroup start, StationGroup end, JourneyRequest journeyRequest);

    LowestCostsForDestRoutes getLowestCostCalculatorFor(LocationSet desintationRoutes, JourneyRequest journeyRequest);

}
