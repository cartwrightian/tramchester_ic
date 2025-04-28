package com.tramchester.domain;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;
import java.util.stream.Stream;

public class LocationCollectionSingleton implements LocationCollection {

    private final Location<?> theLocation;

    private LocationCollectionSingleton(final Location<?> theLocation) {
        this.theLocation = theLocation;
    }

    public static LocationCollection of(final Location<?> theLocation) {
        return new LocationCollectionSingleton(theLocation);
    }

    @Override
    public EnumSet<TransportMode> getModes() {
        return theLocation.getTransportModes();
    }

    @Override
    public Stream<Location<?>> locationStream() {
        return Stream.of(theLocation);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(LocationId<?> locationId) {
        return theLocation.getLocationId().equals(locationId);
    }
}
