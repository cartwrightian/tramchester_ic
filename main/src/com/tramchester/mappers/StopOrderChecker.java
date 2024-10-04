package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import org.apache.commons.collections4.SetUtils;

import java.util.Set;

@LazySingleton
public class StopOrderChecker {

    /***
     *
     * @param date date to check for
     * @param begin station
     * @param middle station
     * @param end station
     * @return true iff there is a route where these stations appear in trip sequence order begin->middle->end
     */
    public boolean check(TramDate date, Station begin, Station middle, Station end) {
        Set<Route> beginRoutes = begin.getPickupRoutes();
        Set<Route> middleRoutes = middle.getDropoffRoutes();
        Set<Route> endRoutes = end.getDropoffRoutes();

        SetUtils.SetView<Route> beginAndMiddle = SetUtils.intersection(beginRoutes, middleRoutes);
        SetUtils.SetView<Route> beginMiddleAndEnd = SetUtils.intersection(beginAndMiddle, endRoutes);

        return beginMiddleAndEnd.stream().
                filter(route -> route.isAvailableOn(date)).
                anyMatch(route -> appearInOrder(date, begin, middle, end, route));

    }

    private boolean appearInOrder(TramDate date, Station begin, Station middle, Station end, Route route) {
        return route.getTrips().stream().
                filter(trip -> trip.operatesOn(date)).
                anyMatch(trip -> appearInOrder(begin.getId(), middle.getId(), end.getId(), trip));
    }

    private boolean appearInOrder(IdFor<Station> begin, IdFor<Station> middle, IdFor<Station> end, Trip trip) {
        if (trip.callsAt(begin) && trip.callsAt(middle) && trip.callsAt(end)) {
            return trip.isAfter(begin, middle) && trip.isAfter(middle, end);
        }
        return false;
    }
}
