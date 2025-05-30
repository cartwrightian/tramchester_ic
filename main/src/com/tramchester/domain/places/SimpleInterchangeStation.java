package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.domain.id.HasId.asId;

public class SimpleInterchangeStation implements InterchangeStation {

    private final Station station;
    private final InterchangeType interchangeType;

    public SimpleInterchangeStation(Station station, InterchangeType interchangeType) {
        this.station = station;
        this.interchangeType = interchangeType;
    }

    @Override
    public boolean isMultiMode() {
        return station.getTransportModes().size() > 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleInterchangeStation that = (SimpleInterchangeStation) o;

        return station.equals(that.station);
    }

    @Override
    public int hashCode() {
        return station.hashCode();
    }

    @Override
    public Set<Route> getDropoffRoutes() {
        return Collections.unmodifiableSet(station.getDropoffRoutes());
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return Collections.unmodifiableSet(station.getPickupRoutes());
    }

    @Override
    public boolean servesRoutePickup(final Route route) {
        return station.servesRoutePickup(route);
    }

    @Override
    public boolean servesRouteDropOff(final Route route) {
        return station.servesRouteDropOff(route);
    }

    @Override
    public InterchangeType getType() {
        return interchangeType;
    }

    @Override
    public Station getStation() {
        return station;
    }

    @Override
    public LocationId<?> getLocationId() {
        return station.getLocationId();
    }

    @Override
    public Set<Station> getAllStations() {
        return Collections.singleton(station);
    }

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        return station.getTransportModes();
    }

    @Override
    public boolean anyOverlapWith(final EnumSet<TransportMode> modes) {
        return station.anyOverlapWith(modes);
    }

    @Override
    public String toString() {
        return "SimpleInterchangeStation{" +
                "station=" + asId(station) +
                ", interchangeType=" + interchangeType +
                '}';
    }

    @Override
    public IdFor<Station> getId() {
        return station.getId();
    }
}
