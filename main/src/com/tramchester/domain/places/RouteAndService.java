package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;

import java.util.Objects;
import java.util.Set;

class RouteAndService {

    private final Route route;
    private final Service service;
    private final int hashCode;

    public RouteAndService(final Route route, final Service service) {
        this.route = route;
        this.service = service;
        hashCode = Objects.hash(route, service);
    }

    public static boolean contains(Set<RouteAndService> routeAndServices, Route route) {
        return routeAndServices.stream().
                anyMatch(routeAndService -> routeAndService.getRoute().equals(route));
    }

    public TransportMode getTransportMode() {
        return route.getTransportMode();
    }

    public boolean isAvailableOn(final TramDate date) {
        if (!route.isAvailableOn(date)) {
            return false;
        }
        return service.getCalendar().operatesOn(date);
    }

    public Route getRoute() {
        return route;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteAndService that = (RouteAndService) o;
        return route.equals(that.route) && service.equals(that.service);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "RouteAndService{" +
                "route=" + route.getId() +
                ", service=" + service.getId() +
                '}';
    }
}
