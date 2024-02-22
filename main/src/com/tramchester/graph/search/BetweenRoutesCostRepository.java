package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.graph.search.routes.RouteToRouteCosts;

@ImplementedBy(RouteToRouteCosts.class)
public interface BetweenRoutesCostRepository {

    NumberOfChanges getNumberOfChanges(LocationSet<Station> starts, LocationSet<Station> destinations, JourneyRequest journeyRequest);
    NumberOfChanges getNumberOfChanges(Location<?> start, Location<?> destination, JourneyRequest journeyRequest);
    NumberOfChanges getNumberOfChanges(StationGroup start, StationGroup end, JourneyRequest journeyRequest);

    LowestCostsForDestRoutes getLowestCostCalculatorFor(LocationCollection desintationRoutes, JourneyRequest journeyRequest);

    NumberOfChanges getNumberOfChanges(Location<?> start, LocationSet<Station> destinations, JourneyRequest journeyRequest);
    NumberOfChanges getNumberOfChanges(LocationSet<Station> starts, Location<?> destination, JourneyRequest journeyRequest);
}
