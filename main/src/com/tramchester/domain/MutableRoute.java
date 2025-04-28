package com.tramchester.domain;

import com.tramchester.domain.dates.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphPropertyKey;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MutableRoute implements Route {

    private final IdFor<Route> id;
    private final String shortName;
    private final String name;
    private final Agency agency;
    private final TransportMode transportMode;
    private final Set<Service> services;
    private final Trips trips;

    private final RouteCalendar routeCalendar;

    public static final Route Walking;
    static {
            Walking = new MutableRoute(StringIdFor.createId("Walk", Route.class), "Walk", "Walk", MutableAgency.Walking,
                    TransportMode.Walk);
    }

    public MutableRoute(IdFor<Route> id, String shortName, String name, Agency agency, TransportMode transportMode) {
        this.id = id;
        this.shortName = shortName.intern();
        this.name = name.intern();

        this.agency = agency;
        this.transportMode = transportMode;
        services = new HashSet<>();
        trips  = new Trips();

        routeCalendar = new RouteCalendar(this);
    }

    // test support
    public static Route getRoute(IdFor<Route> id, String shortName, String name, Agency agency, TransportMode transportMode) {
        return new MutableRoute(id, shortName, name, agency, transportMode);
    }

    @Override
    public IdFor<Route> getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<Service> getServices() {
        return services;
    }

    public void addTrip(Trip trip) {
        trips.add(trip);
    }

    public void addService(Service service) {
        services.add(service);
        // can't check this due to data load order
//        if (!service.hasCalendar()) {
//            throw new RuntimeException("Service must have calendar to add service");
//        }
    }

    @Override
    public Agency getAgency() {
        return agency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableRoute that = (MutableRoute) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String getShortName() {
        return shortName;
    }

    @Override
    public TransportMode getTransportMode() {
        return transportMode;
    }

    @Override
    public String toString() {
        return "MutableRoute{" +
                "id=" + id +
                ", shortName='" + shortName + '\'' +
                ", name='" + name + '\'' +
                ", agency=" + agency.getId() +
                ", transportMode=" + transportMode +
                ", services=" + HasId.asIds(services) +
                ", trips=" +  trips.size() +
                ", serviceDateCache=" + routeCalendar +
                ", intoNextDay=" + intoNextDay() +
                '}';
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.ROUTE_ID;
    }

    @Override
    public Set<Trip> getTrips() {
        return trips.getTrips();
    }

    @Override
    public boolean isDateOverlap(final Route otherRoute) {
        // TODO use trips?

        // some bus routes for tfgm have no trips/services
        if (services.isEmpty()) {
            return false;
        }
        if (otherRoute.getServices().isEmpty()) {
            return false;
        }
        final MutableRoute otherMutableRoute = (MutableRoute) otherRoute;
        return routeCalendar.anyOverlapInRunning(otherMutableRoute.routeCalendar);
    }

    @Override
    public DateRange getDateRange() {
        // TODO use trips?
        return routeCalendar.getDateRange();
    }

    @Override
    public boolean isAvailableOn(final TramDate date) {
        if (this.services.isEmpty()) {
            // some bus routes for tfgm have no trips/services
            return false;
        }
        if (routeCalendar.isAvailableOn(date)) {
            // actual dates
            return trips.anyOn(date);
        }
        return false;
    }

    @Override
    public IdSet<Station> getStartStations() {
        return trips.getStartStations();
    }

    @Override
    public boolean intoNextDay() {
        return trips.intoNextDay();
    }

    private static class RouteCalendar {
        private final Route owningRoute;
        private ServiceCalendar serviceCalendar;

        private boolean loaded;

        RouteCalendar(final Route owningRoute) {
            this.owningRoute = owningRoute;
            loaded = false;
        }

        public boolean isAvailableOn(final TramDate date) {
            loadFromParent();

            return serviceCalendar.operatesOn(date);
        }

        private void loadFromParent() {
            if (loaded) {
                return;
            }
            final Set<ServiceCalendar> calendars = owningRoute.getServices().stream().
                    map(Service::getCalendar).
                    collect(Collectors.toSet());
            if (calendars.isEmpty()) {
                serviceCalendar = new EmptyServiceCalendar();
            } else {
                serviceCalendar = new AggregateServiceCalendar(calendars);
            }
            loaded = true;
        }

        public DateRange getDateRange() {
            loadFromParent();
            return serviceCalendar.getDateRange();
        }

        public boolean anyOverlapInRunning(final RouteCalendar otherCalendar) {
            loadFromParent();
            otherCalendar.loadFromParent();

            return serviceCalendar.anyDateOverlaps(otherCalendar.serviceCalendar);
        }

        @Override
        public String toString() {
            return "RouteCalendar{" +
                    "parent=" + owningRoute.getId() +
                    ", serviceCalendar=" + serviceCalendar +
                    ", loaded=" + loaded +
                    '}';
        }
    }
}
