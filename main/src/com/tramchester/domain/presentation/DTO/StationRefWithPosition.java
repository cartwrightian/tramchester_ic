package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.LatLong;

@SuppressWarnings("unused")
public class StationRefWithPosition extends StationRefDTO {

    private LatLong latLong;

    public StationRefWithPosition(Location<?> location) {
        super(location);
        this.latLong = location.getLatLong();
    }

    public StationRefWithPosition() {
        // deserialisation
    }

    public LatLong getLatLong() {
        return latLong;
    }
}
