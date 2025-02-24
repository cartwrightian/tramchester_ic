package com.tramchester.domain;

import com.tramchester.domain.collections.DomainPair;
import com.tramchester.domain.dates.TramDate;

public class RoutePair extends DomainPair<Route> {

    public RoutePair(final Route first, final Route second) {
        super(first, second);
    }

    public static RoutePair of(final Route routeA, final Route routeB) {
        return new RoutePair(routeA, routeB);
    }

    @Override
    public String toString() {
        return "RoutePair{" + super.toString() + '}';
    }

    public boolean bothAvailableOn(final TramDate date) {
        return first().isAvailableOn(date) && second().isAvailableOn(date);
    }

    public boolean sameMode() {
        return first().getTransportMode().equals(second().getTransportMode());
    }

    public boolean isDateOverlap() {
        return first().isDateOverlap(second());
    }
}
