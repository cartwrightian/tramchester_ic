package com.tramchester.graph.search;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.graph.search.routes.RouteToRouteCosts;

import java.util.EnumSet;

@ImplementedBy(RouteToRouteCosts.class)
public interface BetweenRoutesCostRepository {

    NumberOfChanges getNumberOfChanges(LocationSet starts, LocationSet destinations, TramDate date, TimeRange time, EnumSet<TransportMode> modes);
    NumberOfChanges getNumberOfChanges(Location<?> start, Location<?> destination, EnumSet<TransportMode> modes, TramDate date, TimeRange time);
    NumberOfChanges getNumberOfChanges(StationGroup start, StationGroup end, TramDate date, TimeRange time, EnumSet<TransportMode> modes);
    NumberOfChanges getNumberOfChanges(Route routeA, Route routeB, TramDate date, TimeRange timeRange, EnumSet<TransportMode> requestedModes);

    LowestCostsForDestRoutes getLowestCostCalcutatorFor(LocationSet desintationRoutes, JourneyRequest journeyRequest);

    // use version with Journey request
    @Deprecated
    LowestCostsForDestRoutes getLowestCostCalcutatorFor(LocationSet desintationRoutes, TramDate date, TimeRange time, EnumSet<TransportMode> modes);

}
