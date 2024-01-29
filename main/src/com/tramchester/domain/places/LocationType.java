package com.tramchester.domain.places;

import com.tramchester.domain.id.IdFor;

public enum LocationType {
    Station(false),
    Postcode(true),
    MyLocation(true),
    Platform(false),
    StationGroup(false);

    private final boolean walk;

    LocationType(boolean walk) {
        this.walk = walk;
    }

    // todo location type into IdFor
    public static <T extends Location<?>> LocationType getFor(final IdFor<T> locationId) {
        final Class<T> theType = locationId.getDomainType();
        if (theType.equals(com.tramchester.domain.places.Station.class)) {
            return Station;
        }
        if (theType.equals(PostcodeLocation.class)) {
            return Postcode;
        }
        if (theType.equals(com.tramchester.domain.Platform.class)) {
            return Platform;
        }
        if (theType.equals(com.tramchester.domain.places.StationGroup.class)) {
            return StationGroup;
        }
        if (theType.equals(com.tramchester.domain.places.MyLocation.class)) {
            return MyLocation;
        }
        throw new RuntimeException("Cannot convert type " + theType + " to a location type for id " + locationId);

    }

    public boolean isWalk() {
        return walk;
    }
}
