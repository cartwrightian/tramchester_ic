package com.tramchester.testSupport;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class TransportDataFilter {

    public static Set<Trip> getTripsFor(final Collection<Trip> trips, final HasId<Station> station) {
        return trips.stream().filter(trip -> trip.callsAt(station.getId())).collect(Collectors.toSet());
    }
}
