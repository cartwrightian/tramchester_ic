package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.reference.TransportMode;

public class ChangeStationRefWithPosition extends LocationRefWithPosition {

    private TransportMode fromTransportMode;

    public ChangeStationRefWithPosition() {
        // deserialization
    }

    public ChangeStationRefWithPosition(Location<?> station, TransportMode fromTransportMode) {
        super(station);
        this.fromTransportMode = fromTransportMode;
    }

    public TransportMode getFromMode() {
        return fromTransportMode;
    }

    // deserialization
    public void setFromMode(TransportMode fromTransportMode) {
        this.fromTransportMode = fromTransportMode;
    }
}
