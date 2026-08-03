package com.tramchester.domain.transportStages;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;

public class WalkingToStationStage extends WalkingStage<Location<?>, Station> {

    public WalkingToStationStage(Location<?> start, Station destination, TramDuration duration, TramTime beginTime) {
        super(start, destination, duration, beginTime);
    }

    @Override
    public boolean getTowardsMyLocation() {
        return false;
    }

    @Override
    public String getHeadSign() {
        return "My Location";
    }

    @Override
    public Location<?> getActionStation() {
        return getLastStation();
    }
    
    @Override
    public String toString() {
        return "WalkingToStationStage{} " + super.toString();
    }
}
