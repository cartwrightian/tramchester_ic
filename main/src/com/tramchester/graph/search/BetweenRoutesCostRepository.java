package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.search.routes.RouteToRouteCosts;

@ImplementedBy(RouteToRouteCosts.class)
public interface BetweenRoutesCostRepository {

    int getNumberOfChanges(Location<?> start, Location<?> destination, JourneyRequest journeyRequest);

    int getNumberOfChanges(LocationSet<Station> starts, LocationSet<Station> destinations, JourneyRequest journeyRequest);
    //int getNumberOfChanges(StationGroup start, StationGroup end, JourneyRequest journeyRequest);
    int getNumberOfChanges(Location<?> start, LocationSet<Station> destinations, JourneyRequest journeyRequest);
    int getNumberOfChanges(LocationSet<Station> starts, Location<?> destination, JourneyRequest journeyRequest);

    LowestCostsForDestRoutes getLowestCostCalculatorFor(LocationCollection destinationRoutes, JourneyRequest journeyRequest);

}
