package com.tramchester.domain;

import java.util.Set;

public interface HasRoutes {
    /***
     * Use StationAvailabilityRepository if care about date and time
     * @return all drop off routes for a station, regardless of date
     */
    Set<Route> getDropoffRoutes();

    /***
     * Use StationAvailabilityRepository if care about date and time
     * @return all pick up routes for a station, regardless of date
     */
    Set<Route> getPickupRoutes();

    boolean servesRoutePickup(Route route);

    boolean servesRouteDropOff(Route route);
}
