package com.tramchester.domain.input;

import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramTime;

public class PlatformStopCall extends StopCall {
    private final Platform callingPlatform;

    public PlatformStopCall(final Platform platform, final Station station, final TramTime arrivalTime, final TramTime departureTime,
                            final int sequenceNumber, final GTFSPickupDropoffType pickupType, final GTFSPickupDropoffType dropoffType,
                            final Trip trip) {
        super(station, arrivalTime, departureTime, sequenceNumber, pickupType, dropoffType, trip);
        this.callingPlatform = platform;
    }

    @Override
    public Platform getPlatform() {
        return callingPlatform;
    }

    @Override
    public String toString() {
        return "PlatformStopCall{" +
                "callingPlatform=" + callingPlatform.getId() +
                "} " + super.toString();
    }

    @Override
    public boolean hasPlatfrom() {
        return true;
    }
}
